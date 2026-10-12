package com.eirspay.domain.usecase

import com.eirspay.domain.model.*
import com.eirspay.domain.repository.AnalyticsRepository
import com.eirspay.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSalesAnalyticsUseCase @Inject constructor(private val analytics: AnalyticsRepository) {
    suspend operator fun invoke(range: DashboardRange, branch: Branch?): SalesAnalytics = analytics.salesAnalytics(range, branch)
}

/** Latest invoices, receipts and cancellations, newest first. */
class ObserveRecentActivityUseCase @Inject constructor(
    private val invoices: InvoiceRepository,
    private val receipts: ObserveReceiptViewsUseCase,
) {
    operator fun invoke(limit: Int = 10): Flow<List<ActivityItem>> =
        combine(invoices.observeInvoices(), receipts()) { inv, rcp ->
            val fromInvoices = inv.map {
                ActivityItem(
                    kind = if (it.isCanceled) ActivityKind.CANCELED else ActivityKind.INVOICE,
                    documentId = it.id, number = "INV-${it.number}", party = it.buyer.name, branch = it.branch,
                    transactionType = it.transactionType, at = it.issuedAt, amount = it.totals.grandTotal,
                )
            }
            val fromReceipts = rcp.map {
                ActivityItem(
                    kind = ActivityKind.RECEIPT, documentId = it.receipt.id, number = "RCP-${it.receipt.number}",
                    party = it.buyer.name, branch = it.receipt.branch, transactionType = null,
                    at = it.receipt.issuedAt, amount = it.amount,
                )
            }
            (fromInvoices + fromReceipts).sortedByDescending { it.at }.take(limit)
        }
}
