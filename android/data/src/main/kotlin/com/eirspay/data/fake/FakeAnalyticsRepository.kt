package com.eirspay.data.fake

import com.eirspay.data.seed.SampleData
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.AnalyticsRepository
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Generates plausible sales series per range and branch (same seeded generator as the prototype).
 * No aggregate endpoint exists yet; once one does, this becomes a thin API call. Computing it from
 * listInvoices()/listReceipts() on-device is fine at today's volumes but won't scale.
 */
@Singleton
class FakeAnalyticsRepository @Inject constructor() : AnalyticsRepository {
    private val today = LocalDate.of(2026, 9, 25)

    private class Cfg(val n: Int, val label: (Int) -> String, val base: Double, val delta: String, val cmp: String, val seed: Long)

    private fun day(back: Int): String {
        val d = today.minusDays(back.toLong())
        return d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + d.dayOfMonth
    }

    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep")

    private fun cfg(range: DashboardRange) = when (range) {
        DashboardRange.TODAY -> Cfg(12, { (8 + it).toString().padStart(2, '0') + ":00" }, 9200.0, "8.2%", "vs yesterday", 3)
        DashboardRange.DAYS_7 -> Cfg(7, { day(6 - it) }, 68000.0, "12.4%", "vs previous 7 days", 11)
        DashboardRange.DAYS_30 -> Cfg(30, { day(29 - it) }, 64000.0, "6.1%", "vs previous 30 days", 23)
        DashboardRange.MONTH -> Cfg(25, { "Sep ${it + 1}" }, 66000.0, "9.7%", "vs Aug 1 – 25", 31)
        DashboardRange.YEAR -> Cfg(9, { months[it] }, 1_850_000.0, "31.5%", "vs Jan – Sep 2025", 47)
    }

    private val share = mapOf(Branch.MAIN to .58, Branch.BOLE to .29, Branch.PIASSA to .13)

    override suspend fun salesAnalytics(range: DashboardRange, branch: Branch?): SalesAnalytics {
        val c = cfg(range)
        val sh = branch?.let { share.getValue(it) } ?: 1.0
        var seed = c.seed
        fun rnd(): Double { seed = (seed * 9301 + 49297) % 233280; return seed / 233280.0 }
        val series = List(c.n) { i ->
            (sh * c.base * (0.6 + 0.7 * rnd()) * (0.85 + 0.3 * i / c.n) * 100).roundToInt() / 100.0
        }
        val sales = series.sum()
        val taxable = sales / 1.125
        val tax = sales - taxable
        val docs = max(1, (sales / 3850).roundToInt())
        val inv = max(1, (docs * .38).roundToInt())
        val rcp = max(1, docs - inv)
        val canc = max(1, (docs * .015).roundToInt())
        val total = sales / sh
        val totalDocs = (total / 3850).roundToInt()

        return SalesAnalytics(
            range = range, branch = branch,
            points = series.mapIndexed { i, v -> SalesPoint(c.label(i), v, v - v / 1.125) },
            totalSales = sales, taxCollected = tax, delta = c.delta, comparison = c.cmp,
            invoicesIssued = inv, receiptsIssued = rcp, canceledCount = canc, canceledValue = canc * (sales / docs) * 1.3,
            withholding = taxable * .0084,
            taxByCode = listOf(
                Triple(TaxCode.VAT15, "Standard VAT", .78), Triple(TaxCode.EXC10, "Excise", .08), Triple(TaxCode.VAT0, "Zero-rated", .14),
            ).map { (code, label, s) ->
                TaxCodeAmount(code, label, taxable * s * code.rate, max(0.02, s * code.rate / .117))
            },
            byTransactionType = listOf(
                TransactionType.B2C to .52, TransactionType.B2B to .31, TransactionType.B2G to .11,
                TransactionType.G2B to .04, TransactionType.G2C to .02,
            ).map { (t, s) -> TransactionTypeShare(t, s, max(1, (docs * s).roundToInt()), sales * s) },
            byBranch = SampleData.branches.map { info ->
                val s = share.getValue(info.branch)
                BranchSales(info.branch, total * s, (totalDocs * s).roundToInt(), s, info.sourceSystems)
            },
        )
    }
}
