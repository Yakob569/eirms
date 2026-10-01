import SwiftUI
import EIRSDomain

private func invDecimal(_ text: String) -> Decimal {
    let trimmed = text.trimmingCharacters(in: .whitespaces)
    if trimmed.isEmpty { return Decimal(0) }
    return Decimal(string: trimmed, locale: Locale(identifier: "en_US_POSIX")) ?? Decimal(0)
}

/// One editable line of the form. Catalog-added lines carry the catalog item's code, nature and unit.
struct InvLineDraft: Identifiable, Equatable {
    let id: Int
    var description = ""
    var quantity = "1"
    var price = ""
    var taxCode: TaxCode = .vat15
    var itemCode: String?
    var nature: NatureOfSupply = .goods
    var unit = "PCS"

    var qty: Decimal { invDecimal(quantity) }
    var unitPrice: Decimal { invDecimal(price) }
    var preTax: Decimal { (qty * unitPrice).rounded() }
    var tax: Decimal { (preTax * taxCode.rate).rounded() }
    var total: Decimal { preTax + tax }
    var isEmpty: Bool {
        description.trimmingCharacters(in: .whitespaces).isEmpty && price.trimmingCharacters(in: .whitespaces).isEmpty
    }
    var isPriced: Bool {
        !description.trimmingCharacters(in: .whitespaces).isEmpty && unitPrice > Decimal(0)
    }
}

@MainActor @Observable final class RegisterInvoiceModel {
    var branch: Branch = .main
    var documentType: DocumentType = .inv
    var transactionType: TransactionType = .b2c
    var walkIn = true
    var idType: BuyerIdType = .nid
    var name = ""
    var idNumber = ""
    var tin = ""
    var vat = ""
    var address = ""
    var lookingUp = false
    var previousIrn = ""
    var amendReason = ""
    var lines: [InvLineDraft] = [InvLineDraft(id: 0)]
    var paymentMode: InvoicePaymentMode = .cash
    var paymentTerm: PaymentTerm = .immediate
    var quickAdd: [CatalogItem] = []
    var loading: LoaderMessage?

    @ObservationIgnored private let registerUseCase: RegisterInvoiceUseCase
    @ObservationIgnored private let lookupUseCase: LookupTaxpayerUseCase
    @ObservationIgnored private let observeCatalog: ObserveCatalogUseCase
    @ObservationIgnored private let observeInvoices: ObserveInvoicesUseCase
    @ObservationIgnored private var invoices: [Invoice] = []
    @ObservationIgnored private var nextKey = 1

    init(container: AppContainer) {
        branch = container.preferences.selectedBranch ?? .main
        registerUseCase = container.registerInvoice
        lookupUseCase = container.lookupTaxpayer
        observeCatalog = container.observeCatalog
        observeInvoices = container.observeInvoices
    }

    // MARK: Derived

    var consumer: Bool { transactionType.isConsumer }
    var grandTotal: Decimal { lines.map { $0.total }.sumMoney() }

    /// Prototype rule: one priced line, and a buyer name unless it's a walk-in consumer sale.
    var valid: Bool {
        let hasName = !name.trimmingCharacters(in: .whitespaces).isEmpty
        let buyerOk: Bool = consumer ? (walkIn || hasName) : hasName
        return lines.contains { $0.isPriced } && buyerOk
    }

    var billTo: String {
        if consumer && walkIn { return "Walk-in customer" }
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        return trimmed.isEmpty ? "—" : trimmed
    }

    // MARK: Streams

    func observeCatalogItems() async {
        for await items in observeCatalog() {
            quickAdd = Array(items.filter { $0.active }.prefix(5))
        }
    }

    func observeInvoiceList() async {
        for await list in observeInvoices() { invoices = list }
    }

    // MARK: Intents

    func setTin(_ value: String) {
        tin = String(value.filter { $0.isNumber }.prefix(10))
    }

    func lookup() async {
        if lookingUp { return }
        lookingUp = true
        let party = await lookupUseCase(tin: tin)
        if let party {
            name = party.name
            vat = party.vatNumber ?? ""
            address = party.address ?? ""
        }
        lookingUp = false
    }

    private func updateLine(_ id: Int, _ change: (inout InvLineDraft) -> Void) {
        guard let index = lines.firstIndex(where: { $0.id == id }) else { return }
        var line = lines[index]
        change(&line)
        lines[index] = line
    }

    func setLineDescription(_ id: Int, _ value: String) { updateLine(id) { $0.description = value } }
    func setLineQuantity(_ id: Int, _ value: String) { updateLine(id) { $0.quantity = value.decimalOnly } }
    func setLinePrice(_ id: Int, _ value: String) { updateLine(id) { $0.price = value.decimalOnly } }
    func setLineTaxCode(_ id: Int, _ code: TaxCode) { updateLine(id) { $0.taxCode = code } }

    func removeLine(_ id: Int) {
        if lines.count > 1 {
            lines = lines.filter { $0.id != id }
        } else {
            lines = [InvLineDraft(id: takeKey())]
        }
    }

    func addLine() { lines.append(InvLineDraft(id: takeKey())) }

    private func takeKey() -> Int {
        let key = nextKey
        nextKey += 1
        return key
    }

    /// Quick-add fills the first empty line, or appends a new one.
    func addCatalogItem(_ item: CatalogItem) {
        let emptyIndex: Int? = lines.firstIndex { $0.isEmpty }
        let key: Int = emptyIndex.map { lines[$0].id } ?? takeKey()
        var line = InvLineDraft(id: key)
        line.description = item.description
        line.quantity = "1"
        line.price = NSDecimalNumber(decimal: item.unitPrice).stringValue
        line.taxCode = item.taxCode
        line.itemCode = item.code
        line.nature = item.nature
        line.unit = item.unit
        if let emptyIndex {
            lines[emptyIndex] = line
        } else {
            lines.append(line)
        }
    }

    /// Registers the invoice; returns the new invoice id once MoR has signed it.
    func submit() async -> String? {
        if !valid || loading != nil { return nil }
        let draft = makeDraft()
        loading = LoaderMessage(title: "Registering with EIRS", subtitle: "Waiting for MoR to sign the invoice")
        var createdId: String?
        do {
            let created = try await registerUseCase(draft)
            createdId = created.id
        } catch {
            createdId = nil
        }
        loading = nil
        return createdId
    }

    private func makeDraft() -> InvoiceDraft {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedId = idNumber.trimmingCharacters(in: .whitespaces)
        let trimmedVat = vat.trimmingCharacters(in: .whitespaces)
        let trimmedAddress = address.trimmingCharacters(in: .whitespaces)
        let buyer: Party
        if consumer && walkIn {
            buyer = Party.walkIn
        } else if consumer {
            buyer = Party(name: trimmedName.isEmpty ? "Consumer" : trimmedName, idType: idType,
                          idNumber: trimmedId.isEmpty ? nil : trimmedId)
        } else {
            buyer = Party(name: trimmedName, tin: tin.trimmingCharacters(in: .whitespaces),
                          vatNumber: trimmedVat.isEmpty ? nil : trimmedVat,
                          address: trimmedAddress.isEmpty ? nil : trimmedAddress)
        }
        let priced: [InvLineDraft] = lines.filter { $0.isPriced }
        var items: [LineItem] = []
        for (index, line) in priced.enumerated() {
            let quantity: Decimal = line.qty > Decimal(0) ? line.qty : Decimal(1)
            items.append(LineItem(
                description: line.description.trimmingCharacters(in: .whitespaces),
                itemCode: line.itemCode ?? "ITM-\(900 + index)",
                nature: line.nature, unit: line.unit, quantity: quantity,
                unitPrice: line.unitPrice.rounded(), taxCode: line.taxCode))
        }
        var amendment: Amendment?
        if documentType.isAmendment {
            let reason = amendReason.trimmingCharacters(in: .whitespaces)
            amendment = Amendment(reason: reason.isEmpty ? "Amendment" : reason,
                                  previousInvoiceId: resolvePrevious(previousIrn))
        }
        return InvoiceDraft(documentType: documentType, transactionType: transactionType, branch: branch,
                            buyer: buyer, paymentMode: paymentMode, paymentTerm: paymentTerm,
                            items: items, amendment: amendment)
    }

    /// Matches the typed IRN (full, "ETH-" prefixed or partial) or document number against invoices on record.
    private func resolvePrevious(_ input: String) -> String {
        let trimmed = input.trimmingCharacters(in: .whitespaces)
        var q = trimmed
        if q.lowercased().hasPrefix("eth-") { q = String(q.dropFirst(4)) }
        if let range = q.range(of: "··") { q = String(q[..<range.lowerBound]) }
        if let range = q.range(of: "…") { q = String(q[..<range.lowerBound]) }
        q = q.lowercased()
        if q.isEmpty { return trimmed }
        let hit: Invoice? = invoices.first { $0.irn.lowercased() == q }
            ?? invoices.first { $0.number == q }
            ?? invoices.first { $0.irn.lowercased().hasPrefix(q) }
        return hit?.id ?? trimmed
    }
}
