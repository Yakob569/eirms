package com.eirspay.mobile.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.CancelReason
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.usecase.CancelInvoiceUseCase
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoiceDetailUiState(
    val invoice: Invoice? = null,
    /** For credit/debit notes: the invoice being amended, when it is on record. */
    val amended: Invoice? = null,
    val cancelSheetOpen: Boolean = false,
    val cancelReason: CancelReason = CancelReason.CUSTOMER_REQUEST,
    val loading: LoaderMessage? = null,
)

/** Backs both the invoice detail and its printable receipt. */
@HiltViewModel
class InvoiceDetailViewModel @Inject constructor(
    private val observeInvoices: ObserveInvoicesUseCase,
    private val cancelInvoice: CancelInvoiceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(InvoiceDetailUiState())
    val state: StateFlow<InvoiceDetailUiState> = _state.asStateFlow()
    private var boundId: String? = null
    private var job: Job? = null

    fun bind(id: String) {
        if (boundId == id) return
        boundId = id
        job?.cancel()
        job = viewModelScope.launch {
            observeInvoices().collect { all ->
                val inv = all.find { it.id == id }
                val amended = inv?.amendment?.let { a -> all.find { it.id == a.previousInvoiceId } }
                _state.update { it.copy(invoice = inv, amended = amended) }
            }
        }
    }

    fun openCancel() = _state.update { it.copy(cancelSheetOpen = true) }
    fun closeCancel() = _state.update { it.copy(cancelSheetOpen = false) }
    fun pickReason(reason: CancelReason) = _state.update { it.copy(cancelReason = reason) }

    fun confirmCancel() {
        val inv = _state.value.invoice ?: return
        if (_state.value.loading != null) return
        val reason = _state.value.cancelReason
        _state.update {
            it.copy(cancelSheetOpen = false, loading = LoaderMessage("Canceling with MoR", "Recording against IRN ETH-" + inv.irn.take(8)))
        }
        viewModelScope.launch {
            try {
                val updated = cancelInvoice(inv.id, reason)
                _state.update { it.copy(invoice = updated) }
            } finally {
                _state.update { it.copy(loading = null) }
            }
        }
    }
}
