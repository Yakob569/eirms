import Foundation

// MARK: - Row actions

/// Options in an invoice card's ⋮ menu, in display order.
public enum InvoiceRowAction: Hashable, Sendable {
    case viewDetails, makeCreditNote, makeDebitNote, cancel, makeInterim, makeFinal
}

/// Options in a receipt card's ⋮ menu.
public enum ReceiptRowAction: Hashable, Sendable {
    case viewDetails
    case viewLinkedInvoice(String)
}

public enum RowActions {
    /// Whether a live (not canceled) credit or debit note already amends `invoiceId`.
    public static func hasAmendment(_ type: DocumentType, for invoiceId: String, in all: [Invoice]) -> Bool {
        all.contains { $0.documentType == type && $0.amendment?.previousInvoiceId == invoiceId && !$0.isCanceled }
    }

    /// INV: details, credit note and debit note (each hidden once one exists), cancel.
    /// CRE/DEB/INT/FIN and canceled rows: details only. RTN: details, interim and final invoice.
    public static func forInvoice(_ invoice: Invoice, among all: [Invoice]) -> [InvoiceRowAction] {
        var actions: [InvoiceRowAction] = [.viewDetails]
        guard !invoice.isCanceled else { return actions }
        switch invoice.documentType {
        case .inv:
            if !hasAmendment(.cre, for: invoice.id, in: all) { actions.append(.makeCreditNote) }
            if !hasAmendment(.deb, for: invoice.id, in: all) { actions.append(.makeDebitNote) }
            actions.append(.cancel)
        case .rtn:
            actions.append(contentsOf: [.makeInterim, .makeFinal])
        case .cre, .deb, .int, .fin:
            break
        }
        return actions
    }

    /// "View linked invoice" only when the receipt already shows a linked-invoice chip.
    public static func forReceipt(_ view: ReceiptView) -> [ReceiptRowAction] {
        var actions: [ReceiptRowAction] = [.viewDetails]
        if let linked = view.linkedInvoice { actions.append(.viewLinkedInvoice(linked.id)) }
        return actions
    }
}

// MARK: - Credit / debit notes

/// New quantity and unit price for one ticked line of the original invoice.
public struct LineAdjustment: Hashable, Sendable {
    public var quantity: Decimal
    public var unitPrice: Money
    public init(quantity: Decimal, unitPrice: Money) { self.quantity = quantity; self.unitPrice = unitPrice }
}

public enum AdjustmentIssue: Hashable, Sendable {
    case invalidSource
    case noLines
    case missingReason
    /// Credit notes can't exceed the original line's quantity or unit price.
    case exceedsOriginal(line: Int)
}

/// A credit/debit note is a constrained copy of the original: same buyer, branch, transaction type and
/// payment; only the ticked lines, with their new quantity and price.
public enum AdjustmentNote {
    public static func exceeds(kind: DocumentType, original line: LineItem, adjustment: LineAdjustment) -> Bool {
        kind == .cre && (adjustment.quantity > line.quantity || adjustment.unitPrice > line.unitPrice)
    }

    /// Ticked lines (by index into the original's items), in original order.
    public static func items(original: Invoice, adjustments: [Int: LineAdjustment]) -> [LineItem] {
        original.items.indices.compactMap { index -> LineItem? in
            guard let adjustment = adjustments[index] else { return nil }
            var item = original.items[index]
            item.quantity = adjustment.quantity
            item.unitPrice = adjustment.unitPrice.rounded()
            return item
        }
    }

    public static func totals(original: Invoice, adjustments: [Int: LineAdjustment]) -> DocumentTotals {
        TaxCalculator.calculate(items(original: original, adjustments: adjustments))
    }

    public static func issues(kind: DocumentType, original: Invoice, adjustments: [Int: LineAdjustment],
                              reason: String) -> [AdjustmentIssue] {
        var out: [AdjustmentIssue] = []
        if !kind.isAmendment || original.documentType != .inv || original.isCanceled { out.append(.invalidSource) }
        let ticked = adjustments.keys.filter { original.items.indices.contains($0) }
        if ticked.isEmpty { out.append(.noLines) }
        if reason.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { out.append(.missingReason) }
        for index in ticked.sorted() {
            if let adjustment = adjustments[index], exceeds(kind: kind, original: original.items[index], adjustment: adjustment) {
                out.append(.exceedsOriginal(line: index))
            }
        }
        return out
    }

    public static func draft(kind: DocumentType, original: Invoice, adjustments: [Int: LineAdjustment], reason: String) -> InvoiceDraft {
        InvoiceDraft(documentType: kind, transactionType: original.transactionType, branch: original.branch,
                     buyer: original.buyer, paymentMode: original.paymentMode, paymentTerm: original.paymentTerm,
                     items: items(original: original, adjustments: adjustments),
                     amendment: Amendment(reason: reason.trimmingCharacters(in: .whitespacesAndNewlines),
                                          previousInvoiceId: original.id))
    }
}

public enum DocumentRuleError: Error, Equatable, Sendable {
    case adjustment([AdjustmentIssue])
    case retainer([RetainerIssue])
}

public struct RegisterAdjustmentNoteUseCase: Sendable {
    let invoices: InvoiceRepository
    public init(invoices: InvoiceRepository) { self.invoices = invoices }

    public func callAsFunction(kind: DocumentType, original: Invoice, adjustments: [Int: LineAdjustment],
                               reason: String) async throws -> Invoice {
        let problems = AdjustmentNote.issues(kind: kind, original: original, adjustments: adjustments, reason: reason)
        guard problems.isEmpty else { throw DocumentRuleError.adjustment(problems) }
        return try await invoices.register(AdjustmentNote.draft(kind: kind, original: original, adjustments: adjustments, reason: reason))
    }
}

// MARK: - Interim / final invoices against a retainer

public enum RetainerIssue: Hashable, Sendable {
    case invalidSource
    case missingBuyerName
    case noPricedLines
}

/// INT/FIN invoices are full invoices: any buyer and items; only the retainer reference and type are fixed.
public enum RetainerInvoice {
    public static func issues(kind: DocumentType, retainer: Invoice, buyer: Party, items: [LineItem]) -> [RetainerIssue] {
        var out: [RetainerIssue] = []
        if !kind.isRetainerFollowUp || retainer.documentType != .rtn || retainer.isCanceled { out.append(.invalidSource) }
        if buyer.name.trimmingCharacters(in: .whitespaces).isEmpty { out.append(.missingBuyerName) }
        if !items.contains(where: { !$0.description.trimmingCharacters(in: .whitespaces).isEmpty && $0.unitPrice > 0 }) {
            out.append(.noPricedLines)
        }
        return out
    }

    public static func draft(kind: DocumentType, retainer: Invoice, buyer: Party, paymentMode: InvoicePaymentMode,
                             paymentTerm: PaymentTerm, items: [LineItem]) -> InvoiceDraft {
        let priced = items.filter { !$0.description.trimmingCharacters(in: .whitespaces).isEmpty && $0.unitPrice > 0 }
        return InvoiceDraft(documentType: kind, transactionType: retainer.transactionType, branch: retainer.branch,
                            buyer: buyer, paymentMode: paymentMode, paymentTerm: paymentTerm, items: priced,
                            amendment: Amendment(reason: "\(kind.displayName) against retainer \(retainer.displayNumber)",
                                                 previousInvoiceId: retainer.id))
    }
}

public struct RegisterRetainerInvoiceUseCase: Sendable {
    let invoices: InvoiceRepository
    public init(invoices: InvoiceRepository) { self.invoices = invoices }

    public func callAsFunction(kind: DocumentType, retainer: Invoice, buyer: Party, paymentMode: InvoicePaymentMode,
                               paymentTerm: PaymentTerm, items: [LineItem]) async throws -> Invoice {
        let problems = RetainerInvoice.issues(kind: kind, retainer: retainer, buyer: buyer, items: items)
        guard problems.isEmpty else { throw DocumentRuleError.retainer(problems) }
        return try await invoices.register(RetainerInvoice.draft(kind: kind, retainer: retainer, buyer: buyer,
                                                                 paymentMode: paymentMode, paymentTerm: paymentTerm, items: items))
    }
}
