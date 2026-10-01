import Foundation

public struct ObserveInvoicesUseCase: Sendable {
    let invoices: InvoiceRepository
    public init(invoices: InvoiceRepository) { self.invoices = invoices }
    public func callAsFunction() -> AsyncStream<[Invoice]> { invoices.observeInvoices() }
}

public struct RegisterInvoiceUseCase: Sendable {
    let invoices: InvoiceRepository
    public init(invoices: InvoiceRepository) { self.invoices = invoices }
    public func callAsFunction(_ draft: InvoiceDraft) async throws -> Invoice { try await invoices.register(draft) }
}

public struct CancelInvoiceUseCase: Sendable {
    let invoices: InvoiceRepository
    public init(invoices: InvoiceRepository) { self.invoices = invoices }
    public func callAsFunction(invoiceId: String, reason: CancelReason) async throws -> Invoice {
        try await invoices.cancel(invoiceId: invoiceId, reason: reason)
    }
}

/// Resolves receipts against invoices and withholding so every screen shows the same amounts.
public struct ObserveReceiptViewsUseCase: Sendable {
    let receipts: ReceiptRepository
    let invoices: InvoiceRepository
    let withholding: WithholdingRepository

    public init(receipts: ReceiptRepository, invoices: InvoiceRepository, withholding: WithholdingRepository) {
        self.receipts = receipts; self.invoices = invoices; self.withholding = withholding
    }

    public func callAsFunction() -> AsyncStream<[ReceiptView]> {
        combineLatest3(receipts.observeReceipts(), invoices.observeInvoices(), withholding.observeRecords()) { r, i, w in
            r.map { Self.resolve($0, invoices: i, records: w) }
        }
    }

    public static func resolve(_ r: Receipt, invoices: [Invoice], records: [WithholdingRecord]) -> ReceiptView {
        let linked = r.linkedInvoiceId.flatMap { id in invoices.first { $0.id == id } }
        let buyer = r.buyer ?? linked?.buyer ?? Party(name: "—")
        if !r.items.isEmpty {
            let totals = TaxCalculator.calculate(r.items)
            return ReceiptView(receipt: r, buyer: buyer, totals: totals, amount: totals.grandTotal,
                               linkedInvoice: linked, withholding: nil, withheldAmount: 0)
        }
        let record = records.first { $0.invoiceId == r.linkedInvoiceId }
        var withheld: Money = 0
        if let record, let linked, r.type == .payment { withheld = (linked.totals.subtotal * record.rate).rounded() }
        let amount: Money = r.amount ?? linked.map { $0.totals.grandTotal - withheld } ?? 0
        return ReceiptView(
            receipt: r, buyer: buyer,
            totals: DocumentTotals(lines: [], subtotal: amount, taxTotal: 0, taxes: []),
            amount: amount, linkedInvoice: linked,
            withholding: withheld > 0 ? record : nil, withheldAmount: withheld)
    }
}

public struct RegisterReceiptUseCase: Sendable {
    let receipts: ReceiptRepository
    public init(receipts: ReceiptRepository) { self.receipts = receipts }
    public func callAsFunction(_ draft: ReceiptDraft) async throws -> Receipt { try await receipts.register(draft) }
}

public struct ObserveWithholdingUseCase: Sendable {
    let withholding: WithholdingRepository
    let receiptViews: ObserveReceiptViewsUseCase
    let invoices: InvoiceRepository

    public init(withholding: WithholdingRepository, receiptViews: ObserveReceiptViewsUseCase, invoices: InvoiceRepository) {
        self.withholding = withholding; self.receiptViews = receiptViews; self.invoices = invoices
    }

    public func callAsFunction() -> AsyncStream<[WithholdingView]> {
        combineLatest3(withholding.observeRecords(), invoices.observeInvoices(), receiptViews()) { records, inv, rcp in
            records.compactMap { rec -> WithholdingView? in
                guard let invoice = inv.first(where: { $0.id == rec.invoiceId }) else { return nil }
                let payment = rcp.first { $0.receipt.linkedInvoiceId == rec.invoiceId && $0.receipt.type == .payment }
                return WithholdingView(record: rec, invoice: invoice, paymentReceipt: payment)
            }
        }
    }
}

public struct ObserveCatalogUseCase: Sendable {
    let catalog: CatalogRepository
    public init(catalog: CatalogRepository) { self.catalog = catalog }
    public func callAsFunction() -> AsyncStream<[CatalogItem]> { catalog.observeItems() }
}

public enum CatalogValidation: Equatable, Sendable {
    case ok
    case duplicateCode
    case missing([String])
}

public struct ValidateCatalogItemUseCase: Sendable {
    public init() {}
    public func callAsFunction(_ item: CatalogItem, isNew: Bool, existing: [CatalogItem]) -> CatalogValidation {
        let code = item.code.trimmingCharacters(in: .whitespaces)
        if isNew && existing.contains(where: { $0.code.caseInsensitiveCompare(code) == .orderedSame }) { return .duplicateCode }
        var missing: [String] = []
        if item.code.trimmingCharacters(in: .whitespaces).isEmpty { missing.append("code") }
        if item.description.trimmingCharacters(in: .whitespaces).isEmpty { missing.append("description") }
        if item.nature == .goods && item.hsCode.isEmpty { missing.append("HS code") }
        if item.unitPrice <= 0 { missing.append("price") }
        return missing.isEmpty ? .ok : .missing(missing)
    }
}

public struct SaveCatalogItemUseCase: Sendable {
    let catalog: CatalogRepository
    public init(catalog: CatalogRepository) { self.catalog = catalog }
    public func callAsFunction(_ item: CatalogItem, isNew: Bool) async { await catalog.save(item, isNew: isNew) }
}

public struct SetCatalogItemActiveUseCase: Sendable {
    let catalog: CatalogRepository
    public init(catalog: CatalogRepository) { self.catalog = catalog }
    public func callAsFunction(code: String, active: Bool) async { await catalog.setActive(code: code, active: active) }
}

public struct MarkCertificateReceivedUseCase: Sendable {
    let withholding: WithholdingRepository
    public init(withholding: WithholdingRepository) { self.withholding = withholding }
    public func callAsFunction(id: String) async { await withholding.markCertificateReceived(id: id) }
}

public struct LookupTaxpayerUseCase: Sendable {
    let taxpayers: TaxpayerRepository
    public init(taxpayers: TaxpayerRepository) { self.taxpayers = taxpayers }
    public func callAsFunction(tin: String) async -> Party? {
        guard tin.count == 10, tin.allSatisfy(\.isNumber) else { return nil }
        return await taxpayers.lookup(tin: tin)
    }
}
