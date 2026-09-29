package com.eirspay.mobile.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.InvoiceStatus
import com.eirspay.domain.model.TransactionType
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    observeInvoices: ObserveInvoicesUseCase,
    preferences: PreferencesRepository,
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
}
