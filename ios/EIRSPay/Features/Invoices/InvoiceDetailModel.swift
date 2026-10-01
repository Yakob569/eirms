import SwiftUI
import EIRSDomain

/// Backs both the invoice detail and its printable receipt.
@MainActor @Observable final class InvoiceDetailModel {
    var invoice: Invoice?
    /// For credit/debit notes: the invoice being amended, when it is on record.
    var amended: Invoice?
    var cancelSheetOpen = false
    var cancelReason: CancelReason = .customerRequest
    var loading: LoaderMessage?
    let copier = InvCopyFeedback()

    let id: String
    @ObservationIgnored private let observeInvoices: ObserveInvoicesUseCase
    @ObservationIgnored private let cancelUseCase: CancelInvoiceUseCase

    init(container: AppContainer, id: String) {
        self.id = id
        observeInvoices = container.observeInvoices
        cancelUseCase = container.cancelInvoice
    }

    func observe() async {
        for await all in observeInvoices() {
            let found: Invoice? = all.first { $0.id == id }
            var previous: Invoice?
            if let amendment = found?.amendment {
                previous = all.first { $0.id == amendment.previousInvoiceId }
            }
            invoice = found
            amended = previous
        }
    }

    func openCancel() { cancelSheetOpen = true }
    func closeCancel() { cancelSheetOpen = false }

    func confirmCancel() async {
        guard let inv = invoice, loading == nil else { return }
        let reason = cancelReason
        cancelSheetOpen = false
        loading = LoaderMessage(title: "Canceling with MoR", subtitle: "Recording against IRN ETH-" + String(inv.irn.prefix(8)))
        do {
            let updated = try await cancelUseCase(invoiceId: inv.id, reason: reason)
            invoice = updated
        } catch {
            // The invoice stays as it was; the loader just closes.
        }
        loading = nil
    }
}
