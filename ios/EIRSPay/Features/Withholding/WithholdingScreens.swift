import SwiftUI
import EIRSDomain

enum CertFilter: CaseIterable {
    case all, awaiting, received

    var label: String {
        switch self {
        case .all: return "All"
        case .awaiting: return "Awaiting certificate"
        case .received: return "Certificate received"
        }
    }
}

struct WithholdingStat: Identifiable {
    let label: String
    let value: String
    let warn: Bool
    var id: String { label }
}

@MainActor @Observable
final class WithholdingModel {
    private let container: AppContainer
    private let selectedId: String?

    var views: [WithholdingView] = []
    var branch: Branch?
    var filter: CertFilter = .all

    init(container: AppContainer, id: String? = nil) {
        self.container = container
        self.selectedId = id
        self.branch = container.preferences.selectedBranch
    }

    func observeRecords() async {
        for await value in container.observeWithholding() { views = value }
    }

    func observeBranch() async {
        for await value in container.preferences.observeSelectedBranch() { branch = value }
    }

    var eyebrow: String {
        "TAX WITHHELD · " + (branch?.displayName ?? "All branches").uppercased()
    }

    var records: [WithholdingView] {
        let scoped = views.filter { view in
            guard let selected = branch else { return true }
            return view.invoice.branch == selected
        }
        return scoped.filter { view in
            switch filter {
            case .all: return true
            case .received: return view.record.certificateReceived
            case .awaiting: return !view.record.certificateReceived
            }
        }
    }

    var stats: [WithholdingStat] {
        let rows = records
        let total: Decimal = rows.map { $0.amount }.sumMoney()
        let net: Decimal = rows.map { $0.netPaid }.sumMoney()
        let awaiting = rows.filter { !$0.record.certificateReceived }.count
        return [
            WithholdingStat(label: "Records", value: "\(rows.count)", warn: false),
            WithholdingStat(label: "Total withheld", value: Fmt.etb(total), warn: false),
            WithholdingStat(label: "Awaiting certificate", value: "\(awaiting)", warn: true),
            WithholdingStat(label: "Net paid to you", value: Fmt.etb(net), warn: false),
        ]
    }

    var detail: WithholdingView? {
        guard let id = selectedId else { return nil }
        return views.first { $0.record.id == id }
    }

    func markCertificateReceived(_ id: String) async {
        await container.markCertificateReceived(id: id)
    }
}

private struct CertTone {
    let label: String
    let bg: Color
    let fg: Color
}

private func certTone(_ received: Bool, _ c: EirsColors) -> CertTone {
    if received { return CertTone(label: "Certificate received", bg: c.paidTint, fg: c.paidInk) }
    return CertTone(label: "Awaiting certificate", bg: c.warnTint, fg: c.warnInk)
}

// MARK: - List

@MainActor
struct WithholdingScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: WithholdingModel
    private let onBack: () -> Void
    private let onOpenRecord: (String) -> Void
    private let onOpenInvoice: (String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onOpenRecord: @escaping (String) -> Void,
         onOpenInvoice: @escaping (String) -> Void) {
        _model = State(initialValue: WithholdingModel(container: container))
        self.onBack = onBack
        self.onOpenRecord = onOpenRecord
        self.onOpenInvoice = onOpenInvoice
    }

    var body: some View {
        let records = model.records
        ViewScaffold(
            padding: EdgeInsets(top: 16, leading: 16, bottom: 40, trailing: 16),
            header: {
                ViewHeader(eyebrow: model.eyebrow, onBack: onBack, title: { ViewTitle(plain: "Withholding") })
            },
            content: {
                Text("Buyers who withheld part of a payment and remitted it to MoR for you. Collect each certificate to claim it.")
                    .font(EirsFont.sans(13)).lineSpacing(4).foregroundStyle(eirs.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                ReceiptGrid2(items: model.stats) { stat in
                    statCard(stat)
                }
                ChipRail {
                    ForEach(CertFilter.allCases, id: \.self) { filter in
                        EirsChip(label: filter.label, selected: model.filter == filter) { model.filter = filter }
                    }
                }
                ForEach(records) { record in
                    recordCard(record)
                }
            })
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await model.observeRecords() }
        .task { await model.observeBranch() }
    }

    private func statCard(_ stat: WithholdingStat) -> some View {
        EirsCard(padding: EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14), spacing: 5) {
            Text(stat.label).font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
            Text(stat.value).font(EirsFont.serif(22)).foregroundStyle(stat.warn ? eirs.warnInk : eirs.ink)
                .lineLimit(1).truncationMode(.tail)
        }
    }

    private func recordCard(_ w: WithholdingView) -> some View {
        let st = certTone(w.record.certificateReceived, eirs)
        let invoiceNo = "INV-\(w.invoice.number)"
        let invoiceId = w.invoice.id
        return ListCard(action: { onOpenRecord(w.record.id) }) {
            HStack {
                MonoText(text: w.record.id, size: 13, weight: .medium)
                Spacer(minLength: 8)
                MonoText(text: Fmt.etb(w.amount), size: 14, weight: .medium)
            }
            HStack(spacing: 8) {
                Text(w.invoice.buyer.name).font(EirsFont.sans(13, .medium)).lineLimit(1).truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("\(ReceiptStyle.percentText(w.record.rate)) of \(Fmt.etb(w.base))")
                    .font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1)
            }
            HStack {
                Badge(text: st.label, background: st.bg, content: st.fg)
                Spacer(minLength: 8)
                ReceiptLinkChip(text: invoiceNo) { onOpenInvoice(invoiceId) }
            }
        }
    }
}

// MARK: - Detail

@MainActor
struct WithholdingDetailScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: WithholdingModel
    private let id: String
    private let onBack: () -> Void
    private let onOpenInvoice: (String) -> Void
    private let onOpenReceipt: (String) -> Void

    init(container: AppContainer, id: String, onBack: @escaping () -> Void,
         onOpenInvoice: @escaping (String) -> Void, onOpenReceipt: @escaping (String) -> Void) {
        _model = State(initialValue: WithholdingModel(container: container, id: id))
        self.id = id
        self.onBack = onBack
        self.onOpenInvoice = onOpenInvoice
        self.onOpenReceipt = onOpenReceipt
    }

    var body: some View {
        Group {
            if let w = model.detail {
                content(w)
            } else {
                ViewScaffold(
                    header: { ViewHeader(eyebrow: "WITHHOLDING", onBack: onBack, title: { ViewTitle(plain: id) }) },
                    content: { EmptyView() })
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task { await model.observeRecords() }
    }

    private func content(_ w: WithholdingView) -> some View {
        let inv = w.invoice
        let st = certTone(w.record.certificateReceived, eirs)
        let agent = inv.buyer
        return ViewScaffold(
            padding: EdgeInsets(top: 16, leading: 16, bottom: 40, trailing: 16),
            header: {
                ViewHeader(eyebrow: "WHT · \(inv.transactionType.rawValue) · \(st.label.uppercased())", onBack: onBack,
                           title: { ViewTitle(plain: w.record.id) })
            },
            content: {
                if !w.record.certificateReceived { certificateBanner(w, agentName: agent.name) }
                calculationCard(w)
                relatedInvoiceCard(w)
                paymentReceiptCard(w)
                agentCard(agent)
                recordCard(w, statusLabel: st.label)
            })
    }

    private func certificateBanner(_ w: WithholdingView, agentName: String) -> some View {
        let shape = RoundedRectangle(cornerRadius: 14, style: .continuous)
        return VStack(alignment: .leading, spacing: 10) {
            Text("No certificate yet. Ask \(agentName) for it so you can claim \(Fmt.etb(w.amount)) against your tax.")
                .font(EirsFont.sans(13)).lineSpacing(4).foregroundStyle(eirs.amberInk)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                Task { await model.markCertificateReceived(w.record.id) }
            } label: {
                Text("Mark certificate received")
                    .font(EirsFont.sans(14, .semibold))
                    .foregroundStyle(Color.white)
                    .frame(maxWidth: .infinity, minHeight: 42, maxHeight: 42)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PressFillStyle(normal: eirs.amberInk, pressed: eirs.amberInk.opacity(0.85), radius: 10))
        }
        .padding(14)
        .background(eirs.amberTint, in: shape)
        .overlay(shape.strokeBorder(eirs.amberBorder, lineWidth: 1))
    }

    private func calculationCard(_ w: WithholdingView) -> some View {
        EirsCard {
            Eyebrow(text: "CALCULATION", color: eirs.muted, size: 10, tracking: 0.12).padding(.bottom, 6)
            HStack {
                Text("Taxable base").font(EirsFont.sans(14)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                MonoText(text: Fmt.etb(w.base), size: 14)
            }
            .padding(.vertical, 8)
            HStack {
                Text("× Rate").font(EirsFont.sans(14)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                MonoText(text: ReceiptStyle.percentText(w.record.rate), size: 14)
            }
            .padding(.vertical, 8)
            .hairline(.bottom, color: eirs.line)
            HStack(alignment: .lastTextBaseline) {
                Text("Withheld").font(EirsFont.sans(14, .semibold))
                Spacer(minLength: 8)
                Text(Fmt.etb(w.amount)).font(EirsFont.serif(32)).foregroundStyle(eirs.brand)
            }
            .padding(.vertical, 12)
            totalsBox(w)
        }
    }

    private func totalsBox(_ w: WithholdingView) -> some View {
        VStack(spacing: 6) {
            HStack {
                Text("Invoice total").font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                MonoText(text: Fmt.etb(w.invoice.totals.grandTotal), size: 13)
            }
            HStack {
                Text("Net paid to you").font(EirsFont.sans(13, .semibold))
                Spacer(minLength: 8)
                MonoText(text: Fmt.etb(w.netPaid), size: 13, weight: .medium)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func relatedInvoiceCard(_ w: WithholdingView) -> some View {
        let inv = w.invoice
        return Button { onOpenInvoice(inv.id) } label: {
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 4) {
                    Eyebrow(text: "RELATED INVOICE", color: eirs.muted, size: 10, tracking: 0.12)
                    Text("INV-\(inv.number)").font(EirsFont.serif(24))
                    Text("\(Fmt.dateTime(inv.issuedAt)) · \(inv.transactionType.rawValue)")
                        .font(EirsFont.sans(12)).foregroundStyle(eirs.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                LineIcon(EirsIcons.chevronRight, size: 15, tint: eirs.muted)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(ListCardStyle())
    }

    @ViewBuilder
    private func paymentReceiptCard(_ w: WithholdingView) -> some View {
        if let pay = w.paymentReceipt {
            Button { onOpenReceipt(pay.receipt.id) } label: {
                HStack {
                    (Text("Paid on").foregroundColor(eirs.muted) + Text(" ")
                        + Text("RCP-\(pay.receipt.number)").font(EirsFont.mono(13, .medium)))
                        .font(EirsFont.sans(13))
                    Spacer(minLength: 8)
                    MonoText(text: Fmt.etb(pay.amount) + " ↗", size: 13)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(ListCardStyle())
        }
    }

    private func agentCard(_ agent: Party) -> some View {
        EirsCard(padding: EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16)) {
            Eyebrow(text: "WITHHOLDING AGENT", color: eirs.muted, size: 10, tracking: 0.12)
            Text(agent.name).font(EirsFont.serif(22)).padding(.top, 6).padding(.bottom, 8)
            KeyValueRow(key: "TIN", value: agent.tin ?? "—")
            KeyValueRow(key: "VAT number", value: agent.vatNumber ?? "—")
            KeyValueRow(key: "Address", value: agent.address ?? "—")
            KeyValueRow(key: "Email", value: agent.email ?? "—")
        }
    }

    private func recordCard(_ w: WithholdingView, statusLabel: String) -> some View {
        let irn: String = "ETH-" + String(w.invoice.irn.prefix(12)) + "…"
        let rows: [(String, String)] = [
            ("Certificate no.", w.record.id), ("Withheld on", Fmt.day(w.record.withheldOn)),
            ("Tax period", w.record.taxPeriod), ("Related IRN", irn), ("Status", statusLabel),
        ]
        return ReceiptRowsCard(title: "RECORD", rows: rows)
    }
}
