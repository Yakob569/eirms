import SwiftUI
import EIRSDomain

struct InvoiceFilters: Equatable {
    var query = ""
    var canceledMode = false
    /// nil = all statuses (within the active or canceled pool).
    var status: InvoiceStatus?
    var transactionType: TransactionType?
}

@MainActor @Observable final class InvoicesModel {
    var filters = InvoiceFilters()
    var branch: Branch?
    var all: [Invoice] = []
    var loaded = false
    let copier = InvCopyFeedback()

    @ObservationIgnored private let observeInvoices: ObserveInvoicesUseCase
    @ObservationIgnored private let preferences: PreferencesRepository
    @ObservationIgnored private var handledCanceledRequest = 0

    init(container: AppContainer) {
        observeInvoices = container.observeInvoices
        preferences = container.preferences
        branch = container.preferences.selectedBranch
    }

    var rows: [Invoice] {
        let q = filters.query.trimmingCharacters(in: .whitespaces).lowercased()
        return all.filter { matches($0, q) }
    }

    private func matches(_ inv: Invoice, _ q: String) -> Bool {
        if let selected = branch, inv.branch != selected { return false }
        if filters.canceledMode != inv.isCanceled { return false }
        if let status = filters.status, inv.status != status { return false }
        if let type = filters.transactionType, inv.transactionType != type { return false }
        if q.isEmpty { return true }
        let parts: [String] = [inv.number, inv.buyer.name, inv.buyer.tin ?? "", inv.buyer.idNumber ?? "", inv.irn]
        return parts.joined(separator: " ").lowercased().contains(q)
    }

    func observe() async {
        for await value in observeInvoices() {
            all = value
            loaded = true
        }
    }

    func observeBranch() async {
        for await value in preferences.observeSelectedBranch() { branch = value }
    }

    /// Status chip: "Canceled" switches to the canceled pool, the others filter the active pool.
    func pickStatus(_ status: InvoiceStatus?) {
        if status == .canceled {
            filters.canceledMode = true
            filters.status = nil
        } else {
            filters.canceledMode = false
            filters.status = status
        }
    }

    func pickTransactionType(_ type: TransactionType?) { filters.transactionType = type }

    /// The shell bumps a counter to ask for the canceled list; each request is applied once.
    func onCanceledRequest(_ request: Int) {
        if request > 0 && request != handledCanceledRequest {
            handledCanceledRequest = request
            filters.canceledMode = true
            filters.status = nil
        }
    }
}

private struct InvStatusChip: Identifiable {
    let id: String
    let status: InvoiceStatus?
    let label: String
}

private struct InvTypeChip: Identifiable {
    let id: String
    let type: TransactionType?
    let label: String
}

/// Invoices tab body, embedded in the shell's scrolling column (the shell shows the page title).
@MainActor
struct InvoicesTabContent: View {
    @Environment(\.eirs) private var eirs
    @State private var model: InvoicesModel
    var showCanceledRequest: Int
    var onOpenInvoice: (String) -> Void
    /// Opens the ⋮ bottom sheet (owned by the shell) for an invoice.
    var onRowActions: (String) -> Void

    private static let statusChips: [InvStatusChip] = [
        InvStatusChip(id: "all", status: nil, label: "All"),
        InvStatusChip(id: "issued", status: .issued, label: "Issued"),
        InvStatusChip(id: "paid", status: .paid, label: "Paid"),
        InvStatusChip(id: "withheld", status: .withheld, label: "Withheld"),
        InvStatusChip(id: "canceled", status: .canceled, label: "Canceled")
    ]

    private static let typeChips: [InvTypeChip] = [
        InvTypeChip(id: "any", type: nil, label: "Any type"),
        InvTypeChip(id: "b2b", type: .b2b, label: "B2B"),
        InvTypeChip(id: "b2c", type: .b2c, label: "B2C"),
        InvTypeChip(id: "b2g", type: .b2g, label: "B2G"),
        InvTypeChip(id: "g2b", type: .g2b, label: "G2B"),
        InvTypeChip(id: "g2c", type: .g2c, label: "G2C")
    ]

    init(container: AppContainer, showCanceledRequest: Int, onOpenInvoice: @escaping (String) -> Void,
         onRowActions: @escaping (String) -> Void) {
        _model = State(initialValue: InvoicesModel(container: container))
        self.showCanceledRequest = showCanceledRequest
        self.onOpenInvoice = onOpenInvoice
        self.onRowActions = onRowActions
    }

    var body: some View {
        let rows: [Invoice] = model.rows
        VStack(spacing: 12) {
            SearchField(text: $model.filters.query, placeholder: "Number, IRN, customer, TIN")
            statusRail
            typeRail
            summaryRow(rows)
            ForEach(rows) { inv in
                InvoiceListCard(
                    inv: inv,
                    canceledMode: model.filters.canceledMode,
                    copied: model.copier.copiedKey == inv.id,
                    onCopy: { model.copier.copy(key: inv.id, text: inv.irn) },
                    onOpen: { onOpenInvoice(inv.id) },
                    onMenu: { onRowActions(inv.id) })
            }
            if model.loaded && rows.isEmpty {
                EmptyState(text: "No documents match these filters.")
            }
        }
        .frame(maxWidth: .infinity)
        .riseIn(duration: 0.3)
        .onChange(of: showCanceledRequest, initial: true) { _, newValue in
            model.onCanceledRequest(newValue)
        }
        .task { await model.observe() }
        .task { await model.observeBranch() }
    }

    private var statusRail: some View {
        let current: InvoiceStatus? = model.filters.canceledMode ? InvoiceStatus.canceled : model.filters.status
        return ChipRail {
            ForEach(Self.statusChips) { chip in
                EirsChip(label: chip.label, selected: current == chip.status) { model.pickStatus(chip.status) }
            }
        }
    }

    private var typeRail: some View {
        ChipRail {
            ForEach(Self.typeChips) { chip in
                EirsChip(label: chip.label, selected: model.filters.transactionType == chip.type, style: .tint,
                         height: 30, radius: 8, mono: true, fontSize: 12, horizontalPadding: 11) {
                    model.pickTransactionType(chip.type)
                }
            }
        }
    }

    private func summaryRow(_ rows: [Invoice]) -> some View {
        let sum: Decimal = rows.map { $0.totals.grandTotal }.sumMoney()
        let count = Fmt.plural(rows.count, "document") + " · " + (model.branch?.displayName ?? "All branches")
        let amount = Text(Fmt.etb(sum)).font(EirsFont.mono(13, .medium)).foregroundColor(eirs.ink)
        let suffix = Text(model.filters.canceledMode ? " voided" : " gross").font(EirsFont.sans(13)).foregroundColor(eirs.muted)
        return HStack(alignment: .bottom) {
            Text(count)
                .font(EirsFont.sans(13))
                .foregroundStyle(eirs.muted)
                .lineLimit(1)
            Spacer(minLength: 8)
            (amount + suffix).lineLimit(1)
        }
    }
}

private struct InvoiceListCard: View {
    @Environment(\.eirs) private var eirs
    let inv: Invoice
    let canceledMode: Bool
    let copied: Bool
    let onCopy: () -> Void
    let onOpen: () -> Void
    let onMenu: () -> Void

    var body: some View {
        ListCard(action: onOpen) {
            titleRow
            buyerRow
            middleBlock
            footerRow
        }
    }

    private var titleRow: some View {
        let colors = invStatusColors(inv.status, eirs)
        return HStack(spacing: 8) {
            HStack(spacing: 7) {
                Text(inv.number).font(EirsFont.sans(16, .semibold)).lineLimit(1)
                Text(inv.documentType.rawValue)
                    .font(EirsFont.mono(10))
                    .foregroundStyle(eirs.muted)
                    .lineLimit(1)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 2)
                    .solidBorder(eirs.line, radius: 5)
                Badge(text: inv.status.rawValue, background: colors.bg, content: colors.fg)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 2) {
                MonoText(text: Fmt.etb(inv.totals.grandTotal), size: 13, color: canceledMode ? eirs.muted : eirs.ink,
                         weight: .medium, strike: canceledMode, lineLimit: 1)
                RowMenuButton(onTap: onMenu)
            }
        }
    }

    private var buyerRow: some View {
        let type = Text(inv.transactionType.rawValue).font(EirsFont.mono(13, .medium)).foregroundColor(eirs.ink)
        let rest = Text(" · " + invPartyIdLabel(inv.buyer)).font(EirsFont.sans(13)).foregroundColor(eirs.muted)
        return HStack(spacing: 8) {
            Text(inv.buyer.name)
                .font(EirsFont.sans(13, .medium))
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            (type + rest).lineLimit(1)
        }
    }

    @ViewBuilder private var middleBlock: some View {
        if !canceledMode {
            HStack(spacing: 8) {
                MonoText(text: Fmt.irnShort(inv.irn), size: 11, lineLimit: 1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                InvCopyChip(copied: copied, onCopy: onCopy)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(eirs.background, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        } else if let cancellation = inv.cancellation {
            VStack(alignment: .leading, spacing: 2) {
                Text(cancellation.reason.label).font(EirsFont.sans(13, .semibold)).foregroundStyle(eirs.red)
                MonoText(text: "Code \(cancellation.reason.code) · was \(cancellation.originalStatus.rawValue)", size: 10, color: eirs.redInk)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(eirs.redTint, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }

    @ViewBuilder private var footerRow: some View {
        HStack {
            if canceledMode, let cancellation = inv.cancellation {
                footerText(Fmt.dateTime(cancellation.canceledAt))
                Spacer(minLength: 8)
                footerText("Issued " + Fmt.day(inv.issuedAt))
            } else {
                footerText(Fmt.dateTime(inv.issuedAt))
                Spacer(minLength: 8)
                footerText(inv.branch.displayName)
            }
        }
    }

    private func footerText(_ text: String) -> some View {
        Text(text).font(EirsFont.sans(11)).foregroundStyle(eirs.muted).lineLimit(1)
    }
}
