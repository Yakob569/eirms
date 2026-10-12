package com.eirspay.mobile.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.InvoicePaymentMode
import com.eirspay.domain.model.LineItem
import com.eirspay.domain.model.Party
import com.eirspay.domain.model.PaymentTerm
import com.eirspay.domain.model.TaxCode
import com.eirspay.domain.model.cents
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import com.eirspay.domain.usecase.RegisterRetainerInvoiceUseCase
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/** Editable buyer, pre-filled from the retainer. Nothing has to match the original. */
data class BuyerForm(
    val name: String = "",
    val tin: String = "",
    val vat: String = "",
    val phone: String = "",
    val email: String = "",
    val region: String = "",
    val city: String = "",
    val wereda: String = "",
)

data class RetainerInvoiceUiState(
    val kind: DocumentType = DocumentType.INT,
    val retainer: Invoice? = null,
    val buyer: BuyerForm = BuyerForm(),
    val lines: List<LineDraft> = emptyList(),
    val paymentMode: InvoicePaymentMode = InvoicePaymentMode.BANK_TRANSFER,
    val paymentTerm: PaymentTerm = PaymentTerm.IMMEDIATE,
    val loading: LoaderMessage? = null,
    val error: String? = null,
) {
    val subtotal: BigDecimal get() = lines.map { it.preTax }.sumMoney()
    val taxes: List<Pair<TaxCode, BigDecimal>>
        get() = lines.groupBy { it.taxCode }.map { (code, group) -> code to group.map { it.tax }.sumMoney() }
    val taxTotal: BigDecimal get() = lines.map { it.tax }.sumMoney()
    val grandTotal: BigDecimal get() = subtotal + taxTotal
    val ready: Boolean get() = buyer.name.isNotBlank() && lines.any { it.isPriced }
    val hint: String
        get() = error ?: when {
            ready -> "Ready to register. MoR returns an IRN and signed QR."
            buyer.name.isBlank() -> "Add the buyer’s legal name."
            else -> "Add at least one priced line item."
        }
}

/**
 * Interim / final invoice against a retainer: the full invoice form with only the retainer
 * reference and document type locked.
 */
@HiltViewModel
class RetainerInvoiceViewModel @Inject constructor(
    private val observeInvoices: ObserveInvoicesUseCase,
    private val registerInvoice: RegisterRetainerInvoiceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(RetainerInvoiceUiState())
    val state: StateFlow<RetainerInvoiceUiState> = _state.asStateFlow()

    private val _registered = Channel<String>(Channel.BUFFERED)
    val registered: Flow<String> = _registered.receiveAsFlow()

    private var bound: String? = null
    private var nextKey = 0L

    fun bind(retainerId: String, kind: DocumentType) {
        val key = "$retainerId/$kind"
        if (bound == key) return
        bound = key
        viewModelScope.launch {
            val rtn = observeInvoices().first().find { it.id == retainerId } ?: return@launch
            val b = rtn.buyer
            _state.value = RetainerInvoiceUiState(
                kind = kind,
                retainer = rtn,
                buyer = BuyerForm(
                    name = b.name, tin = b.tin.orEmpty(), vat = b.vatNumber.orEmpty(), phone = b.phone.orEmpty(), email = b.email.orEmpty(),
                    region = "Addis Ababa", city = "Addis Ababa",
                    wereda = Regex("\\d+").find(b.address.orEmpty())?.value.orEmpty(),
                ),
                lines = rtn.items.map { it.toDraft(nextKey++) },
                paymentMode = rtn.paymentMode,
                paymentTerm = rtn.paymentTerm,
            )
        }
    }

    private fun LineItem.toDraft(key: Long) = LineDraft(
        key = key, description = description, quantity = quantity.plain(), price = unitPrice.plain(), taxCode = taxCode,
        itemCode = itemCode, nature = nature, unit = unit,
    )

    fun editBuyer(f: (BuyerForm) -> BuyerForm) = _state.update { it.copy(buyer = f(it.buyer), error = null) }
    fun replaceLine(line: LineDraft) = _state.update { s -> s.copy(lines = s.lines.map { if (it.key == line.key) line else it }) }
    fun removeLine(key: Long) = _state.update { s -> if (s.lines.size > 1) s.copy(lines = s.lines.filter { it.key != key }) else s }
    fun addLine() = _state.update { s -> s.copy(lines = s.lines + LineDraft(nextKey++, quantity = "1")) }
    fun setPaymentMode(m: InvoicePaymentMode) = _state.update { it.copy(paymentMode = m) }
    fun setPaymentTerm(t: PaymentTerm) = _state.update { it.copy(paymentTerm = t) }

    fun submit() {
        val s = _state.value
        val rtn = s.retainer ?: return
        if (!s.ready || s.loading != null) return
        val b = s.buyer
        val buyer = Party(
            name = b.name.trim(),
            tin = b.tin.trim().ifBlank { null },
            vatNumber = b.vat.trim().ifBlank { null },
            phone = b.phone.trim().ifBlank { null },
            email = b.email.trim().ifBlank { null },
            address = listOfNotNull(b.city.trim().ifBlank { null }, b.wereda.trim().ifBlank { null }?.let { "Wereda $it" })
                .joinToString(" · ").ifBlank { null },
        )
        val items = s.lines.filter { it.isPriced }.map { l ->
            LineItem(
                description = l.description.trim(), itemCode = l.itemCode ?: "ITM-NEW", nature = l.nature, unit = l.unit.ifBlank { "PCS" },
                quantity = if (l.qty > BigDecimal.ZERO) l.qty else BigDecimal.ONE, unitPrice = l.unitPrice.cents(), taxCode = l.taxCode,
            )
        }
        _state.update { it.copy(loading = LoaderMessage("Registering with EIRS", "Waiting for MoR to sign the " + s.kind.displayName.lowercase())) }
        viewModelScope.launch {
            try {
                val created = registerInvoice(s.kind, rtn.id, buyer, items, s.paymentMode, s.paymentTerm)
                _registered.send(created.id)
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(loading = null) }
            }
        }
    }
}
