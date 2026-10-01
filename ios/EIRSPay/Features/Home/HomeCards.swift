import SwiftUI
import EIRSDomain

// MARK: - Helpers

enum HomeFormat {
    private static let groupedFormatter: NumberFormatter = {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.numberStyle = .decimal
        f.usesGroupingSeparator = true
        f.groupingSeparator = ","
        f.maximumFractionDigits = 0
        return f
    }()

    static func grouped(_ n: Int) -> String {
        groupedFormatter.string(from: NSNumber(value: n)) ?? "\(n)"
    }

    /// One decimal place, dropping a trailing ".0".
    static func oneDecimal(_ v: Double) -> String {
        let r = (v * 10).rounded() / 10
        if r.truncatingRemainder(dividingBy: 1) == 0 { return String(Int(r)) }
        return String(r)
    }

    static func whenLabel(_ date: Date) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = ethiopiaTimeZone
        let now = Date()
        if calendar.isDate(date, inSameDayAs: now) { return "Today, " + Fmt.time(date) }
        if let yesterday = calendar.date(byAdding: .day, value: -1, to: now), calendar.isDate(date, inSameDayAs: yesterday) {
            return "Yesterday, " + Fmt.time(date)
        }
        return Fmt.dateTime(date)
    }
}

private struct HomeCardTitle: View {
    @Environment(\.eirs) private var eirs
    var text: String
    var body: some View { Text(text).font(EirsFont.sans(15, .semibold)).foregroundStyle(eirs.ink) }
}

/// Muted track with a brand fill that animates to `fraction`.
private struct HomeShareBar: View {
    @Environment(\.eirs) private var eirs
    var fraction: Double
    var height: CGFloat

    var body: some View {
        let clamped = CGFloat(min(max(fraction, 0), 1))
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(eirs.background)
                Capsule().fill(eirs.brand).frame(width: proxy.size.width * clamped)
            }
        }
        .frame(height: height)
        .animation(.easeOut(duration: 0.4), value: fraction)
    }
}

// MARK: - Total sales

struct HomeTotalSalesCard: View {
    @Environment(\.eirs) private var eirs
    var a: SalesAnalytics

    var body: some View {
        let soft = Color.white.opacity(0.85)
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Total sales").font(EirsFont.sans(13)).foregroundStyle(soft)
                Spacer(minLength: 8)
                Text(a.range.label).font(EirsFont.sans(13)).foregroundStyle(soft)
            }
            Text(Fmt.etb(a.totalSales))
                .font(EirsFont.serif(40)).foregroundStyle(Color.white)
                .lineLimit(1).minimumScaleFactor(0.6)
            HStack(spacing: 6) {
                Text("▲ \(a.delta)")
                    .font(EirsFont.sans(12, .semibold)).foregroundStyle(Color.white)
                    .padding(.horizontal, 7).padding(.vertical, 2)
                    .background(Color.white.opacity(0.18), in: Capsule())
                Text(a.comparison).font(EirsFont.sans(12)).foregroundStyle(Color.white)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.brand)
        .overlay(alignment: .topTrailing) {
            Circle().strokeBorder(Color.white.opacity(0.18), lineWidth: 1)
                .frame(width: 180, height: 180)
                .offset(x: 60, y: -60)
                .allowsHitTesting(false)
        }
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

// MARK: - Tiles

private struct HomeTile {
    let label: String
    let value: String
    let sub: String
    var red = false
}

struct HomeTiles: View {
    @Environment(\.eirs) private var eirs
    var a: SalesAnalytics

    private var tiles: [HomeTile] {
        let taxPct: Double = a.totalSales > 0 ? a.taxCollected / a.totalSales * 100 : 0
        let invoiceAvg: Double = a.totalSales * 0.62 / Double(max(a.invoicesIssued, 1))
        let receiptAvg: Double = a.totalSales * 0.38 / Double(max(a.receiptsIssued, 1))
        return [
            HomeTile(label: "Tax collected", value: Fmt.etb(a.taxCollected), sub: HomeFormat.oneDecimal(taxPct) + "% of gross"),
            HomeTile(label: "Canceled", value: "\(a.canceledCount)", sub: Fmt.etb(a.canceledValue) + " lost", red: true),
            HomeTile(label: "Invoices issued", value: HomeFormat.grouped(a.invoicesIssued), sub: "avg " + Fmt.etb(invoiceAvg)),
            HomeTile(label: "Receipts issued", value: HomeFormat.grouped(a.receiptsIssued), sub: "avg " + Fmt.etb(receiptAvg)),
        ]
    }

    var body: some View {
        let list = tiles
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                tileView(list[0])
                tileView(list[1])
            }
            HStack(spacing: 10) {
                tileView(list[2])
                tileView(list[3])
            }
        }
    }

    private func tileView(_ t: HomeTile) -> some View {
        EirsCard(padding: EdgeInsets(top: 14, leading: 14, bottom: 14, trailing: 14), spacing: 8) {
            Text(t.label).font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
            Text(t.value).font(EirsFont.serif(28)).foregroundStyle(t.red ? eirs.red : eirs.ink)
                .lineLimit(1).minimumScaleFactor(0.6)
            Text(t.sub).font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
        }
    }
}

// MARK: - Sales & tax

struct HomeSalesTaxCard: View {
    @Environment(\.eirs) private var eirs
    var a: SalesAnalytics
    @State private var highlighted: Int?

    private var point: SalesPoint? {
        guard let i = highlighted, i >= 0, i < a.points.count else { return nil }
        return a.points[i]
    }

    var body: some View {
        EirsCard(spacing: 12) {
            HStack {
                HomeCardTitle(text: "Sales & tax")
                Spacer(minLength: 8)
                Text(a.range.granularity + " · " + a.range.label.lowercased())
                    .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            readout
            SalesChart(points: a.points, highlighted: highlighted, onScrub: { highlighted = $0 })
            SalesChartTicks(points: a.points)
        }
        .onChange(of: a.range) { _, _ in highlighted = nil }
        .onChange(of: a.branch) { _, _ in highlighted = nil }
    }

    private var readout: some View {
        let salesValue = Fmt.etb(point?.sales ?? a.totalSales)
        let taxValue = Fmt.etb(point?.tax ?? a.taxCollected)
        return VStack(alignment: .leading, spacing: 4) {
            Text(point?.label ?? a.range.label)
                .font(EirsFont.mono(12)).foregroundStyle(eirs.muted).lineLimit(1)
            HStack(spacing: 14) {
                (Text("Sales ").font(EirsFont.sans(12)).foregroundColor(eirs.ink)
                    + Text(salesValue).font(EirsFont.mono(12, .medium)).foregroundColor(eirs.ink))
                    .lineLimit(1)
                (Text("Tax ").font(EirsFont.sans(12)).foregroundColor(eirs.muted)
                    + Text(taxValue).font(EirsFont.mono(12, .medium)).foregroundColor(eirs.ink))
                    .lineLimit(1)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

// MARK: - Tax by code

struct HomeTaxByCodeCard: View {
    @Environment(\.eirs) private var eirs
    var a: SalesAnalytics

    var body: some View {
        EirsCard(spacing: 14) {
            HomeCardTitle(text: "Tax by code")
            ForEach(a.taxByCode.indices, id: \.self) { index in
                codeRow(a.taxByCode[index])
            }
            withholdingRow
        }
    }

    private func codeRow(_ t: TaxCodeAmount) -> some View {
        VStack(spacing: 6) {
            HStack(spacing: 8) {
                HStack(spacing: 8) {
                    CodeTag(text: t.code.rawValue)
                    Text(t.label).font(EirsFont.sans(13, .medium)).foregroundStyle(eirs.ink).lineLimit(1)
                }
                Spacer(minLength: 8)
                MonoText(text: Fmt.etb(t.tax), lineLimit: 1)
            }
            HomeShareBar(fraction: t.barShare, height: 6)
        }
    }

    private var withholdingRow: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("Withholding").font(EirsFont.sans(13, .semibold)).foregroundStyle(eirs.ink)
                Text("By B2B / B2G buyers").font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
            }
            Spacer(minLength: 8)
            MonoText(text: Fmt.etb(a.withholding))
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

// MARK: - By transaction type

private struct HomeBarSegment: Identifiable {
    let id: Int
    let share: Double
    let color: Color
}

struct HomeTransactionTypeCard: View {
    @Environment(\.eirs) private var eirs
    var a: SalesAnalytics

    private func color(_ i: Int) -> Color {
        switch i {
        case 0: return eirs.brand
        case 1: return eirs.ink
        case 2: return Color(hex: 0x5FA8AE)
        case 3: return Color(hex: 0x9CC9CD)
        case 4: return Color(hex: 0xC9E5E7)
        default: return eirs.tint2
        }
    }

    private var segments: [HomeBarSegment] {
        var list: [HomeBarSegment] = []
        for (i, t) in a.byTransactionType.enumerated() where t.share > 0 {
            list.append(HomeBarSegment(id: i, share: t.share, color: color(i)))
        }
        return list
    }

    var body: some View {
        EirsCard(spacing: 12) {
            HomeCardTitle(text: "By transaction type")
            stackedBar
            VStack(spacing: 0) {
                ForEach(a.byTransactionType.indices, id: \.self) { index in
                    typeRow(index, a.byTransactionType[index])
                }
            }
        }
    }

    private var stackedBar: some View {
        let list = segments
        let total = list.reduce(0.0) { $0 + $1.share }
        return GeometryReader { proxy in
            let gaps = CGFloat(max(list.count - 1, 0)) * 2
            let usable = max(proxy.size.width - gaps, 0)
            HStack(spacing: 2) {
                ForEach(list) { seg in
                    Rectangle().fill(seg.color)
                        .frame(width: total > 0 ? usable * CGFloat(seg.share / total) : 0)
                }
            }
        }
        .frame(height: 10)
        .clipShape(RoundedRectangle(cornerRadius: 5, style: .continuous))
    }

    private func typeRow(_ index: Int, _ t: TransactionTypeShare) -> some View {
        let percent = Int((t.share * 100).rounded())
        return HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 3, style: .continuous).fill(color(index)).frame(width: 9, height: 9)
            MonoText(text: t.type.rawValue, size: 13, weight: .medium)
                .frame(width: 36, alignment: .leading)
            Text("\(percent)% · \(t.documents) docs")
                .font(EirsFont.sans(12)).foregroundStyle(eirs.muted).lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            MonoText(text: Fmt.etb(t.amount), lineLimit: 1)
        }
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity)
        .hairline(.top, color: eirs.line)
    }
}

// MARK: - By branch

struct HomeBranchCard: View {
    @Environment(\.eirs) private var eirs
    var a: SalesAnalytics
    var selected: Branch?
    var infos: [BranchInfo]
    var onToggle: (Branch) -> Void

    private var maxShare: Double {
        let m = a.byBranch.map { $0.share }.max() ?? 0
        return m > 0 ? m : 1
    }

    var body: some View {
        EirsCard(spacing: 10) {
            HStack {
                HomeCardTitle(text: "By branch")
                Spacer(minLength: 8)
                Text("Tap to filter").font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
            }
            ForEach(a.byBranch, id: \.branch) { b in
                branchRow(b)
            }
        }
    }

    private func branchRow(_ b: BranchSales) -> some View {
        let on = selected == b.branch
        let systems = infos.first(where: { $0.branch == b.branch })?.sourceSystems ?? b.sourceSystems
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        let dimmed = selected != nil && !on
        return Button { onToggle(b.branch) } label: {
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    Text(b.branch.displayName).font(EirsFont.sans(14, .semibold)).foregroundStyle(eirs.ink).lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    MonoText(text: Fmt.etb(b.amount), lineLimit: 1)
                }
                HomeShareBar(fraction: b.share / maxShare, height: 5)
                Text("\(b.documents) docs · \(Fmt.plural(systems, "source system"))")
                    .font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(12)
            .overlay(shape.strokeBorder(on ? eirs.brand : eirs.line, lineWidth: 1))
            .contentShape(Rectangle())
        }
        .buttonStyle(PlainTapStyle())
        .opacity(dimmed ? 0.45 : 1)
        .animation(.easeOut(duration: 0.2), value: dimmed)
    }
}

// MARK: - Recent activity

private struct HomeRowPressStyle: ButtonStyle {
    @Environment(\.eirs) private var eirs

    func makeBody(configuration: Configuration) -> some View {
        configuration.label.background(configuration.isPressed ? eirs.background : Color.white)
    }
}

struct HomeRecentActivityCard: View {
    @Environment(\.eirs) private var eirs
    var items: [ActivityItem]
    var onOpen: (ActivityItem) -> Void
    var onViewAll: () -> Void

    var body: some View {
        EirsCard(padding: EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0)) {
            HomeCardTitle(text: "Recent activity")
                .padding(.top, 16).padding(.horizontal, 16).padding(.bottom, 6)
            ForEach(items) { item in
                activityRow(item)
            }
            Button(action: onViewAll) {
                Text("View all invoices →").font(EirsFont.sans(14, .semibold)).foregroundStyle(eirs.brand)
                    .padding(.horizontal, 16).padding(.vertical, 14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PlainTapStyle())
            .hairline(.top, color: eirs.line)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func chipStyle(_ kind: ActivityKind) -> (bg: Color, fg: Color, tag: String) {
        switch kind {
        case .invoice: return (eirs.tint1, eirs.brandDark, "INV")
        case .receipt: return (eirs.neutralChip, eirs.ink, "RCP")
        case .canceled: return (eirs.redTint, eirs.red, "CXL")
        }
    }

    private func activityRow(_ item: ActivityItem) -> some View {
        let style = chipStyle(item.kind)
        let canceled = item.kind == .canceled
        let amountColor: Color = canceled ? eirs.muted : eirs.ink
        return Button { onOpen(item) } label: {
            HStack(spacing: 12) {
                Text(style.tag).font(EirsFont.mono(11, .medium)).foregroundStyle(style.fg)
                    .frame(width: 36, height: 36)
                    .background(style.bg, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.party).font(EirsFont.sans(14, .medium)).foregroundStyle(eirs.ink).lineLimit(1)
                    Text("\(item.number) · \(HomeFormat.whenLabel(item.at))")
                        .font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                MonoText(text: Fmt.etb(item.amount), color: amountColor, strike: canceled, lineLimit: 1)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(HomeRowPressStyle())
        .hairline(.top, color: eirs.line)
    }
}
