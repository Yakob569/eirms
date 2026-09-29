package com.eirspay.mobile.feature.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.CatalogItem
import com.eirspay.domain.model.DocumentTotals
import com.eirspay.domain.model.Party
import com.eirspay.domain.model.PaymentMethod
import com.eirspay.domain.model.ReceiptType
import com.eirspay.domain.model.TaxCalculator
import com.eirspay.domain.model.UserRole
import com.eirspay.domain.model.ZeroMoney
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.repository.ReceiptDraft
import com.eirspay.domain.usecase.LookupTaxpayerUseCase
import com.eirspay.domain.usecase.ObserveCatalogUseCase
import com.eirspay.domain.usecase.RegisterReceiptUseCase
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class BuyerMode(val label: String) { WALKIN("Walk-in"), PHONE("Phone"), BUSINESS("Business") }

enum class TerminalPay(val label: String, val method: PaymentMethod, val provider: String?) {
    CASH("Cash", PaymentMethod.CASH, null),
    MOBILE("telebirr", PaymentMethod.MOBILE_MONEY, "telebirr"),
    CARD("Card", PaymentMethod.CARD, null),
    TRANSFER("Transfer", PaymentMethod.BANK_TRANSFER, null),
}

data class CartLine(val item: CatalogItem, val qty: Int)

data class SaleLine(val qty: Int, val name: String, val total: String)

data class TerminalSale(
    val number: String,
    val irn: String,
    val total: BigDecimal,
    val change: BigDecimal,
    val pay: TerminalPay,
    val time: String,
    val buyer: String,
    val lines: List<SaleLine>,
) {
    val hasChange: Boolean get() = pay == TerminalPay.CASH && change > BigDecimal("0.004")
}

data class TerminalUiState(
    val staffName: String = "Yakob Girma",
    val staffRole: String = "Owner",
    val items: List<CatalogItem> = emptyList(),
    val category: String = "All",
    val query: String = "",
    val cart: List<CartLine> = emptyList(),
    val buyer: BuyerMode = BuyerMode.WALKIN,
    val tin: String = "",
    val business: Party? = null,
    val lookingUp: Boolean = false,
    val phone: String = "",
    val pay: TerminalPay = TerminalPay.CASH,
    val tendered: BigDecimal? = null,
    val sale: TerminalSale? = null,
    val sales: List<TerminalSale> = emptyList(),
    val cartOpen: Boolean = false,
    val confirmOpen: Boolean = false,
    val busy: LoaderMessage? = null,
    val since: Long = System.currentTimeMillis(),
    val now: Long = System.currentTimeMillis(),
    val exited: Boolean = false,
) {
    val firstName: String get() = staffName.split(" ").first()
    val initials: String get() = staffName.split(" ").filter { it.isNotEmpty() }.joinToString("") { it.take(1) }.take(2).uppercase()

    val categories: List<String> get() = listOf("All") + items.map { it.category }.distinct()

    val tiles: List<CatalogItem>
        get() {
            val q = query.trim().lowercase()
            return items.filter { (category == "All" || it.category == category) && (q.isEmpty() || it.description.lowercase().contains(q)) }
        }

    fun qtyOf(code: String): Int = cart.firstOrNull { it.item.code == code }?.qty ?: 0

    val totals: DocumentTotals by lazy { TaxCalculator.calculate(cart.map { it.item.toLineItem(BigDecimal(it.qty)) }) }
    val total: BigDecimal get() = totals.grandTotal
    val count: Int get() = cart.sumOf { it.qty }
    val hasCart: Boolean get() = count > 0
    val isBusiness: Boolean get() = buyer == BuyerMode.BUSINESS
    val bizOk: Boolean get() = !isBusiness || (tin.length == 10 && tin.all { it.isDigit() } && business != null)
    val cashOk: Boolean get() = pay != TerminalPay.CASH || tendered == null || tendered >= total
    val ok: Boolean get() = count > 0 && bizOk && cashOk
    val cartCount: String get() = Fmt.plural(count, "item")

    /** Exact + round-ups to the next 100 / 500 / 1000. */
    val tenderOptions: List<BigDecimal>
        get() {
            val t = total
            if (t.signum() <= 0) return emptyList()
            fun up(step: Int): BigDecimal = t.divide(BigDecimal(step), 0, RoundingMode.CEILING).multiply(BigDecimal(step)).setScale(2)
            val out = mutableListOf<BigDecimal>()
            listOf(t.setScale(2), up(100), up(500), up(1000)).forEach { v -> if (out.none { it.compareTo(v) == 0 }) out += v }
            return out.take(4)
        }

    fun isTendered(v: BigDecimal): Boolean = tendered != null && tendered.compareTo(v) == 0

    val chargeSub: String
        get() = when {
            count == 0 -> "Add items first"
            !bizOk -> "Look up the buyer TIN"
            pay == TerminalPay.CASH && tendered != null -> "Change " + Fmt.f2((tendered - total).max(ZeroMoney))
            else -> cartCount + " · " + (if (isBusiness) "B2B" else "B2C") + " · " + pay.label
        }

    val shiftFor: String
        get() {
            val el = ((now - since) / 60000).toInt()
            return if (el < 60) "${el}m" else "${el / 60}h ${el % 60}m"
        }

    val clock: String get() = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))

    val shiftTotal: BigDecimal get() = sales.fold(ZeroMoney) { a, s -> a + s.total }
    val cashTotal: BigDecimal get() = sales.filter { it.pay == TerminalPay.CASH }.fold(ZeroMoney) { a, s -> a + s.total }
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val observeCatalog: ObserveCatalogUseCase,
    private val registerReceipt: RegisterReceiptUseCase,
    private val lookupTaxpayer: LookupTaxpayerUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TerminalUiState())
    val state: StateFlow<TerminalUiState> = _state.asStateFlow()

    private var lookupJob: Job? = null
    private var workJob: Job? = null

    init {
        viewModelScope.launch {
            auth.session.collect { s ->
                val user = s?.user
                _state.update {
                    it.copy(
                        staffName = user?.fullName?.takeIf { n -> n.isNotBlank() } ?: "Yakob Girma",
                        staffRole = when (user?.role) {
                            UserRole.ADMIN -> "Admin"
                            UserRole.CASHIER -> "Cashier"
                            else -> "Owner"
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            observeCatalog().collect { items -> _state.update { it.copy(items = items.filter { i -> i.active }) } }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                _state.update { it.copy(now = System.currentTimeMillis()) }
            }
        }
    }

    fun pickCategory(c: String) = _state.update { it.copy(category = c) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun add(item: CatalogItem) = _state.update { s ->
        val ex = s.cart.any { it.item.code == item.code }
        s.copy(cart = if (ex) s.cart.map { if (it.item.code == item.code) it.copy(qty = it.qty + 1) else it } else s.cart + CartLine(item, 1))
    }

    fun inc(index: Int) = _state.update { s -> s.copy(cart = s.cart.mapIndexed { j, l -> if (j == index) l.copy(qty = l.qty + 1) else l }) }

    fun dec(index: Int) = _state.update { s ->
        val l = s.cart.getOrNull(index) ?: return@update s
        val next = if (l.qty > 1) s.cart.mapIndexed { j, x -> if (j == index) x.copy(qty = x.qty - 1) else x } else s.cart.filterIndexed { j, _ -> j != index }
        s.copy(cart = next, cartOpen = s.cartOpen && next.isNotEmpty())
    }

    fun clearCart() = _state.update { it.copy(cart = emptyList(), tendered = null, cartOpen = false) }
    fun openCart() = _state.update { it.copy(cartOpen = true) }
    fun closeCart() = _state.update { it.copy(cartOpen = false) }

    fun pickBuyer(m: BuyerMode) = _state.update { it.copy(buyer = m) }
    fun setPhone(p: String) = _state.update { it.copy(phone = p) }
    fun setTin(t: String) {
        lookupJob?.cancel()
        _state.update { it.copy(tin = t.filter { c -> c.isDigit() }.take(10), business = null, lookingUp = false) }
    }

    fun lookup() {
        val tin = _state.value.tin
        if (tin.length != 10 || _state.value.lookingUp) return
        lookupJob?.cancel()
        _state.update { it.copy(lookingUp = true) }
        lookupJob = viewModelScope.launch {
            val party = try { lookupTaxpayer(tin) } catch (e: Exception) { null }
            _state.update { if (it.tin == tin) it.copy(business = party, lookingUp = false) else it.copy(lookingUp = false) }
        }
    }

    fun pickPay(p: TerminalPay) = _state.update { it.copy(pay = p, tendered = null) }
    fun pickTender(v: BigDecimal) = _state.update { it.copy(tendered = v) }

    fun charge(branch: Branch) {
        val s = _state.value
        if (!s.ok || s.busy != null) return
        _state.update { it.copy(busy = LoaderMessage("Registering with EIRS", "Waiting for MoR to sign RCP-…"), cartOpen = false) }
        workJob = viewModelScope.launch {
            val buyer: Party = when (s.buyer) {
                BuyerMode.WALKIN -> Party.WalkIn
                BuyerMode.PHONE -> if (s.phone.isNotBlank()) Party(name = Party.WalkIn.name, phone = s.phone.trim()) else Party.WalkIn
                BuyerMode.BUSINESS -> s.business ?: Party.WalkIn
            }
            val totals = s.totals
            val tendered = if (s.pay == TerminalPay.CASH) s.tendered else null
            try {
                val receipt = registerReceipt(
                    ReceiptDraft(
                        type = ReceiptType.SALES,
                        method = s.pay.method,
                        branch = branch,
                        buyer = buyer,
                        items = s.cart.map { it.item.toLineItem(BigDecimal(it.qty)) },
                        reference = null,
                        provider = s.pay.provider,
                        tendered = tendered,
                        linkedInvoiceId = null,
                        amount = null,
                    ),
                )
                val paid = tendered ?: totals.grandTotal
                val sale = TerminalSale(
                    number = "RCP-" + receipt.number,
                    irn = receipt.irn,
                    total = totals.grandTotal,
                    change = paid - totals.grandTotal,
                    pay = s.pay,
                    time = Fmt.time(receipt.issuedAt),
                    buyer = when {
                        s.isBusiness -> s.business?.name ?: "Walk-in"
                        s.buyer == BuyerMode.PHONE && s.phone.isNotBlank() -> s.phone.trim()
                        else -> "Walk-in"
                    },
                    lines = totals.lines.mapIndexed { i, l -> SaleLine(s.cart[i].qty, s.cart[i].item.description, Fmt.f2(l.total)) },
                )
                _state.update {
                    it.copy(
                        busy = null, sale = sale, sales = it.sales + sale,
                        cart = emptyList(), tendered = null, buyer = BuyerMode.WALKIN, tin = "", business = null, phone = "",
                        pay = TerminalPay.CASH,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(busy = null) }
            }
        }
    }

    fun newSale() = _state.update { it.copy(sale = null) }

    fun holdComplete() = _state.update { it.copy(confirmOpen = true, cartOpen = false) }
    fun closeExit() = _state.update { it.copy(confirmOpen = false) }

    /** Back is swallowed; it only dismisses the sheet or dialog on top, never leaves the terminal. */
    fun back() = _state.update { if (it.confirmOpen) it.copy(confirmOpen = false) else if (it.cartOpen) it.copy(cartOpen = false) else it }

    fun exit() {
        if (_state.value.busy != null) return
        _state.update { it.copy(busy = LoaderMessage("Leaving terminal mode", "Clearing this screen"), confirmOpen = false, cartOpen = false) }
        workJob = viewModelScope.launch {
            delay(1500)
            reset()
            _state.update { it.copy(exited = true) }
        }
    }

    fun consumeExit() = _state.update { it.copy(exited = false) }

    /** Wipes cart, buyer, sales and every overlay so nothing carries over to the next time terminal mode opens. */
    fun reset() {
        lookupJob?.cancel()
        _state.update { s ->
            val now = System.currentTimeMillis()
            TerminalUiState(staffName = s.staffName, staffRole = s.staffRole, items = s.items, since = now, now = now)
        }
    }

    override fun onCleared() {
        lookupJob?.cancel()
        workJob?.cancel()
        _state.value = TerminalUiState()
    }
}
