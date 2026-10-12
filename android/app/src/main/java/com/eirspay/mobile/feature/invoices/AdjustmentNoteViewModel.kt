package com.eirspay.mobile.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.LineItem
import com.eirspay.domain.model.cents
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.usecase.AdjustmentLine
import com.eirspay.domain.usecase.AdjustmentRules
import com.eirspay.domain.usecase.AdjustmentValidation
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import com.eirspay.domain.usecase.RegisterAdjustmentNoteUseCase
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** One original line: ticked lines get editable quantity / unit price, prefilled from the original. */
data class AdjustLineUi(
    val index: Int,
    val item: LineItem,
    val selected: Boolean = false,
    val quantity: String = "",
    val price: String = "",
) {
    val qty: BigDecimal get() = quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val unitPrice: BigDecimal get() = price.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val preTax: BigDecimal get() = if (selected) (qty * unitPrice).cents() else BigDecimal.ZERO.cents()
    val tax: BigDecimal get() = (preTax * BigDecimal.valueOf(item.taxCode.rate)).cents()
    val total: BigDecimal get() = preTax + tax
    val originalTotal: BigDecimal
        get() = (item.quantity * item.unitPrice).cents().let { it + (it * BigDecimal.valueOf(item.taxCode.rate)).cents() }
}

data class AdjustmentUiState(
    val kind: DocumentType = DocumentType.CRE,
    val original: Invoice? = null,
    val lines: List<AdjustLineUi> = emptyList(),
    val reason: String = "",
    val loading: LoaderMessage? = null,
    /** Set when MoR (or the server-side rules) refuse the note, e.g. one already exists. */
    val error: String? = null,
) {
    val isCredit: Boolean get() = kind == DocumentType.CRE
    val picks: List<AdjustmentLine> get() = lines.filter { it.selected }.map { AdjustmentLine(it.index, it.qty, it.unitPrice) }
    val validation: AdjustmentValidation?
        get() = original?.let { AdjustmentRules.validate(kind, it, picks, reason) }
    val exceeding: Set<Int> get() = (validation as? AdjustmentValidation.ExceedsOriginal)?.indexes.orEmpty()
    val ready: Boolean get() = validation == AdjustmentValidation.Ok
    val subtotal: BigDecimal get() = lines.map { it.preTax }.sumMoney()
    val taxTotal: BigDecimal get() = lines.map { it.tax }.sumMoney()
    val grandTotal: BigDecimal get() = subtotal + taxTotal
    val hint: String
        get() = error ?: when (validation) {
            AdjustmentValidation.Ok -> "Ready to register. MoR returns an IRN and signed QR."
            AdjustmentValidation.NoLines, null -> "Tick at least one line to adjust."
            AdjustmentValidation.MissingReason -> "Add a reason."
            is AdjustmentValidation.ExceedsOriginal -> "Fix the highlighted line."
        }
}

/** Credit / debit note against one invoice: everything is locked except the ticked lines and the reason. */
@HiltViewModel
class AdjustmentNoteViewModel @Inject constructor(
    private val observeInvoices: ObserveInvoicesUseCase,
    private val registerNote: RegisterAdjustmentNoteUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AdjustmentUiState())
    val state: StateFlow<AdjustmentUiState> = _state.asStateFlow()

    private val _registered = Channel<String>(Channel.BUFFERED)
    /** Emits the new note's id once MoR has signed it. */
    val registered: Flow<String> = _registered.receiveAsFlow()

    private var bound: String? = null
    private var job: Job? = null

    fun bind(invoiceId: String, kind: DocumentType) {
        val key = "$invoiceId/$kind"
        if (bound == key) return
        bound = key
        job?.cancel()
        job = viewModelScope.launch {
            observeInvoices().collect { all ->
                val inv = all.find { it.id == invoiceId } ?: return@collect
                _state.update { s ->
                    if (s.original?.id == inv.id) s.copy(original = inv)
                    else AdjustmentUiState(kind = kind, original = inv, lines = inv.items.mapIndexed { i, item -> AdjustLineUi(i, item) })
                }
            }
        }
    }

    private fun updateLine(index: Int, f: (AdjustLineUi) -> AdjustLineUi) =
        _state.update { s -> s.copy(lines = s.lines.map { if (it.index == index) f(it) else it }) }

    /** Ticking a line prefills it with the original quantity and price. */
    fun toggle(index: Int) = updateLine(index) {
        if (it.selected) it.copy(selected = false, quantity = "", price = "")
        else it.copy(selected = true, quantity = it.item.quantity.plain(), price = it.item.unitPrice.plain())
    }

    fun setQuantity(index: Int, v: String) = updateLine(index) { it.copy(quantity = v) }
    fun setPrice(index: Int, v: String) = updateLine(index) { it.copy(price = v) }
    fun setReason(v: String) = _state.update { it.copy(reason = v, error = null) }

    fun submit() {
        val s = _state.value
        val original = s.original ?: return
        if (!s.ready || s.loading != null) return
        _state.update {
            it.copy(loading = LoaderMessage("Registering with EIRS", "Waiting for MoR to sign the " + s.kind.displayName.lowercase()))
        }
        viewModelScope.launch {
            try {
                val created = registerNote(s.kind, original.id, s.picks, s.reason)
                _registered.send(created.id)
            } catch (e: IllegalStateException) {
                _state.update { it.copy(error = e.message) }
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(loading = null) }
            }
        }
    }
}

internal fun BigDecimal.plain(): String = stripTrailingZeros().toPlainString()
