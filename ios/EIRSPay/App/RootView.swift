import SwiftUI
import EIRSDomain

/// The app's navigation graph. Feature screens only know callbacks; this is the one place that maps
/// them to destinations.
@MainActor
struct RootView: View {
    let container: AppContainer
    @State private var router = Router()

    var body: some View {
        NavigationStack(path: $router.path) {
            rootContent
                .navigationDestination(for: Route.self) { destination($0) }
        }
        .fullScreenCover(item: $router.terminalBranch) { branch in
            TerminalScreen(container: container, branch: branch, onExit: { router.terminalBranch = nil })
                .eirsTheme(.teal)
        }
        // Password reset links from the "Check your inbox" email: eirspay://reset?token=...
        .onOpenURL { url in
            guard url.scheme == "eirspay", url.host == "reset",
                  let token = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "token" })?.value
            else { return }
            router.push(.resetPassword(token: token))
        }
    }

    // MARK: Root

    @ViewBuilder private var rootContent: some View {
        switch router.root {
        case .splash:
            SplashScreen(onFinished: {
                Task {
                    if await container.auth.currentSession() != nil { router.reset(to: .main) }
                    else if container.preferences.onboardingCompleted { router.reset(to: .signIn) }
                    else { router.reset(to: .onboarding) }
                }
            })
        case .onboarding:
            OnboardingScreen(container: container, onCreateAccount: { router.push(.signUp) }, onSignIn: { router.push(.signIn) })
        case .signIn:
            signInScreen
        case .main:
            MainScreen(
                container: container,
                navigator: mainNavigator,
                invoicesTab: { request in InvoicesTabContent(container: container, showCanceledRequest: request, onOpenInvoice: { router.push(.invoiceDetail(id: $0)) }) },
                receiptsTab: {
                    ReceiptsTabContent(container: container,
                                       onOpenReceipt: { router.push(.receiptDetail(id: $0)) },
                                       onOpenInvoice: { router.push(.invoiceDetail(id: $0)) },
                                       onNewReceipt: { router.push(.registerReceipt) })
                })
        }
    }

    private var signInScreen: some View {
        SignInScreen(
            container: container,
            onBack: { router.pop() },
            onForgotPassword: { router.push(.forgotPassword) },
            onCreateAccount: { router.push(.signUp) },
            onSignedIn: { mustChangePassword in
                if mustChangePassword { router.push(.changePassword) } else { router.reset(to: .main) }
            })
    }

    private var mainNavigator: MainNavigator {
        MainNavigator(
            openInvoice: { router.push(.invoiceDetail(id: $0)) },
            openReceipt: { router.push(.receiptDetail(id: $0)) },
            registerInvoice: { router.push(.registerInvoice) },
            registerReceipt: { router.push(.registerReceipt) },
            openItems: { router.push(.items) },
            openWithholding: { router.push(.withholding) },
            startTerminal: { router.terminalBranch = $0 },
            signedOut: { router.reset(to: .signIn) })
    }

    // MARK: Pushed destinations

    @ViewBuilder private func destination(_ route: Route) -> some View {
        let back: () -> Void = { router.pop() }
        switch route {
        case .signIn:
            signInScreen

        case .signUp:
            SignUpScreen(container: container, onBack: back, onSignIn: { router.push(.signIn) }, onAccountCreated: { router.push(.verifyMethod) })
        case .verifyMethod:
            VerifyMethodScreen(container: container, onBack: back,
                               onCodeSent: { channel, sentTo in router.push(.verifyCode(channel: channel, sentTo: sentTo)) })
        case let .verifyCode(channel, sentTo):
            VerifyCodeScreen(container: container, channel: channel, sentTo: sentTo, onBack: back, onUseDifferentMethod: back,
                             onVerified: { router.push(.verifySuccess(sentTo: sentTo)) })
        case let .verifySuccess(sentTo):
            VerifySuccessScreen(container: container, sentTo: sentTo, onBack: back, onContinue: { router.reset(to: .main) })
        case .forgotPassword:
            ForgotPasswordScreen(container: container, onBack: back, onSignIn: back, onLinkSent: { router.push(.resetLinkSent(email: $0)) })
        case let .resetLinkSent(email):
            ResetLinkSentScreen(container: container, email: email, onBack: back, onUseAnotherEmail: back,
                                onOpenResetLink: debugShortcuts ? { router.push(.resetPassword(token: "debug-reset-token")) } : nil)
        case let .resetPassword(token):
            ResetPasswordScreen(container: container, token: token, onBack: back, onPasswordReset: { router.push(.resetDone) })
        case .resetDone:
            ResetDoneScreen(onBack: { router.reset(to: .signIn) }, onContinueToSignIn: { router.reset(to: .signIn) })
        case .changePassword:
            ChangePasswordScreen(container: container, onBack: back, onSignOut: { router.reset(to: .signIn) }, onPasswordChanged: { router.push(.changeDone) })
        case .changeDone:
            ChangeDoneScreen(container: container, onBack: back, onContinue: { router.reset(to: .main) })

        case let .invoiceDetail(id):
            InvoiceDetailScreen(container: container, id: id, onBack: back, onViewReceipt: { router.push(.invoicePrint(id: $0)) })
        case let .invoicePrint(id):
            InvoicePrintScreen(container: container, id: id, onBack: back)
        case .registerInvoice:
            RegisterInvoiceScreen(container: container, onBack: back, onRegistered: { router.replaceTop(with: .invoiceDetail(id: $0)) })
        case let .receiptDetail(id):
            ReceiptDetailScreen(container: container, id: id, onBack: back,
                                onOpenInvoice: { router.push(.invoiceDetail(id: $0)) },
                                onOpenWithholding: { router.push(.withholdingDetail(id: $0)) })
        case .registerReceipt:
            RegisterReceiptScreen(container: container, onBack: back, onRegistered: { router.replaceTop(with: .receiptDetail(id: $0)) })
        case .items:
            ItemsScreen(container: container, onBack: back, onNewItem: { router.push(.itemForm(code: nil)) },
                        onEditItem: { router.push(.itemForm(code: $0)) })
        case let .itemForm(code):
            ItemFormScreen(container: container, code: code, onBack: back)
        case .withholding:
            WithholdingScreen(container: container, onBack: back, onOpenRecord: { router.push(.withholdingDetail(id: $0)) },
                              onOpenInvoice: { router.push(.invoiceDetail(id: $0)) })
        case let .withholdingDetail(id):
            WithholdingDetailScreen(container: container, id: id, onBack: back,
                                    onOpenInvoice: { router.push(.invoiceDetail(id: $0)) },
                                    onOpenReceipt: { router.push(.receiptDetail(id: $0)) })
        }
    }

    /// Prototype-only shortcuts (e.g. "Open reset link") are available in debug builds only.
    private var debugShortcuts: Bool {
        #if DEBUG
        true
        #else
        false
        #endif
    }
}
