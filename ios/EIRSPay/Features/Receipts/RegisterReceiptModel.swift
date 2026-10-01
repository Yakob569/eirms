import SwiftUI
import EIRSDomain

struct ReceiptRegLine: Hashable {
    var code: String
    var quantity: Int
}

struct ReceiptRegForm {
    var type: ReceiptType = .sales
    var method: PaymentMethod = .cash
    var linkId: String?
    var amount: String = ""
    var walkIn: Bool = true
    var name: String = ""
    var phone: String = ""
    var reference: String = ""
    var provider: String = mobileMoneyProviders.first ?? "telebirr"
    var tendered: String = ""
    /// Catalog code and quantity, in the order added.
    var lines: [ReceiptRegLine] = []
    var query: String = ""
}

struct ReceiptCatalogTile: Identifiable {
    let code: String
    let description: String
    let price: String
    let meta: String
    let inCart: Bool
    var id: String { code }
}

struct ReceiptCartLine: Identifiable {
    let index: Int
    let description: String
    let total: String
    let quantity: String
    var id: Int { index }
}

struct ReceiptLinkOption: Identifiable {
    let id: String
    let number: String
    let total: String
    let party: String
    let status: String
    let selected: Bool
}

struct ReceiptTenderChip: Identifiable {
    let label: String
    let value: String
    var id: String { label }
}

@MainActor @Observable
final class RegisterReceiptModel {
    private let container: AppContainer

    var form = ReceiptRegForm()
    var loading: LoaderMessage?

    private var catalog: [CatalogItem] = []
    private var invoices: [Invoice] = []
    private var withholdingRecords: [WithholdingRecord] = []
    private var branch: Branch?

    init(container: AppContainer) {
        self.container = container
        self.branch = container.preferences.selectedBranch
    }

    // MARK: Observation

    func observeCatalog() async {
        for await value in container.observeCatalog() { catalog = value }
    }

    func observeInvoices() async {
        for await value in container.observeInvoices() { invoices = value }
    }

    func observeWithholding() async {
        for await value in container.observeWithholding() { withholdingRecords = value.map { $0.record } }
    }

    func observeBranch() async {
        for await value in container.preferences.observeSelectedBranch() { branch = value }
    }

    // MARK: Derived state

    var needsItems: Bool { form.type.carriesItems }
    var needsLink: Bool { !needsItems }
    var refund: Bool { form.type == .refund }
    var ready: Bool { problems.isEmpty }
    var fromLabel: String { refund ? "Refund to" : "Received from" }
    var typeCaps: String { form.type.displayName.uppercased() + " · " + form.method.displayName.uppercased() }
    var grandText: String { (refund ? "−" : "") + Fmt.etb(grand) }
    var amountLabel: String { refund ? "Refund amount *" : "Amount received *" }
    var linkNote: String { form.type == .payment ? "Open B2B / B2G invoices" : "Invoice being refunded" }
    var branchCaps: String { (branch ?? .main).displayName.uppercased() }

    var referenceLabel: String {
        switch form.method {
        case .cheque: return "Cheque number *"
        case .bankTransfer: return "Transfer reference *"
        case .mobileMoney: return "Transaction ID *"
        case .card: return "Card (last 4 digits)"
        case .cash: return ""
        }
    }

    var referencePlaceholder: String {
        switch form.method {
        case .cheque: return "Bank and cheque no."
        case .bankTransfer: return "e.g. FT26267AB91C"
        case .mobileMoney: return "From the payment SMS"
        case .card: return "•••• 1234"
        case .cash: return ""
        }
    }

    private var byCode: [String: CatalogItem] {
        var map: [String: CatalogItem] = [:]
        for item in catalog { map[item.code] = item }
        return map
    }

    private var lineItems: [LineItem] {
        let lookup = byCode
        var items: [LineItem] = []
        for line in form.lines {
            if let item = lookup[line.code] { items.append(item.lineItem(quantity: Decimal(line.quantity))) }
        }
        return items
    }

    private var totals: DocumentTotals { TaxCalculator.calculate(lineItems) }

    var grand: Decimal {
        if needsItems { return totals.grandTotal }
        return Fmt.parseMoney(form.amount) ?? 0
    }

    private var linkedInvoice: Invoice? {
        guard let id = form.linkId else { return nil }
        return invoices.first { $0.id == id }
    }

    private var tenderedValue: Decimal? { Fmt.parseMoney(form.tendered) }

    var problems: [String] {
        var list: [String] = []
        let total = grand
        if needsItems && form.lines.isEmpty { list.append("add at least one item") }
        if needsLink && linkedInvoice == nil { list.append("pick the linked invoice") }
        if needsLink && total <= 0 { list.append("enter the amount") }
        if form.method == .cash && !form.tendered.isEmpty && (tenderedValue ?? 0) < total {
            list.append("tendered is less than the total")
        }
        if form.method.requiresReference && form.reference.trimmingCharacters(in: .whitespaces).isEmpty {
            switch form.method {
            case .cheque: list.append("add the cheque number")
            case .bankTransfer: list.append("add the transfer reference")
            default: list.append("add the transaction ID")
            }
        }
        if !needsLink && !form.walkIn && form.name.trimmingCharacters(in: .whitespaces).isEmpty {
            list.append("add customer name")
        }
        return list
    }

    var catalogNote: String { "\(catalog.filter { $0.active }.count) active in Items" }

    var catalogTiles: [ReceiptCatalogTile] {
        let q = form.query.trimmingCharacters(in: .whitespaces).lowercased()
        let active = catalog.filter { $0.active }
        let matching = active.filter { q.isEmpty || "\($0.code) \($0.description)".lowercased().contains(q) }
        return matching.prefix(9).map { item in
            ReceiptCatalogTile(code: item.code, description: item.description, price: Fmt.f2(item.unitPrice),
                               meta: "\(item.taxCode.rawValue) · \(item.unit)",
                               inCart: form.lines.contains { $0.code == item.code })
        }
    }

    var cartLines: [ReceiptCartLine] {
        let lookup = byCode
        let lineTotals = totals.lines
        var out: [ReceiptCartLine] = []
        for (index, line) in form.lines.enumerated() {
            guard let item = lookup[line.code] else { continue }
            let total: Decimal = index < lineTotals.count ? lineTotals[index].total : 0
            out.append(ReceiptCartLine(index: index, description: item.description, total: Fmt.f2(total),
                                       quantity: String(line.quantity)))
        }
        return out
    }

    var linkOptions: [ReceiptLinkOption] {
        let type = form.type
        let eligible = invoices.filter { v in
            guard v.documentType == .inv && v.status != .canceled else { return false }
            if type != .payment { return true }
            return (v.status == .issued || v.status == .withheld) && !v.transactionType.isConsumer
        }
        return eligible.map { v in
            ReceiptLinkOption(id: v.id, number: ReceiptStyle.invoiceNo(v), total: Fmt.etb(v.totals.grandTotal),
                              party: v.buyer.name, status: v.status.rawValue, selected: form.linkId == v.id)
        }
    }

    var amountHint: String {
        guard let li = linkedInvoice else { return "Pick an invoice to prefill the amount." }
        if form.type == .payment {
            if withholdingRecords.contains(where: { $0.invoiceId == li.id }) {
                return "Prefilled with the invoice total less the 2% the buyer withheld. Change it for a part payment."
            }
            return "Prefilled with the invoice total. Change it for a part payment."
        }
        return "Prefilled with the full invoice total. Lower it for a partial refund."
    }

    var linkedNo: String? { linkedInvoice.map { ReceiptStyle.invoiceNo($0) } }
    var linkedParty: String { linkedInvoice?.buyer.name ?? "" }
    var tenderPlaceholder: String { Fmt.f2(grand) }

    var tenderChips: [ReceiptTenderChip] {
        let total = grand
        guard total > 0 else { return [] }
        var options: [Decimal] = []
        let candidates: [Decimal] = [total, ceilTo(total, 100), ceilTo(total, 500), ceilTo(total, 1000)]
        for value in candidates where !options.contains(value) { options.append(value) }
        var chips: [ReceiptTenderChip] = []
        for (index, value) in options.prefix(4).enumerated() {
            let label: String = index == 0 ? "Exact " + Fmt.f2(value) : Fmt.f2(value)
            chips.append(ReceiptTenderChip(label: label, value: String(format: "%.2f", value.doubleValue)))
        }
        return chips
    }

    var change: String {
        if !form.tendered.isEmpty, let tendered = tenderedValue, tendered >= grand { return Fmt.f2(tendered - grand) }
        return "0.00"
    }

    var billTo: String {
        if needsLink { return linkedInvoice?.buyer.name ?? "—" }
        if form.walkIn { return "Walk-in customer" }
        return form.name.isEmpty ? "—" : form.name
    }

    private func ceilTo(_ value: Decimal, _ step: Int) -> Decimal {
        let size = Decimal(step)
        var quotient: Decimal = value / size
        var rounded = Decimal()
        NSDecimalRound(&rounded, &quotient, 0, .up)
        return rounded * size
    }

    // MARK: Intents

    func pickType(_ type: ReceiptType) {
        form.type = type
        form.linkId = nil
        form.amount = ""
    }

    func addItem(_ code: String) {
        if let index = form.lines.firstIndex(where: { $0.code == code }) {
            form.lines[index].quantity += 1
        } else {
            form.lines.append(ReceiptRegLine(code: code, quantity: 1))
        }
    }

    func increment(_ index: Int) {
        guard form.lines.indices.contains(index) else { return }
        form.lines[index].quantity += 1
    }

    func decrement(_ index: Int) {
        guard form.lines.indices.contains(index) else { return }
        if form.lines[index].quantity > 1 {
            form.lines[index].quantity -= 1
        } else {
            form.lines.remove(at: index)
        }
    }

    func pickLink(_ id: String) {
        guard let invoice = invoices.first(where: { $0.id == id }) else { return }
        form.linkId = id
        form.amount = String(format: "%.2f", defaultAmount(form.type, invoice).doubleValue)
    }

    func pickMethod(_ method: PaymentMethod) {
        form.method = method
        form.reference = ""
    }

    func setAmount(_ value: String) { form.amount = value.decimalOnly }
    func setTendered(_ value: String) { form.tendered = value.decimalOnly }

    private func defaultAmount(_ type: ReceiptType, _ invoice: Invoice) -> Decimal {
        let record = withholdingRecords.first { $0.invoiceId == invoice.id }
        if type == .payment, let record {
            return invoice.totals.grandTotal - (invoice.totals.subtotal * record.rate).rounded()
        }
        return invoice.totals.grandTotal
    }

    /// Registers the receipt and returns its id, or nil when not ready or the call failed.
    func submit() async -> String? {
        guard ready, loading == nil else { return nil }
        let f = form
        let li = linkedInvoice
        let total = grand
        let phone = f.phone.trimmingCharacters(in: .whitespaces)
        let buyer: Party?
        if needsLink {
            buyer = nil
        } else if f.walkIn {
            buyer = Party.walkIn
        } else if phone.count == 10 && phone.allSatisfy({ $0.isNumber }) {
            buyer = Party(name: f.name.trimmingCharacters(in: .whitespaces), tin: phone)
        } else {
            buyer = Party(name: f.name.trimmingCharacters(in: .whitespaces), phone: phone.isEmpty ? nil : phone)
        }
        let reference = f.reference.trimmingCharacters(in: .whitespaces)
        var tendered: Decimal?
        if f.method == .cash {
            if let value = Fmt.parseMoney(f.tendered), value > 0 { tendered = value } else { tendered = total }
        }
        let draftBranch: Branch = (needsLink && li != nil) ? (li?.branch ?? .main) : (branch ?? .main)
        let draft = ReceiptDraft(
            type: f.type, method: f.method, branch: draftBranch, buyer: buyer,
            items: needsLink ? [] : lineItems,
            reference: reference.isEmpty ? nil : reference,
            provider: f.method == .mobileMoney ? f.provider : nil,
            tendered: tendered,
            linkedInvoiceId: needsLink ? f.linkId : nil,
            amount: needsLink ? total : nil)
        loading = LoaderMessage(title: "Registering receipt", subtitle: "Waiting for MoR to sign it")
        defer { loading = nil }
        do {
            let receipt = try await container.registerReceipt(draft)
            return receipt.id
        } catch {
            return nil
        }
    }
}
