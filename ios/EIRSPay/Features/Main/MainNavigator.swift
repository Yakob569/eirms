import Foundation
import EIRSDomain

/// Navigation callbacks the signed-in shell raises. RootView maps them to router pushes.
struct MainNavigator {
    var openInvoice: (String) -> Void
    var openReceipt: (String) -> Void
    var registerInvoice: () -> Void
    var registerReceipt: () -> Void
    var openItems: () -> Void
    var openWithholding: () -> Void
    var startTerminal: (Branch) -> Void
    var signedOut: () -> Void
    var openSettings: (SettingsPage) -> Void
    /// Credit (.cre) or debit (.deb) note for an invoice.
    var makeAdjustment: (String, DocumentType) -> Void
    /// Interim (.int) or final (.fin) invoice against a retainer.
    var makeRetainerInvoice: (String, DocumentType) -> Void
}

/// Which list card's ⋮ menu is open.
enum RowActionTarget: Hashable {
    case invoice(String)
    case receipt(String)
}

enum MainTab: Int, CaseIterable, Identifiable {
    case home, invoices, receipts, more
    var id: Int { rawValue }
}

enum MainSheet: Hashable {
    case branch, user, terminal, new, rowActions, cancelInvoice
}
