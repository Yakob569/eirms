package com.eirspay.domain.model

import java.time.LocalDateTime

enum class DashboardRange(val chipLabel: String, val label: String, val granularity: String) {
    TODAY("Today", "Today", "Hourly"),
    DAYS_7("7 days", "Last 7 days", "Daily"),
    DAYS_30("30 days", "Last 30 days", "Daily"),
    MONTH("This month", "This month", "Daily"),
    YEAR("This year", "This year", "Monthly"),
}

data class SalesPoint(val label: String, val sales: Double, val tax: Double)

data class TaxCodeAmount(val code: TaxCode, val label: String, val tax: Double, val barShare: Double)

data class TransactionTypeShare(val type: TransactionType, val share: Double, val documents: Int, val amount: Double)

data class BranchSales(val branch: Branch, val amount: Double, val documents: Int, val share: Double, val sourceSystems: Int)

data class SalesAnalytics(
    val range: DashboardRange,
    val branch: Branch?,
    val points: List<SalesPoint>,
    val totalSales: Double,
    val taxCollected: Double,
    val delta: String,
    val comparison: String,
    val invoicesIssued: Int,
    val receiptsIssued: Int,
    val canceledCount: Int,
    val canceledValue: Double,
    val withholding: Double,
    val taxByCode: List<TaxCodeAmount>,
    val byTransactionType: List<TransactionTypeShare>,
    val byBranch: List<BranchSales>,
)

enum class ActivityKind { INVOICE, RECEIPT, CANCELED }

data class ActivityItem(
    val kind: ActivityKind,
    val documentId: String,
    val number: String,
    val party: String,
    val branch: Branch,
    val transactionType: TransactionType?,
    val at: LocalDateTime,
    val amount: java.math.BigDecimal,
)
