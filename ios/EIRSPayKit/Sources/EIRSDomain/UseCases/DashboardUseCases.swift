import Foundation

public struct GetSalesAnalyticsUseCase: Sendable {
    let analytics: AnalyticsRepository
    public init(analytics: AnalyticsRepository) { self.analytics = analytics }
    public func callAsFunction(range: DashboardRange, branch: Branch?) async -> SalesAnalytics {
        await analytics.salesAnalytics(range: range, branch: branch)
    }
}

/// Latest invoices, receipts and cancellations, newest first.
public struct ObserveRecentActivityUseCase: Sendable {
    let invoices: InvoiceRepository
    let receipts: ObserveReceiptViewsUseCase

    public init(invoices: InvoiceRepository, receipts: ObserveReceiptViewsUseCase) {
        self.invoices = invoices; self.receipts = receipts
    }

    public func callAsFunction(limit: Int = 10) -> AsyncStream<[ActivityItem]> {
        combineLatest2(invoices.observeInvoices(), receipts()) { inv, rcp in
            let fromInvoices = inv.map { i in
                ActivityItem(kind: i.isCanceled ? .canceled : .invoice, documentId: i.id, number: "INV-\(i.number)",
                             party: i.buyer.name, branch: i.branch, transactionType: i.transactionType,
                             at: i.issuedAt, amount: i.totals.grandTotal)
            }
            let fromReceipts = rcp.map { r in
                ActivityItem(kind: .receipt, documentId: r.receipt.id, number: "RCP-\(r.receipt.number)",
                             party: r.buyer.name, branch: r.receipt.branch, transactionType: nil,
                             at: r.receipt.issuedAt, amount: r.amount)
            }
            return Array((fromInvoices + fromReceipts).sorted { $0.at > $1.at }.prefix(limit))
        }
    }
}
