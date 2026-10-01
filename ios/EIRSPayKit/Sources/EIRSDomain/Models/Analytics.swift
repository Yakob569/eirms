import Foundation

public enum DashboardRange: String, CaseIterable, Hashable, Sendable, Identifiable {
    case today, days7, days30, month, year
    public var id: String { rawValue }
    public var chipLabel: String {
        switch self {
        case .today: return "Today"
        case .days7: return "7 days"
        case .days30: return "30 days"
        case .month: return "This month"
        case .year: return "This year"
        }
    }
    public var label: String {
        switch self {
        case .today: return "Today"
        case .days7: return "Last 7 days"
        case .days30: return "Last 30 days"
        case .month: return "This month"
        case .year: return "This year"
        }
    }
    public var granularity: String {
        switch self {
        case .today: return "Hourly"
        case .year: return "Monthly"
        default: return "Daily"
        }
    }
}

public struct SalesPoint: Hashable, Sendable { public let label: String; public let sales: Double; public let tax: Double
    public init(label: String, sales: Double, tax: Double) { self.label = label; self.sales = sales; self.tax = tax } }

public struct TaxCodeAmount: Hashable, Sendable { public let code: TaxCode; public let label: String; public let tax: Double; public let barShare: Double
    public init(code: TaxCode, label: String, tax: Double, barShare: Double) { self.code = code; self.label = label; self.tax = tax; self.barShare = barShare } }

public struct TransactionTypeShare: Hashable, Sendable { public let type: TransactionType; public let share: Double; public let documents: Int; public let amount: Double
    public init(type: TransactionType, share: Double, documents: Int, amount: Double) { self.type = type; self.share = share; self.documents = documents; self.amount = amount } }

public struct BranchSales: Hashable, Sendable { public let branch: Branch; public let amount: Double; public let documents: Int; public let share: Double; public let sourceSystems: Int
    public init(branch: Branch, amount: Double, documents: Int, share: Double, sourceSystems: Int) { self.branch = branch; self.amount = amount; self.documents = documents; self.share = share; self.sourceSystems = sourceSystems } }

public struct SalesAnalytics: Hashable, Sendable {
    public let range: DashboardRange
    public let branch: Branch?
    public let points: [SalesPoint]
    public let totalSales: Double
    public let taxCollected: Double
    public let delta: String
    public let comparison: String
    public let invoicesIssued: Int
    public let receiptsIssued: Int
    public let canceledCount: Int
    public let canceledValue: Double
    public let withholding: Double
    public let taxByCode: [TaxCodeAmount]
    public let byTransactionType: [TransactionTypeShare]
    public let byBranch: [BranchSales]

    public init(range: DashboardRange, branch: Branch?, points: [SalesPoint], totalSales: Double, taxCollected: Double,
                delta: String, comparison: String, invoicesIssued: Int, receiptsIssued: Int, canceledCount: Int,
                canceledValue: Double, withholding: Double, taxByCode: [TaxCodeAmount],
                byTransactionType: [TransactionTypeShare], byBranch: [BranchSales]) {
        self.range = range; self.branch = branch; self.points = points; self.totalSales = totalSales
        self.taxCollected = taxCollected; self.delta = delta; self.comparison = comparison
        self.invoicesIssued = invoicesIssued; self.receiptsIssued = receiptsIssued; self.canceledCount = canceledCount
        self.canceledValue = canceledValue; self.withholding = withholding; self.taxByCode = taxByCode
        self.byTransactionType = byTransactionType; self.byBranch = byBranch
    }
}

public enum ActivityKind: Hashable, Sendable { case invoice, receipt, canceled }

public struct ActivityItem: Hashable, Sendable, Identifiable {
    public let kind: ActivityKind
    public let documentId: String
    public let number: String
    public let party: String
    public let branch: Branch
    public let transactionType: TransactionType?
    public let at: Date
    public let amount: Money
    public var id: String { documentId }

    public init(kind: ActivityKind, documentId: String, number: String, party: String, branch: Branch,
                transactionType: TransactionType?, at: Date, amount: Money) {
        self.kind = kind; self.documentId = documentId; self.number = number; self.party = party
        self.branch = branch; self.transactionType = transactionType; self.at = at; self.amount = amount
    }
}
