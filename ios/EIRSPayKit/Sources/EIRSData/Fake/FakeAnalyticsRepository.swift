import Foundation
import EIRSDomain

/// Generates plausible sales series per range and branch (same seeded generator as the prototype).
/// No aggregate endpoint exists yet; once one does, this becomes a thin API call.
public final class FakeAnalyticsRepository: AnalyticsRepository, @unchecked Sendable {
    public init() {}

    private struct Config {
        let count: Int
        let label: (Int) -> String
        let base: Double
        let delta: String
        let comparison: String
        let seed: Int
    }

    private static let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep"]
    private static let today = ethiopiaDate("2026-09-25")

    private static func day(back: Int) -> String {
        let calendar = Calendar.ethiopia
        let date = calendar.date(byAdding: .day, value: -back, to: today) ?? today
        let parts = calendar.dateComponents([.month, .day], from: date)
        return "\(months[(parts.month ?? 1) - 1]) \(parts.day ?? 1)"
    }

    private func config(for range: DashboardRange) -> Config {
        switch range {
        case .today: return Config(count: 12, label: { String(format: "%02d:00", 8 + $0) }, base: 9200, delta: "8.2%", comparison: "vs yesterday", seed: 3)
        case .days7: return Config(count: 7, label: { Self.day(back: 6 - $0) }, base: 68000, delta: "12.4%", comparison: "vs previous 7 days", seed: 11)
        case .days30: return Config(count: 30, label: { Self.day(back: 29 - $0) }, base: 64000, delta: "6.1%", comparison: "vs previous 30 days", seed: 23)
        case .month: return Config(count: 25, label: { "Sep \($0 + 1)" }, base: 66000, delta: "9.7%", comparison: "vs Aug 1 – 25", seed: 31)
        case .year: return Config(count: 9, label: { Self.months[$0] }, base: 1_850_000, delta: "31.5%", comparison: "vs Jan – Sep 2025", seed: 47)
        }
    }

    private static let share: [Branch: Double] = [.main: 0.58, .bole: 0.29, .piassa: 0.13]

    public func salesAnalytics(range: DashboardRange, branch: Branch?) async -> SalesAnalytics {
        let c = config(for: range)
        let sh = branch.flatMap { Self.share[$0] } ?? 1.0
        var seed = c.seed
        func rnd() -> Double { seed = (seed * 9301 + 49297) % 233_280; return Double(seed) / 233_280.0 }
        // Split into typed steps: as one expression it exceeds the type checker's time limit.
        let series: [Double] = (0..<c.count).map { i -> Double in
            let noise: Double = 0.6 + 0.7 * rnd()
            let trend: Double = 0.85 + 0.3 * Double(i) / Double(c.count)
            let value: Double = sh * c.base * noise * trend
            return (value * 100).rounded() / 100
        }
        let sales = series.reduce(0, +)
        let taxable = sales / 1.125
        let tax = sales - taxable
        let docs = max(1, Int((sales / 3850).rounded()))
        let inv = max(1, Int((Double(docs) * 0.38).rounded()))
        let rcp = max(1, docs - inv)
        let canceled = max(1, Int((Double(docs) * 0.015).rounded()))
        let total = sales / sh
        let totalDocs = Int((total / 3850).rounded())

        let codes: [(TaxCode, String, Double)] = [(.vat15, "Standard VAT", 0.78), (.exc10, "Excise", 0.08), (.vat0, "Zero-rated", 0.14)]
        let types: [(TransactionType, Double)] = [(.b2c, 0.52), (.b2b, 0.31), (.b2g, 0.11), (.g2b, 0.04), (.g2c, 0.02)]

        return SalesAnalytics(
            range: range, branch: branch,
            points: series.enumerated().map { SalesPoint(label: c.label($0.offset), sales: $0.element, tax: $0.element - $0.element / 1.125) },
            totalSales: sales, taxCollected: tax, delta: c.delta, comparison: c.comparison,
            invoicesIssued: inv, receiptsIssued: rcp, canceledCount: canceled,
            canceledValue: Double(canceled) * (sales / Double(docs)) * 1.3, withholding: taxable * 0.0084,
            taxByCode: codes.map { code, label, s in
                TaxCodeAmount(code: code, label: label, tax: taxable * s * code.rate.doubleValue,
                              barShare: max(0.02, s * code.rate.doubleValue / 0.117))
            },
            byTransactionType: types.map { type, s in
                TransactionTypeShare(type: type, share: s, documents: max(1, Int((Double(docs) * s).rounded())), amount: sales * s)
            },
            byBranch: SampleData.branches.map { info in
                let s = Self.share[info.branch] ?? 0
                return BranchSales(branch: info.branch, amount: total * s, documents: Int((Double(totalDocs) * s).rounded()),
                                   share: s, sourceSystems: info.sourceSystems)
            })
    }
}
