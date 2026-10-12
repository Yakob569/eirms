import SwiftUI
import EIRSDomain

/// Data behind the list cards' ⋮ bottom sheet: the documents (to work out which options apply) and the
/// cancel flow that the sheet hands off to.
@MainActor @Observable
final class RowActionsModel {
    var invoices: [Invoice] = []
    var receipts: [ReceiptView] = []
    var target: RowActionTarget?
    var cancelInvoiceId: String?
    var cancelReason: CancelReason = .customerRequest
    var loading: LoaderMessage?

    @ObservationIgnored private let container: AppContainer

    init(container: AppContainer) { self.container = container }

    func observeInvoices() async {
        for await value in container.observeInvoices() { invoices = value }
    }

    func observeReceipts() async {
        for await value in container.observeReceiptViews() { receipts = value }
    }

    func invoice(_ id: String) -> Invoice? { invoices.first { $0.id == id } }
    func receipt(_ id: String) -> ReceiptView? { receipts.first { $0.id == id } }

    func invoiceActions(_ invoice: Invoice) -> [InvoiceRowAction] { RowActions.forInvoice(invoice, among: invoices) }

    func confirmCancel() async {
        guard let id = cancelInvoiceId, let inv = invoice(id), loading == nil else { return }
        let reason = cancelReason
        loading = LoaderMessage(title: "Canceling with MoR", subtitle: "Recording against IRN ETH-" + String(inv.irn.prefix(8)))
        _ = try? await container.cancelInvoice(invoiceId: id, reason: reason)
        loading = nil
        cancelInvoiceId = nil
    }
}

/// One option in the ⋮ sheet.
struct RowActionItem: Identifiable {
    let id: String
    let label: String
    let icon: LineIconSpec
    var danger = false
    var separatorBefore = false
    let action: () -> Void
}

/// Bottom-sheet body: eyebrow, serif document number, buyer · amount, then the options.
struct RowActionSheet: View {
    @Environment(\.eirs) private var eirs
    var eyebrow: String
    var title: String
    var subtitle: String
    var items: [RowActionItem]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 3) {
                Eyebrow(text: eyebrow, size: 10, tracking: 0.12)
                Text(title).font(EirsFont.serif(28))
                Text(subtitle).font(EirsFont.sans(13)).foregroundStyle(eirs.muted).lineLimit(1)
            }
            .padding(.horizontal, 6)
            .padding(.bottom, 10)
            ForEach(items) { item in
                if item.separatorBefore {
                    Hairline().padding(.horizontal, 6).padding(.vertical, 4)
                }
                Button(action: item.action) {
                    HStack(spacing: 12) {
                        LineIcon(item.icon, size: 18, strokeWidth: 1.9, tint: item.danger ? eirs.red : eirs.ink)
                        Text(item.label).font(EirsFont.sans(15, .medium)).foregroundStyle(item.danger ? eirs.red : eirs.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        LineIcon(EirsIcons.chevronRight, size: 15, tint: eirs.muted)
                    }
                    .padding(.horizontal, 10)
                    .frame(maxWidth: .infinity, minHeight: 52, maxHeight: 52)
                    .contentShape(Rectangle())
                }
                .buttonStyle(PressFillStyle(normal: .clear, pressed: eirs.background, radius: 12))
            }
        }
    }
}

/// The ⋮ button on list cards. A tap gesture (not a Button) so it can sit inside the card's own button.
struct RowMenuButton: View {
    @Environment(\.eirs) private var eirs
    var onTap: () -> Void

    var body: some View {
        VStack(spacing: 3) {
            ForEach(0..<3, id: \.self) { _ in Circle().fill(eirs.muted).frame(width: 3.6, height: 3.6) }
        }
        .frame(width: 36, height: 36)
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
        .padding(.vertical, -8)
        .padding(.trailing, -10)
        .accessibilityLabel("More actions")
        .accessibilityAddTraits(.isButton)
    }
}
