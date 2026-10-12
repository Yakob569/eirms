import SwiftUI
import EIRSDomain

struct ReceiptRowUi: Identifiable {
    let id: String
    let number: String
    let type: ReceiptType
    let amount: String
    let refund: Bool
    let party: String
    let method: String
    let methodSub: String
    let linkedInvoiceId: String?
    let linkedNo: String?
    let whenText: String
    let branch: String
}

@MainActor @Observable
final class ReceiptsTabModel {
    private let container: AppContainer

    var views: [ReceiptView] = []
    var branch: Branch?
    var query: String = ""
    var type: ReceiptType?
    var method: PaymentMethod?

    init(container: AppContainer) {
        self.container = container
        self.branch = container.preferences.selectedBranch
    }

    func observeReceipts() async {
        for await value in container.observeReceiptViews() { views = value }
    }

    func observeBranch() async {
        for await value in container.preferences.observeSelectedBranch() { branch = value }
    }

    private var filtered: [ReceiptView] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        return views.filter { view in
            let r = view.receipt
            if let selectedBranch = branch, r.branch != selectedBranch { return false }
            if let selectedType = type, r.type != selectedType { return false }
            if let selectedMethod = method, r.method != selectedMethod { return false }
            if q.isEmpty { return true }
            let haystack = [ReceiptStyle.receiptNo(r), view.buyer.name, r.reference ?? "",
                            view.linkedInvoice.map { ReceiptStyle.invoiceNo($0) } ?? ""].joined(separator: " ").lowercased()
            return haystack.contains(q)
        }
    }

    var rows: [ReceiptRowUi] { filtered.map { toRow($0) } }

    var summary: String {
        let list = filtered
        let collected: Decimal = list.filter { $0.receipt.type != .refund }.map { $0.amount }.sumMoney()
        return Fmt.plural(list.count, "receipt") + " · " + Fmt.etb(collected) + " collected"
    }

    private func toRow(_ v: ReceiptView) -> ReceiptRowUi {
        let r = v.receipt
        let refund = r.type == .refund
        let sub: String
        switch r.method {
        case .mobileMoney: sub = "\(r.provider ?? "") · \(r.reference ?? "")"
        case .cash: sub = "Tendered " + Fmt.f2(r.tendered ?? v.amount)
        default: sub = r.reference ?? "—"
        }
        return ReceiptRowUi(
            id: r.id, number: ReceiptStyle.receiptNo(r), type: r.type,
            amount: (refund ? "−" : "") + Fmt.etb(v.amount), refund: refund,
            party: v.buyer.name, method: r.method.displayName, methodSub: sub,
            linkedInvoiceId: v.linkedInvoice?.id, linkedNo: v.linkedInvoice.map { ReceiptStyle.invoiceNo($0) },
            whenText: Fmt.dateTime(r.issuedAt), branch: r.branch.displayName)
    }
}

/// Receipts tab body, embedded in the shell's scrolling column (the shell draws the page title).
@MainActor
struct ReceiptsTabContent: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ReceiptsTabModel
    var onOpenReceipt: (String) -> Void
    var onOpenInvoice: (String) -> Void
    var onNewReceipt: () -> Void
    /// Opens the ⋮ bottom sheet (owned by the shell) for a receipt.
    var onRowActions: (String) -> Void

    init(container: AppContainer, onOpenReceipt: @escaping (String) -> Void,
         onOpenInvoice: @escaping (String) -> Void, onNewReceipt: @escaping () -> Void,
         onRowActions: @escaping (String) -> Void) {
        _model = State(initialValue: ReceiptsTabModel(container: container))
        self.onOpenReceipt = onOpenReceipt
        self.onOpenInvoice = onOpenInvoice
        self.onNewReceipt = onNewReceipt
        self.onRowActions = onRowActions
    }

    private var typeOptions: [ReceiptType?] {
        var options: [ReceiptType?] = [nil]
        for t in ReceiptType.allCases { options.append(t) }
        return options
    }

    private var methodOptions: [PaymentMethod?] {
        var options: [PaymentMethod?] = [nil]
        for m in PaymentMethod.allCases { options.append(m) }
        return options
    }

    var body: some View {
        let rows = model.rows
        VStack(spacing: 12) {
            SearchField(text: $model.query, placeholder: "Receipt no., customer, reference")
            typeRail
            methodRail
            HStack {
                Text(model.summary).font(EirsFont.sans(13)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                TextLink("+ New receipt", size: 13, action: onNewReceipt)
            }
            ForEach(rows) { row in
                ReceiptsTabCard(row: row, onOpen: { onOpenReceipt(row.id) }, onOpenInvoice: onOpenInvoice,
                                onMenu: { onRowActions(row.id) })
            }
            if rows.isEmpty { EmptyState(text: "No receipts match these filters.") }
        }
        .riseIn(duration: 0.3)
        .task { await model.observeReceipts() }
        .task { await model.observeBranch() }
    }

    private var typeRail: some View {
        ChipRail {
            ForEach(typeOptions.indices, id: \.self) { index in
                let option: ReceiptType? = typeOptions[index]
                EirsChip(label: option?.displayName ?? "All", selected: model.type == option, style: .solid) {
                    model.type = option
                }
            }
        }
    }

    private var methodRail: some View {
        ChipRail {
            ForEach(methodOptions.indices, id: \.self) { index in
                let option: PaymentMethod? = methodOptions[index]
                EirsChip(label: option?.displayName ?? "Any method", selected: model.method == option, style: .tint) {
                    model.method = option
                }
            }
        }
    }
}

private struct ReceiptsTabCard: View {
    @Environment(\.eirs) private var eirs
    var row: ReceiptRowUi
    var onOpen: () -> Void
    var onOpenInvoice: (String) -> Void
    var onMenu: () -> Void

    var body: some View {
        let tone = ReceiptStyle.tone(for: row.type, eirs)
        ListCard(action: onOpen) {
            HStack(spacing: 8) {
                HStack(spacing: 7) {
                    Text(row.number).font(EirsFont.sans(16, .semibold)).lineLimit(1)
                    Badge(text: row.type.displayName, background: tone.bg, content: tone.fg)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 2) {
                    MonoText(text: row.amount, size: 13, color: row.refund ? eirs.red : eirs.ink, weight: .medium, lineLimit: 1)
                    RowMenuButton(onTap: onMenu)
                }
            }
            HStack(spacing: 8) {
                Text(row.party).font(EirsFont.sans(13, .medium)).lineLimit(1).truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(row.method).font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1)
            }
            subRow
            HStack {
                Text(row.whenText).font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
                Spacer(minLength: 8)
                Text(row.branch).font(EirsFont.sans(11)).foregroundStyle(eirs.muted)
            }
        }
    }

    private var subRow: some View {
        HStack(spacing: 8) {
            MonoText(text: row.methodSub, size: 11, color: eirs.muted, lineLimit: 1)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let invoiceId = row.linkedInvoiceId, let number = row.linkedNo {
                ReceiptLinkChip(text: number) { onOpenInvoice(invoiceId) }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}
