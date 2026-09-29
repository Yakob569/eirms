package com.eirspay.mobile.feature.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Amendment
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.BuyerIdType
import com.eirspay.domain.model.CatalogItem
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.InvoicePaymentMode
import com.eirspay.domain.model.LineItem
import com.eirspay.domain.model.NatureOfSupply
import com.eirspay.domain.model.Party
import com.eirspay.domain.model.PaymentTerm
import com.eirspay.domain.model.TaxCode
import com.eirspay.domain.model.TransactionType
import com.eirspay.domain.model.cents
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.repository.InvoiceDraft
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.LookupTaxpayerUseCase
import com.eirspay.domain.usecase.ObserveCatalogUseCase
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import com.eirspay.domain.usecase.RegisterInvoiceUseCase
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
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

/** One editable line of the form. Catalog-added lines carry the catalog item's code, nature and unit. */
data class LineDraft(
    val key: Long,
    val description: String = "",
    val quantity: String = "1",
    val price: String = "",
    val taxCode: TaxCode = TaxCode.VAT15,
    val itemCode: String? = null,
    val nature: NatureOfSupply = NatureOfSupply.GOODS,
    val unit: String = "PCS",
) {
    val qty: BigDecimal get() = quantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val unitPrice: BigDecimal get() = price.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val preTax: BigDecimal get() = (qty * unitPrice).cents()
    val tax: BigDecimal get() = (preTax * BigDecimal.valueOf(taxCode.rate)).cents()
    val total: BigDecimal get() = preTax + tax
    val isEmpty: Boolean get() = description.isBlank() && price.isBlank()
    val isPriced: Boolean get() = description.isNotBlank() && unitPrice > BigDecimal.ZERO
}

data class RegisterInvoiceUiState(
    val branch: Branch = Branch.MAIN,
    val documentType: DocumentType = DocumentType.INV,
    val transactionType: TransactionType = TransactionType.B2C,
    val walkIn: Boolean = true,
    val idType: BuyerIdType = BuyerIdType.NID,
    val name: String = "",
    val idNumber: String = "",
    val tin: String = "",
    val vat: String = "",
    val address: String = "",
    val lookingUp: Boolean = false,
    val previousIrn: String = "",
    val amendReason: String = "",
    val lines: List<LineDraft> = listOf(LineDraft(0)),
    val paymentMode: InvoicePaymentMode = InvoicePaymentMode.CASH,
    val paymentTerm: PaymentTerm = PaymentTerm.IMMEDIATE,
    val quickAdd: List<CatalogItem> = emptyList(),
    val loading: LoaderMessage? = null,
) {
    val consumer: Boolean get() = transactionType.isConsumer
    val grandTotal: BigDecimal get() = lines.map { it.total }.sumMoney()
    /** Prototype rule: one priced line, and a buyer name unless it's a walk-in consumer sale. */
    val valid: Boolean
        get() = lines.any { it.isPriced } && (if (consumer) walkIn || name.isNotBlank() else name.isNotBlank())
    val billTo: String get() = if (consumer && walkIn) "Walk-in customer" else name.ifBlank { "—" }
}

@HiltViewModel
class RegisterInvoiceViewModel @Inject constructor(
    private val registerInvoice: RegisterInvoiceUseCase,
    private val lookupTaxpayer: LookupTaxpayerUseCase,
    observeCatalog: ObserveCatalogUseCase,
    observeInvoices: ObserveInvoicesUseCase,
    preferences: PreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterInvoiceUiState(branch = preferences.selectedBranch.value ?: Branch.MAIN))
    val state: StateFlow<RegisterInvoiceUiState> = _state.asStateFlow()

    private val _registered = Channel<String>(Channel.BUFFERED)
    /** Emits the new invoice id once MoR has signed it. */
    val registered: Flow<String> = _registered.receiveAsFlow()

    private var invoices: List<Invoice> = emptyList()
    private var nextKey = 1L

    init {
        viewModelScope.launch {
            observeCatalog().collect { items -> _state.update { it.copy(quickAdd = items.filter { i -> i.active }.take(5)) } }
        }
        viewModelScope.launch { observeInvoices().collect { invoices = it } }
    }

    fun setDocumentType(t: DocumentType) = _state.update { it.copy(documentType = t) }
    fun setTransactionType(t: TransactionType) = _state.update { it.copy(transactionType = t) }
    fun toggleWalkIn() = _state.update { it.copy(walkIn = !it.walkIn) }
    fun setIdType(t: BuyerIdType) = _state.update { it.copy(idType = t) }
    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setIdNumber(v: String) = _state.update { it.copy(idNumber = v) }
    fun setTin(v: String) = _state.update { it.copy(tin = v.filter { c -> c.isDigit() }.take(10)) }
    fun setVat(v: String) = _state.update { it.copy(vat = v) }
    fun setPreviousIrn(v: String) = _state.update { it.copy(previousIrn = v) }
    fun setAmendReason(v: String) = _state.update { it.copy(amendReason = v) }
    fun setPaymentMode(m: InvoicePaymentMode) = _state.update { it.copy(paymentMode = m) }
    fun setPaymentTerm(t: PaymentTerm) = _state.update { it.copy(paymentTerm = t) }

    fun lookup() {
        val tin = _state.value.tin
        if (_state.value.lookingUp) return
        _state.update { it.copy(lookingUp = true) }
        viewModelScope.launch {
            try {
                val party = lookupTaxpayer(tin)
                if (party != null) {
                    _state.update { it.copy(name = party.name, vat = party.vatNumber.orEmpty(), address = party.address.orEmpty()) }
                }
            } finally {
                _state.update { it.copy(lookingUp = false) }
            }
        }
    }

    private fun updateLine(key: Long, f: (LineDraft) -> LineDraft) =
        _state.update { s -> s.copy(lines = s.lines.map { if (it.key == key) f(it) else it }) }

    fun setLineDescription(key: Long, v: String) = updateLine(key) { it.copy(description = v) }
    fun setLineQuantity(key: Long, v: String) = updateLine(key) { it.copy(quantity = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setLinePrice(key: Long, v: String) = updateLine(key) { it.copy(price = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setLineTaxCode(key: Long, code: TaxCode) = updateLine(key) { it.copy(taxCode = code) }

    fun removeLine(key: Long) = _state.update { s ->
        s.copy(lines = if (s.lines.size > 1) s.lines.filter { it.key != key } else listOf(LineDraft(nextKey++)))
    }

    fun addLine() = _state.update { s -> s.copy(lines = s.lines + LineDraft(nextKey++)) }

    /** Quick-add fills the first empty line, or appends a new one. */
    fun quickAdd(item: CatalogItem) = _state.update { s ->
        val empty = s.lines.indexOfFirst { it.isEmpty }
        val key = if (empty >= 0) s.lines[empty].key else nextKey++
        val line = LineDraft(
            key = key, description = item.description, quantity = "1", price = item.unitPrice.stripTrailingZeros().toPlainString(),
            taxCode = item.taxCode, itemCode = item.code, nature = item.nature, unit = item.unit,
        )
        s.copy(lines = if (empty >= 0) s.lines.mapIndexed { i, l -> if (i == empty) line else l } else s.lines + line)
    }

    fun submit() {
        val s = _state.value
        if (!s.valid || s.loading != null) return
        val buyer = when {
            s.consumer && s.walkIn -> Party.WalkIn
            s.consumer -> Party(name = s.name.trim().ifBlank { "Consumer" }, idType = s.idType, idNumber = s.idNumber.trim().ifBlank { null })
            else -> Party(
                name = s.name.trim(), tin = s.tin.trim(), vatNumber = s.vat.trim().ifBlank { null },
                address = s.address.trim().ifBlank { null },
            )
        }
        val items = s.lines.filter { it.isPriced }.mapIndexed { i, l ->
            LineItem(
                description = l.description.trim(), itemCode = l.itemCode ?: "ITM-${900 + i}", nature = l.nature, unit = l.unit,
                quantity = if (l.qty > BigDecimal.ZERO) l.qty else BigDecimal.ONE, unitPrice = l.unitPrice.cents(), taxCode = l.taxCode,
            )
        }
        val amendment = if (s.documentType.isAmendment) {
            Amendment(reason = s.amendReason.trim().ifBlank { "Amendment" }, previousInvoiceId = resolvePrevious(s.previousIrn))
        } else null
        val draft = InvoiceDraft(
            documentType = s.documentType, transactionType = s.transactionType, branch = s.branch, buyer = buyer,
            paymentMode = s.paymentMode, paymentTerm = s.paymentTerm, items = items, amendment = amendment,
        )
        _state.update { it.copy(loading = LoaderMessage("Registering with EIRS", "Waiting for MoR to sign the invoice")) }
        viewModelScope.launch {
            try {
                val created = registerInvoice(draft)
                _registered.send(created.id)
            } finally {
                _state.update { it.copy(loading = null) }
            }
        }
    }

    /** Matches the typed IRN (full, "ETH-" prefixed or partial) or document number against invoices on record. */
    private fun resolvePrevious(input: String): String {
        val q = input.trim().removePrefix("ETH-").removePrefix("eth-").substringBefore("··").substringBefore("…").lowercase()
        if (q.isEmpty()) return input.trim()
        val hit = invoices.firstOrNull { it.irn.lowercase() == q }
            ?: invoices.firstOrNull { it.number == q }
            ?: invoices.firstOrNull { it.irn.lowercase().startsWith(q) }
        return hit?.id ?: input.trim()
    }
}
