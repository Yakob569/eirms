package com.eirspay.mobile.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.CancelReason
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.InvoiceStatus
import com.eirspay.domain.model.TransactionType
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.CancelInvoiceUseCase
import com.eirspay.domain.usecase.InvoiceRowAction
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import com.eirspay.domain.usecase.RowActionPolicy
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class InvoiceFilters(
    val query: String = "",
    val canceledMode: Boolean = false,
    /** null = all statuses (within the active or canceled pool). */
    val status: InvoiceStatus? = null,
    val transactionType: TransactionType? = null,
)

data class InvoicesUiState(
    val filters: InvoiceFilters = InvoiceFilters(),
    val branch: Branch? = null,
    val rows: List<Invoice> = emptyList(),
    val sum: BigDecimal = BigDecimal.ZERO,
    val loaded: Boolean = false,
)

/**
 * The ⋮ row-actions sheet and the cancel sheet it can open. Ids stay set while a sheet animates
 * out, so its content doesn't vanish mid-slide.
 */
data class InvoiceSheetsUiState(
    val actionsOpen: Boolean = false,
    val actionInvoice: Invoice? = null,
    val actions: List<InvoiceRowAction> = emptyList(),
    val cancelOpen: Boolean = false,
    val cancelInvoice: Invoice? = null,
    val cancelReason: CancelReason = CancelReason.CUSTOMER_REQUEST,
    val loading: LoaderMessage? = null,
)

private data class SheetSelection(
    val actionId: String? = null,
    val actionsOpen: Boolean = false,
    val cancelId: String? = null,
    val cancelOpen: Boolean = false,
    val reason: CancelReason = CancelReason.CUSTOMER_REQUEST,
    val loading: LoaderMessage? = null,
)

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    observeInvoices: ObserveInvoicesUseCase,
    preferences: PreferencesRepository,
    private val cancelInvoice: CancelInvoiceUseCase,
) : ViewModel() {
    private val filters = MutableStateFlow(InvoiceFilters())
    private var handledCanceledRequest = 0

    val state: StateFlow<InvoicesUiState> =
        combine(observeInvoices(), preferences.selectedBranch, filters) { all, branch, f ->
            val q = f.query.trim().lowercase()
            val rows = all
                .filter { branch == null || it.branch == branch }
                .filter { if (f.canceledMode) it.isCanceled else !it.isCanceled }
                .filter { f.status == null || it.status == f.status }
                .filter { f.transactionType == null || it.transactionType == f.transactionType }
                .filter {
                    q.isEmpty() || listOf(it.number, it.buyer.name, it.buyer.tin.orEmpty(), it.buyer.idNumber.orEmpty(), it.irn)
                        .joinToString(" ").lowercase().contains(q)
                }
            InvoicesUiState(f, branch, rows, rows.map { it.totals.grandTotal }.sumMoney(), loaded = true)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, InvoicesUiState())

    fun setQuery(q: String) = filters.update { it.copy(query = q) }

    /** Status chip: "Canceled" switches to the canceled pool, the others filter the active pool. */
    fun pickStatus(status: InvoiceStatus?) = filters.update {
        if (status == InvoiceStatus.CANCELED) it.copy(canceledMode = true, status = null)
        else it.copy(canceledMode = false, status = status)
    }

    fun pickTransactionType(tt: TransactionType?) = filters.update { it.copy(transactionType = tt) }

    /** The shell bumps a counter to ask for the canceled list; each request is applied once. */
    fun onCanceledRequest(request: Int) {
        if (request > 0 && request != handledCanceledRequest) {
            handledCanceledRequest = request
            filters.update { it.copy(canceledMode = true, status = null) }
        }
    }

    // --- Row actions ------------------------------------------------------------------------

    private val selection = MutableStateFlow(SheetSelection())

    val sheets: StateFlow<InvoiceSheetsUiState> = combine(observeInvoices(), selection) { all, sel ->
        val action = sel.actionId?.let { id -> all.find { it.id == id } }
        InvoiceSheetsUiState(
            actionsOpen = sel.actionsOpen && action != null,
            actionInvoice = action,
            actions = action?.let { RowActionPolicy.invoiceActions(it, all) }.orEmpty(),
            cancelOpen = sel.cancelOpen,
            cancelInvoice = sel.cancelId?.let { id -> all.find { it.id == id } },
            cancelReason = sel.reason,
            loading = sel.loading,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, InvoiceSheetsUiState())

    fun openActions(id: String) = selection.update { it.copy(actionId = id, actionsOpen = true) }
    fun closeActions() = selection.update { it.copy(actionsOpen = false) }

    /** "Cancel invoice" swaps the actions sheet for the existing cancel sheet. */
    fun startCancel(id: String) = selection.update {
        it.copy(actionsOpen = false, cancelId = id, cancelOpen = true, reason = CancelReason.CUSTOMER_REQUEST)
    }

    fun closeCancel() = selection.update { it.copy(cancelOpen = false) }
    fun pickCancelReason(reason: CancelReason) = selection.update { it.copy(reason = reason) }

    fun confirmCancel() {
        val sel = selection.value
        val id = sel.cancelId ?: return
        if (sel.loading != null) return
        val irn = sheets.value.cancelInvoice?.irn.orEmpty()
        selection.update { it.copy(cancelOpen = false, loading = LoaderMessage("Canceling with MoR", "Recording against IRN ETH-" + irn.take(8))) }
        viewModelScope.launch {
            try {
                cancelInvoice(id, sel.reason)
            } finally {
                selection.update { it.copy(loading = null) }
            }
        }
    }
}
