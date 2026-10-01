import Foundation

/// A receipt with everything resolved that the UI needs: buyer, linked invoice, withholding and amount.
public struct ReceiptView: Hashable, Sendable, Identifiable {
    public let receipt: Receipt
    public let buyer: Party
    public let totals: DocumentTotals
    public let amount: Money
    public let linkedInvoice: Invoice?
    public let withholding: WithholdingRecord?
    public let withheldAmount: Money
    public var id: String { receipt.id }

    public init(receipt: Receipt, buyer: Party, totals: DocumentTotals, amount: Money,
                linkedInvoice: Invoice?, withholding: WithholdingRecord?, withheldAmount: Money) {
        self.receipt = receipt; self.buyer = buyer; self.totals = totals; self.amount = amount
        self.linkedInvoice = linkedInvoice; self.withholding = withholding; self.withheldAmount = withheldAmount
    }

    public var change: Money {
        guard let tendered = receipt.tendered else { return 0 }
        return max(tendered - amount, 0)
    }
}

public struct WithholdingView: Hashable, Sendable, Identifiable {
    public let record: WithholdingRecord
    public let invoice: Invoice
    public let paymentReceipt: ReceiptView?
    public var id: String { record.id }

    public init(record: WithholdingRecord, invoice: Invoice, paymentReceipt: ReceiptView?) {
        self.record = record; self.invoice = invoice; self.paymentReceipt = paymentReceipt
    }

    public var base: Money { invoice.totals.subtotal }
    public var amount: Money { (base * record.rate).rounded() }
    public var netPaid: Money { invoice.totals.grandTotal - amount }
}
