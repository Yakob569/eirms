import Foundation

public struct LineItem: Hashable, Sendable {
    public var description: String
    public var itemCode: String
    public var nature: NatureOfSupply
    public var unit: String
    public var quantity: Decimal
    public var unitPrice: Money
    public var taxCode: TaxCode

    public init(description: String, itemCode: String, nature: NatureOfSupply, unit: String,
                quantity: Decimal, unitPrice: Money, taxCode: TaxCode) {
        self.description = description; self.itemCode = itemCode; self.nature = nature; self.unit = unit
        self.quantity = quantity; self.unitPrice = unitPrice; self.taxCode = taxCode
    }
}

public struct LineTotals: Hashable, Sendable {
    public let number: Int
    public let item: LineItem
    public let preTax: Money
    public let tax: Money
    public var total: Money { preTax + tax }
}

public struct TaxLine: Hashable, Sendable {
    public let code: TaxCode
    public let taxable: Money
    public let tax: Money
}

public struct DocumentTotals: Hashable, Sendable {
    public let lines: [LineTotals]
    public let subtotal: Money
    public let taxTotal: Money
    public let taxes: [TaxLine]
    public var grandTotal: Money { subtotal + taxTotal }

    public init(lines: [LineTotals], subtotal: Money, taxTotal: Money, taxes: [TaxLine]) {
        self.lines = lines; self.subtotal = subtotal; self.taxTotal = taxTotal; self.taxes = taxes
    }
    public static let empty = DocumentTotals(lines: [], subtotal: 0, taxTotal: 0, taxes: [])
}

public struct Cancellation: Hashable, Sendable {
    public let reason: CancelReason
    public let canceledAt: Date
    public let originalStatus: InvoiceStatus
    public init(reason: CancelReason, canceledAt: Date, originalStatus: InvoiceStatus) {
        self.reason = reason; self.canceledAt = canceledAt; self.originalStatus = originalStatus
    }
}

/// Credit/debit notes amend an earlier registered invoice.
public struct Amendment: Hashable, Sendable {
    public let reason: String
    public let previousInvoiceId: String
    public init(reason: String, previousInvoiceId: String) { self.reason = reason; self.previousInvoiceId = previousInvoiceId }
}

public struct SourceSystem: Hashable, Sendable {
    public let type: String
    public let number: String
    public init(type: String, number: String) { self.type = type; self.number = number }
}

/// An invoice registered with the Ministry of Revenues. `irn` is the fiscal signature MoR returns;
/// it stays on record even when the invoice is later canceled.
public struct Invoice: Hashable, Sendable, Identifiable {
    public let id: String
    public var number: String
    public var documentType: DocumentType
    public var transactionType: TransactionType
    public var status: InvoiceStatus
    public var issuedAt: Date
    public var branch: Branch
    public var sourceSystem: SourceSystem
    public var seller: Party
    public var buyer: Party
    public var paymentMode: InvoicePaymentMode
    public var paymentTerm: PaymentTerm
    public var items: [LineItem]
    public var irn: String
    public var amendment: Amendment?
    public var cancellation: Cancellation?

    public init(id: String, number: String, documentType: DocumentType, transactionType: TransactionType,
                status: InvoiceStatus, issuedAt: Date, branch: Branch, sourceSystem: SourceSystem, seller: Party,
                buyer: Party, paymentMode: InvoicePaymentMode, paymentTerm: PaymentTerm, items: [LineItem],
                irn: String, amendment: Amendment? = nil, cancellation: Cancellation? = nil) {
        self.id = id; self.number = number; self.documentType = documentType; self.transactionType = transactionType
        self.status = status; self.issuedAt = issuedAt; self.branch = branch; self.sourceSystem = sourceSystem
        self.seller = seller; self.buyer = buyer; self.paymentMode = paymentMode; self.paymentTerm = paymentTerm
        self.items = items; self.irn = irn; self.amendment = amendment; self.cancellation = cancellation
    }

    public var totals: DocumentTotals { TaxCalculator.calculate(items) }
    public var isCanceled: Bool { status == .canceled }
    /// "INV-100", "RTN-095": document type prefix plus number, as shown in menus and panels.
    public var displayNumber: String { "\(documentType.rawValue)-\(number)" }
    /// Sequential counter MoR tracks per source system.
    public var counter: String { String((Int(number) ?? 0) + 1000) }
}

public enum ReceiptType: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case sales = "SALES", payment = "PAYMENT", advance = "ADVANCE", refund = "REFUND"
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .sales: return "Sales"
        case .payment: return "Payment"
        case .advance: return "Advance"
        case .refund: return "Refund"
        }
    }
    public var summary: String {
        switch self {
        case .sales: return "Paid at the counter for goods or services"
        case .payment: return "Settles an invoice issued on credit"
        case .advance: return "Deposit before goods or services are delivered"
        case .refund: return "Money returned against an earlier invoice"
        }
    }
    /// Sales and advance receipts carry their own line items; payment and refund point at an invoice.
    public var carriesItems: Bool { self == .sales || self == .advance }
}

public enum PaymentMethod: String, CaseIterable, Codable, Hashable, Sendable, Identifiable {
    case cash = "CASH", cheque = "CHEQUE", bankTransfer = "BANK_TRANSFER", mobileMoney = "MOBILE_MONEY", card = "CARD"
    public var id: String { rawValue }
    public var displayName: String {
        switch self {
        case .cash: return "Cash"
        case .cheque: return "Cheque"
        case .bankTransfer: return "Bank transfer"
        case .mobileMoney: return "Mobile money"
        case .card: return "Card"
        }
    }
    public var requiresReference: Bool { self == .cheque || self == .bankTransfer || self == .mobileMoney }
}

public let mobileMoneyProviders = ["telebirr", "M-Pesa", "CBE Birr", "HelloCash"]

public struct Receipt: Hashable, Sendable, Identifiable {
    public let id: String
    public var number: String
    public var type: ReceiptType
    public var method: PaymentMethod
    public var issuedAt: Date
    public var branch: Branch
    public var sourceSystem: SourceSystem
    public var irn: String
    public var buyer: Party?
    public var items: [LineItem]
    public var reference: String?
    public var provider: String?
    public var tendered: Money?
    public var linkedInvoiceId: String?
    /// Explicit amount for receipts without items (payments, refunds).
    public var amount: Money?

    public init(id: String, number: String, type: ReceiptType, method: PaymentMethod, issuedAt: Date, branch: Branch,
                sourceSystem: SourceSystem, irn: String, buyer: Party? = nil, items: [LineItem] = [],
                reference: String? = nil, provider: String? = nil, tendered: Money? = nil,
                linkedInvoiceId: String? = nil, amount: Money? = nil) {
        self.id = id; self.number = number; self.type = type; self.method = method; self.issuedAt = issuedAt
        self.branch = branch; self.sourceSystem = sourceSystem; self.irn = irn; self.buyer = buyer; self.items = items
        self.reference = reference; self.provider = provider; self.tendered = tendered
        self.linkedInvoiceId = linkedInvoiceId; self.amount = amount
    }
}

public struct WithholdingRecord: Hashable, Sendable, Identifiable {
    public let id: String
    public let invoiceId: String
    public let rate: Decimal
    public let withheldOn: Date
    public let taxPeriod: String
    public var certificateReceived: Bool
    public init(id: String, invoiceId: String, rate: Decimal, withheldOn: Date, taxPeriod: String, certificateReceived: Bool) {
        self.id = id; self.invoiceId = invoiceId; self.rate = rate; self.withheldOn = withheldOn
        self.taxPeriod = taxPeriod; self.certificateReceived = certificateReceived
    }
}

public struct CatalogItem: Hashable, Sendable, Identifiable {
    public var code: String
    public var description: String
    public var nature: NatureOfSupply
    public var hsCode: String
    public var taxCode: TaxCode
    public var unit: String
    public var unitPrice: Money
    public var active: Bool
    public var category: String
    public var id: String { code }

    public init(code: String, description: String, nature: NatureOfSupply, hsCode: String, taxCode: TaxCode,
                unit: String, unitPrice: Money, active: Bool, category: String) {
        self.code = code; self.description = description; self.nature = nature; self.hsCode = hsCode
        self.taxCode = taxCode; self.unit = unit; self.unitPrice = unitPrice; self.active = active; self.category = category
    }

    public var grossPrice: Money { (unitPrice + unitPrice * taxCode.rate).rounded() }

    public func lineItem(quantity: Decimal = 1) -> LineItem {
        LineItem(description: description, itemCode: code, nature: nature, unit: unit,
                 quantity: quantity, unitPrice: unitPrice, taxCode: taxCode)
    }
}
