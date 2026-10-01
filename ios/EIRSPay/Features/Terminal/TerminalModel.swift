import SwiftUI
import Observation
import EIRSDomain

enum TerminalBuyerMode: CaseIterable, Hashable {
    case walkin, phone, business

    var label: String {
        switch self {
        case .walkin: return "Walk-in"
        case .phone: return "Phone"
        case .business: return "Business"
        }
    }
}

enum TerminalPay: CaseIterable, Hashable {
    case cash, mobile, card, transfer

    var label: String {
        switch self {
        case .cash: return "Cash"
        case .mobile: return "telebirr"
        case .card: return "Card"
        case .transfer: return "Transfer"
        }
    }

    var method: PaymentMethod {
        switch self {
        case .cash: return .cash
        case .mobile: return .mobileMoney
        case .card: return .card
        case .transfer: return .bankTransfer
        }
    }

    var provider: String? { self == .mobile ? "telebirr" : nil }
}

struct TerminalCartLine {
    var item: CatalogItem
    var qty: Int
}

struct TerminalSaleLine {
    var qty: Int
    var name: String
    var total: String
}

struct TerminalSale {
    var number: String
    var irn: String
    var total: Decimal
    var change: Decimal
    var pay: TerminalPay
    var time: String
    var buyer: String
    var lines: [TerminalSaleLine]

    var hasChange: Bool { pay == .cash && change > money("0.004") }
}

/// Terminal mode state: catalog, cart, buyer, tender, sales this shift and every overlay.
/// `reset()` wipes everything so nothing carries over to the next time terminal mode opens.
@MainActor @Observable
final class TerminalModel {
    var staffName = "Yakob Girma"
    var staffRole = "Owner"
    var items: [CatalogItem] = []
    var category = "All"
    var query = ""
    var cart: [TerminalCartLine] = []
    var buyer: TerminalBuyerMode = .walkin
    var tin = ""
    var business: Party?
    var lookingUp = false
    var phone = ""
    var pay: TerminalPay = .cash
    var tendered: Decimal?
    var sale: TerminalSale?
    var sales: [TerminalSale] = []
    var cartOpen = false
    var confirmOpen = false
    var busy: LoaderMessage?
    var since = Date()
    var now = Date()
    var exited = false

    @ObservationIgnored private let container: AppContainer
    @ObservationIgnored private let branch: Branch
    @ObservationIgnored private var lookupTask: Task<Void, Never>?
    @ObservationIgnored private var workTask: Task<Void, Never>?

    init(container: AppContainer, branch: Branch) {
        self.container = container
        self.branch = branch
    }

    // MARK: Streams

    func observeSession() async {
        for await session in container.auth.observeSession() {
            let user = session?.user
            if let user, !user.fullName.trimmingCharacters(in: .whitespaces).isEmpty {
                staffName = user.fullName
            } else {
                staffName = "Yakob Girma"
            }
            switch user?.role {
            case .some(.admin): staffRole = "Admin"
            case .some(.cashier): staffRole = "Cashier"
            default: staffRole = "Owner"
            }
        }
    }

    func observeCatalog() async {
        for await list in container.observeCatalog() {
            items = list.filter { $0.active }
        }
    }

    func tick() async {
        while !Task.isCancelled {
            do { try await Task.sleep(nanoseconds: 15_000_000_000) } catch { return }
            now = Date()
        }
    }

    // MARK: Derived

    var firstName: String { staffName.split(separator: " ").first.map(String.init) ?? staffName }

    var initials: String {
        let parts = staffName.split(separator: " ").filter { !$0.isEmpty }
        let letters = parts.map { String($0.prefix(1)) }.joined()
        return String(letters.prefix(2)).uppercased()
    }

    var categories: [String] {
        var out: [String] = ["All"]
        for item in items where !out.contains(item.category) { out.append(item.category) }
        return out
    }

    var tiles: [CatalogItem] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        return items.filter { item in
            let inCategory = category == "All" || item.category == category
            let matches = q.isEmpty || item.description.lowercased().contains(q)
            return inCategory && matches
        }
    }

    func qtyOf(_ code: String) -> Int {
        cart.first(where: { $0.item.code == code })?.qty ?? 0
    }

    var totals: DocumentTotals {
        TaxCalculator.calculate(cart.map { $0.item.lineItem(quantity: Decimal($0.qty)) })
    }

    var total: Decimal { totals.grandTotal }
    var count: Int { cart.reduce(0) { $0 + $1.qty } }
    var hasCart: Bool { count > 0 }
    var isBusiness: Bool { buyer == .business }
    var bizOk: Bool { !isBusiness || (tin.count == 10 && tin.allSatisfy { $0.isNumber } && business != nil) }

    var cashOk: Bool {
        if pay != .cash { return true }
        guard let tendered else { return true }
        return tendered >= total
    }

    var ok: Bool { count > 0 && bizOk && cashOk }
    var cartCount: String { Fmt.plural(count, "item") }

    /// Exact + round-ups to the next 100 / 500 / 1000.
    var tenderOptions: [Decimal] {
        let t = total
        if t <= 0 { return [] }
        var candidates: [Decimal] = [t]
        for step in [100, 500, 1000] {
            candidates.append(roundUp(t, toMultipleOf: step))
        }
        var out: [Decimal] = []
        for value in candidates where !out.contains(value) { out.append(value) }
        return Array(out.prefix(4))
    }

    private func roundUp(_ value: Decimal, toMultipleOf step: Int) -> Decimal {
        let s = Decimal(step)
        var quotient = value / s
        var rounded = Decimal()
        NSDecimalRound(&rounded, &quotient, 0, .up)
        return rounded * s
    }

    func isTendered(_ value: Decimal) -> Bool {
        guard let tendered else { return false }
        return tendered == value
    }

    var chargeSub: String {
        if count == 0 { return "Add items first" }
        if !bizOk { return "Look up the buyer TIN" }
        if pay == .cash, let tendered {
            let change = tendered - total
            return "Change " + Fmt.f2(change < 0 ? Decimal(0) : change)
        }
        let kind = isBusiness ? "B2B" : "B2C"
        return cartCount + " · " + kind + " · " + pay.label
    }

    var shiftFor: String {
        let minutes = max(0, Int(now.timeIntervalSince(since) / 60))
        return minutes < 60 ? "\(minutes)m" : "\(minutes / 60)h \(minutes % 60)m"
    }

    var clock: String { Fmt.time(now) }

    var shiftTotal: Decimal { sales.reduce(Decimal(0)) { $0 + $1.total } }
    var cashTotal: Decimal { sales.filter { $0.pay == .cash }.reduce(Decimal(0)) { $0 + $1.total } }

    // MARK: Intents

    func pickCategory(_ value: String) { category = value }
    func setQuery(_ value: String) { query = value }

    func add(_ item: CatalogItem) {
        if let index = cart.firstIndex(where: { $0.item.code == item.code }) {
            cart[index].qty += 1
        } else {
            cart.append(TerminalCartLine(item: item, qty: 1))
        }
    }

    func inc(_ index: Int) {
        guard cart.indices.contains(index) else { return }
        cart[index].qty += 1
    }

    func dec(_ index: Int) {
        guard cart.indices.contains(index) else { return }
        if cart[index].qty > 1 {
            cart[index].qty -= 1
        } else {
            cart.remove(at: index)
        }
        cartOpen = cartOpen && !cart.isEmpty
    }

    func clearCart() {
        cart = []
        tendered = nil
        cartOpen = false
    }

    func openCart() { cartOpen = true }
    func closeCart() { cartOpen = false }

    func pickBuyer(_ mode: TerminalBuyerMode) { buyer = mode }
    func setPhone(_ value: String) { phone = value }

    func setTin(_ value: String) {
        lookupTask?.cancel()
        tin = String(value.filter { $0.isNumber }.prefix(10))
        business = nil
        lookingUp = false
    }

    func lookup() {
        let value = tin
        if value.count != 10 || lookingUp { return }
        lookupTask?.cancel()
        lookingUp = true
        lookupTask = Task {
            let party = await container.lookupTaxpayer(tin: value)
            if Task.isCancelled { return }
            if tin == value { business = party }
            lookingUp = false
        }
    }

    func pickPay(_ value: TerminalPay) {
        pay = value
        tendered = nil
    }

    func pickTender(_ value: Decimal) { tendered = value }

    func requestCharge() {
        if !ok || busy != nil { return }
        busy = LoaderMessage(title: "Registering with EIRS", subtitle: "Waiting for MoR to sign RCP-…")
        cartOpen = false
        workTask = Task { await performCharge() }
    }

    private func performCharge() async {
        let lines = cart
        let totals = self.totals
        let payment = pay
        let mode = buyer
        let phoneText = phone.trimmingCharacters(in: .whitespaces)
        let businessParty = business
        let cashTendered: Decimal? = payment == .cash ? tendered : nil

        var party = Party.walkIn
        switch mode {
        case .walkin:
            party = Party.walkIn
        case .phone:
            if !phoneText.isEmpty { party = Party(name: Party.walkIn.name, phone: phoneText) }
        case .business:
            party = businessParty ?? Party.walkIn
        }

        let draft = ReceiptDraft(
            type: .sales, method: payment.method, branch: branch, buyer: party,
            items: lines.map { $0.item.lineItem(quantity: Decimal($0.qty)) },
            reference: nil, provider: payment.provider, tendered: cashTendered,
            linkedInvoiceId: nil, amount: nil)

        do {
            let receipt = try await container.registerReceipt(draft)
            let paid = cashTendered ?? totals.grandTotal
            var buyerLabel = "Walk-in"
            if mode == .business {
                buyerLabel = businessParty?.name ?? "Walk-in"
            } else if mode == .phone && !phoneText.isEmpty {
                buyerLabel = phoneText
            }
            var saleLines: [TerminalSaleLine] = []
            for (index, line) in totals.lines.enumerated() where index < lines.count {
                saleLines.append(TerminalSaleLine(qty: lines[index].qty, name: lines[index].item.description, total: Fmt.f2(line.total)))
            }
            let completed = TerminalSale(
                number: "RCP-" + receipt.number, irn: receipt.irn, total: totals.grandTotal,
                change: paid - totals.grandTotal, pay: payment, time: Fmt.time(receipt.issuedAt),
                buyer: buyerLabel, lines: saleLines)
            busy = nil
            sale = completed
            sales.append(completed)
            cart = []
            tendered = nil
            buyer = .walkin
            tin = ""
            business = nil
            phone = ""
            pay = .cash
        } catch {
            busy = nil
        }
    }

    func newSale() { sale = nil }

    func holdComplete() {
        confirmOpen = true
        cartOpen = false
    }

    func closeExit() { confirmOpen = false }

    /// Back is swallowed: it only dismisses the sheet or dialog on top, never leaves the terminal.
    func back() {
        if confirmOpen { confirmOpen = false } else if cartOpen { cartOpen = false }
    }

    func exit() {
        if busy != nil { return }
        busy = LoaderMessage(title: "Leaving terminal mode", subtitle: "Clearing this screen")
        confirmOpen = false
        cartOpen = false
        workTask = Task {
            do { try await Task.sleep(nanoseconds: 1_500_000_000) } catch { return }
            reset()
            exited = true
        }
    }

    func consumeExit() { exited = false }

    /// Wipes cart, buyer, sales and every overlay; keeps only the staff identity and the catalog.
    func reset() {
        lookupTask?.cancel()
        let date = Date()
        category = "All"
        query = ""
        cart = []
        buyer = .walkin
        tin = ""
        business = nil
        lookingUp = false
        phone = ""
        pay = .cash
        tendered = nil
        sale = nil
        sales = []
        cartOpen = false
        confirmOpen = false
        busy = nil
        since = date
        now = date
        exited = false
    }

    func teardown() {
        lookupTask?.cancel()
        workTask?.cancel()
    }
}
