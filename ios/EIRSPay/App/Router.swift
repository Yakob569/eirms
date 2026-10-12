import SwiftUI
import EIRSDomain

/// What fills the navigation stack's root. Changing it also clears the stack, which is how sign-in,
/// sign-out and "go to dashboard" cut the back history (like the Android graph's popUpTo).
enum RootRoute: Hashable {
    case splash, onboarding, signIn, main
}

/// Screens pushed on top of the root. Arguments are plain values; screens take them as parameters.
enum Route: Hashable {
    case signIn
    case signUp
    case verifyMethod
    case verifyCode(channel: String, sentTo: String)      // channel = VerificationChannel.rawValue
    case verifySuccess(sentTo: String)
    case forgotPassword
    case resetLinkSent(email: String)
    case resetPassword(token: String)
    case resetDone
    case changePassword
    case changeDone

    case invoiceDetail(id: String)
    case invoicePrint(id: String)
    case registerInvoice
    case receiptDetail(id: String)
    case registerReceipt
    case items
    case itemForm(code: String?)
    case withholding
    case withholdingDetail(id: String)

    /// Credit / debit note for an invoice (kind = .cre or .deb).
    case adjustmentNote(invoiceId: String, kind: DocumentType)
    /// Interim / final invoice against a retainer (kind = .int or .fin).
    case retainerInvoice(retainerId: String, kind: DocumentType)

    case settings(SettingsPage)
    case branchDetail(id: String)
    case sourceSystemDetail(id: String)
    /// nil = the "New key" form.
    case apiKeyDetail(id: String?)
}

/// Full-screen pages opened from the More tab's settings list (and the setup stepper).
enum SettingsPage: String, Hashable {
    case account, morCredentials, sellerProfile, branches, sourceSystems, apiKeys, verifySetup
}

@Observable
@MainActor
final class Router {
    var root: RootRoute = .splash
    var path: [Route] = []
    /// Terminal mode is presented full screen and can't be swiped away: only hold-to-exit leaves it.
    var terminalBranch: Branch?

    func push(_ route: Route) { path.append(route) }
    func pop() { if !path.isEmpty { path.removeLast() } }

    /// Replace everything with a new root and an empty back stack.
    func reset(to root: RootRoute) {
        path = []
        self.root = root
    }

    /// Swaps the top screen for `route`; used after a registration form finishes so Back goes to the
    /// list the form was opened from, not back into the form.
    func replaceTop(with route: Route) {
        if !path.isEmpty { path.removeLast() }
        path.append(route)
    }
}
