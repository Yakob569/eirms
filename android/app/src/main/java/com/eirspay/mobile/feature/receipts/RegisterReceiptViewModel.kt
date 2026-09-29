package com.eirspay.mobile.feature.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.repository.ReceiptDraft
import com.eirspay.domain.usecase.ObserveCatalogUseCase
import com.eirspay.domain.usecase.ObserveInvoicesUseCase
import com.eirspay.domain.usecase.ObserveWithholdingUseCase
import com.eirspay.domain.usecase.RegisterReceiptUseCase
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class RegisterReceiptForm(
    val type: ReceiptType = ReceiptType.SALES,
    val method: PaymentMethod = PaymentMethod.CASH,
    val linkId: String? = null,
    val amount: String = "",
    val walkIn: Boolean = true,
    val name: String = "",
    val phone: String = "",
    val reference: String = "",
    val provider: String = MobileMoneyProviders.first(),
    val tendered: String = "",
    /** Catalog code → quantity, in the order added. */
    val lines: List<Pair<String, Int>> = emptyList(),
    val query: String = "",
)

data class CatalogTileUi(val code: String, val description: String, val price: String, val meta: String, val inCart: Boolean)
data class CartLineUi(val index: Int, val description: String, val total: String, val quantity: String)
data class LinkOptionUi(val id: String, val number: String, val total: String, val party: String, val status: String, val selected: Boolean)
data class TenderChipUi(val label: String, val value: String)

data class RegisterReceiptUiState(
    val form: RegisterReceiptForm = RegisterReceiptForm(),
    val branchCaps: String = Branch.MAIN.displayName.uppercase(),
    val catalogNote: String = "",
    val catalog: List<CatalogTileUi> = emptyList(),
    val lines: List<CartLineUi> = emptyList(),
    val linkOptions: List<LinkOptionUi> = emptyList(),
    val amountHint: String = "Pick an invoice to prefill the amount.",
    val linkedNo: String? = null,
    val linkedParty: String = "",
    val grand: BigDecimal = ZeroMoney,
    val tenderPlaceholder: String = "0.00",
    val tenderChips: List<TenderChipUi> = emptyList(),
    val change: String = "0.00",
    val problems: List<String> = emptyList(),
    val billTo: String = "",
    val loading: LoaderMessage? = null,
) {
    val needsItems: Boolean get() = form.type.carriesItems
    val needsLink: Boolean get() = !needsItems
    val refund: Boolean get() = form.type == ReceiptType.REFUND
    val ready: Boolean get() = problems.isEmpty()
    val fromLabel: String get() = if (refund) "Refund to" else "Received from"
    val typeCaps: String get() = form.type.displayName.uppercase() + " · " + form.method.displayName.uppercase()
    val grandText: String get() = (if (refund) "−" else "") + Fmt.etb(grand)
    val amountLabel: String get() = if (refund) "Refund amount *" else "Amount received *"
    val linkNote: String get() = if (form.type == ReceiptType.PAYMENT) "Open B2B / B2G invoices" else "Invoice being refunded"
    val referenceLabel: String
        get() = when (form.method) {
            PaymentMethod.CHEQUE -> "Cheque number *"; PaymentMethod.BANK_TRANSFER -> "Transfer reference *"
            PaymentMethod.MOBILE_MONEY -> "Transaction ID *"; PaymentMethod.CARD -> "Card (last 4 digits)"; else -> ""
        }
    val referencePlaceholder: String
        get() = when (form.method) {
            PaymentMethod.CHEQUE -> "Bank and cheque no."; PaymentMethod.BANK_TRANSFER -> "e.g. FT26267AB91C"
            PaymentMethod.MOBILE_MONEY -> "From the payment SMS"; PaymentMethod.CARD -> "•••• 1234"; else -> ""
        }
}

private data class RegData(
    val catalog: List<CatalogItem>,
    val invoices: List<Invoice>,
    val withholding: List<WithholdingRecord>,
    val branch: Branch?,
)

@HiltViewModel
class RegisterReceiptViewModel @Inject constructor(
    observeCatalog: ObserveCatalogUseCase,
    observeInvoices: ObserveInvoicesUseCase,
    observeWithholding: ObserveWithholdingUseCase,
    preferences: PreferencesRepository,
    private val registerReceipt: RegisterReceiptUseCase,
) : ViewModel() {
    private val form = MutableStateFlow(RegisterReceiptForm())
    private val loading = MutableStateFlow<LoaderMessage?>(null)
    private val events = Channel<String>(Channel.BUFFERED)
    /** Emits the id of the registered receipt. */
    val registered: Flow<String> = events.receiveAsFlow()

    private val data = combine(observeCatalog(), observeInvoices(), observeWithholding(), preferences.selectedBranch) { cat, inv, w, b ->
        RegData(cat, inv, w.map { it.record }, b)
    }

    private var latest: RegData = RegData(emptyList(), emptyList(), emptyList(), null)

    val state: StateFlow<RegisterReceiptUiState> = combine(form, loading, data) { f, l, d ->
        latest = d
        build(f, l, d)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RegisterReceiptUiState())

    private fun set(block: (RegisterReceiptForm) -> RegisterReceiptForm) = form.update(block)

    fun pickType(t: ReceiptType) = set { it.copy(type = t, linkId = null, amount = "") }
    fun onQuery(q: String) = set { it.copy(query = q) }
    fun addItem(code: String) = set { f ->
        val ex = f.lines.any { it.first == code }
        f.copy(lines = if (ex) f.lines.map { if (it.first == code) it.first to it.second + 1 else it } else f.lines + (code to 1))
    }
    fun increment(i: Int) = set { f -> f.copy(lines = f.lines.mapIndexed { j, l -> if (j == i) l.first to l.second + 1 else l }) }
    fun decrement(i: Int) = set { f ->
        val l = f.lines.getOrNull(i) ?: return@set f
        f.copy(lines = if (l.second > 1) f.lines.mapIndexed { j, x -> if (j == i) x.first to x.second - 1 else x } else f.lines.filterIndexed { j, _ -> j != i })
    }
    fun pickLink(id: String) = set { f ->
        val inv = latest.invoices.find { it.id == id } ?: return@set f
        f.copy(linkId = id, amount = defaultAmount(f.type, inv, latest.withholding).setScale(2, RoundingMode.HALF_UP).toPlainString())
    }
    fun onAmount(v: String) = set { it.copy(amount = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    fun toggleWalkIn() = set { it.copy(walkIn = !it.walkIn) }
    fun onName(v: String) = set { it.copy(name = v) }
    fun onPhone(v: String) = set { it.copy(phone = v) }
    fun pickMethod(m: PaymentMethod) = set { it.copy(method = m, reference = "") }
    fun onTendered(v: String) = set { it.copy(tendered = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    fun pickTender(v: String) = set { it.copy(tendered = v) }
    fun pickProvider(p: String) = set { it.copy(provider = p) }
    fun onReference(v: String) = set { it.copy(reference = v) }

    fun submit() {
        val s = state.value
        if (!s.ready || s.loading != null) return
        val f = s.form
        val d = latest
        val needsLink = s.needsLink
        val li = f.linkId?.let { id -> d.invoices.find { it.id == id } }
        val byCode = d.catalog.associateBy { it.code }
        val items = if (needsLink) emptyList() else f.lines.mapNotNull { (code, q) -> byCode[code]?.toLineItem(BigDecimal(q)) }
        val phone = f.phone.trim()
        val buyer = when {
            needsLink -> null
            f.walkIn -> Party.WalkIn
            phone.length == 10 && phone.all { it.isDigit() } -> Party(name = f.name.trim(), tin = phone)
            else -> Party(name = f.name.trim(), phone = phone.ifEmpty { null })
        }
        val draft = ReceiptDraft(
            type = f.type, method = f.method,
            branch = if (needsLink && li != null) li.branch else d.branch ?: Branch.MAIN,
            buyer = buyer, items = items,
            reference = f.reference.trim().ifEmpty { null },
            provider = if (f.method == PaymentMethod.MOBILE_MONEY) f.provider else null,
            tendered = if (f.method == PaymentMethod.CASH) (Fmt.parseMoney(f.tendered)?.takeIf { it > BigDecimal.ZERO } ?: s.grand) else null,
            linkedInvoiceId = if (needsLink) f.linkId else null,
            amount = if (needsLink) s.grand else null,
        )
        viewModelScope.launch {
            loading.value = LoaderMessage("Registering receipt", "Waiting for MoR to sign it")
            try {
                val receipt = registerReceipt(draft)
                events.send(receipt.id)
            } finally {
                loading.value = null
            }
        }
    }

    private fun defaultAmount(type: ReceiptType, v: Invoice, wht: List<WithholdingRecord>): BigDecimal {
        val w = wht.find { it.invoiceId == v.id }
        return if (type == ReceiptType.PAYMENT && w != null) v.totals.grandTotal - (v.totals.subtotal * w.rate).cents() else v.totals.grandTotal
    }

    private fun build(f: RegisterReceiptForm, loading: LoaderMessage?, d: RegData): RegisterReceiptUiState {
        val needsItems = f.type.carriesItems
        val needsLink = !needsItems
        val active = d.catalog.filter { it.active }
        val byCode = d.catalog.associateBy { it.code }
        val q = f.query.trim().lowercase()
        val lineItems = f.lines.mapNotNull { (code, qty) -> byCode[code]?.toLineItem(BigDecimal(qty)) }
        val totals = TaxCalculator.calculate(lineItems)
        val grand = if (needsItems) totals.grandTotal else (Fmt.parseMoney(f.amount) ?: ZeroMoney)

        val invs = d.invoices.filter { v ->
            v.documentType == DocumentType.INV && v.status != InvoiceStatus.CANCELED &&
                (f.type != ReceiptType.PAYMENT || (v.status in setOf(InvoiceStatus.ISSUED, InvoiceStatus.WITHHELD) && !v.transactionType.isConsumer))
        }
        val li = f.linkId?.let { id -> d.invoices.find { it.id == id } }
        val tendered = Fmt.parseMoney(f.tendered)
        val refReq = f.method.requiresReference
        val problems = listOfNotNull(
            "add at least one item".takeIf { needsItems && f.lines.isEmpty() },
            "pick the linked invoice".takeIf { needsLink && li == null },
            "enter the amount".takeIf { needsLink && grand <= BigDecimal.ZERO },
            "tendered is less than the total".takeIf { f.method == PaymentMethod.CASH && f.tendered.isNotEmpty() && (tendered ?: ZeroMoney) < grand },
            ("add the " + when (f.method) {
                PaymentMethod.CHEQUE -> "cheque number"; PaymentMethod.BANK_TRANSFER -> "transfer reference"; else -> "transaction ID"
            }).takeIf { refReq && f.reference.isBlank() },
            "add customer name".takeIf { !needsLink && !f.walkIn && f.name.isBlank() },
        )
        val roundUps = if (grand > BigDecimal.ZERO) {
            listOf(grand, ceilTo(grand, 100), ceilTo(grand, 500), ceilTo(grand, 1000)).map { it.setScale(2) }.distinct().take(4)
        } else emptyList()
        val amountHint = when {
            li == null -> "Pick an invoice to prefill the amount."
            f.type == ReceiptType.PAYMENT -> if (d.withholding.any { it.invoiceId == li.id })
                "Prefilled with the invoice total less the 2% the buyer withheld. Change it for a part payment."
            else "Prefilled with the invoice total. Change it for a part payment."
            else -> "Prefilled with the full invoice total. Lower it for a partial refund."
        }
        return RegisterReceiptUiState(
            form = f,
            branchCaps = (d.branch ?: Branch.MAIN).displayName.uppercase(),
            catalogNote = "${active.size} active in Items",
            catalog = active.filter { q.isEmpty() || "${it.code} ${it.description}".lowercase().contains(q) }.take(9).map { item ->
                CatalogTileUi(item.code, item.description, Fmt.f2(item.unitPrice), "${item.taxCode.name} · ${item.unit}", f.lines.any { it.first == item.code })
            },
            lines = f.lines.mapIndexedNotNull { i, (code, qty) ->
                val item = byCode[code] ?: return@mapIndexedNotNull null
                val lt = totals.lines.getOrNull(i)
                CartLineUi(i, item.description, Fmt.f2(lt?.total ?: ZeroMoney), qty.toString())
            },
            linkOptions = invs.map { v ->
                LinkOptionUi(v.id, v.displayNo, Fmt.etb(v.totals.grandTotal), v.buyer.name, v.status.name, f.linkId == v.id)
            },
            amountHint = amountHint,
            linkedNo = li?.displayNo,
            linkedParty = li?.buyer?.name.orEmpty(),
            grand = grand,
            tenderPlaceholder = Fmt.f2(grand),
            tenderChips = roundUps.mapIndexed { i, v -> TenderChipUi(if (i == 0) "Exact " + Fmt.f2(v) else Fmt.f2(v), v.toPlainString()) },
            change = if (f.tendered.isNotEmpty() && tendered != null && tendered >= grand) Fmt.f2(tendered - grand) else "0.00",
            problems = problems,
            billTo = if (needsLink) (li?.buyer?.name ?: "—") else if (f.walkIn) "Walk-in customer" else f.name.ifEmpty { "—" },
            loading = loading,
        )
    }

    private fun ceilTo(v: BigDecimal, step: Int): BigDecimal {
        val s = BigDecimal(step)
        return v.divide(s, 0, RoundingMode.CEILING) * s
    }
}
