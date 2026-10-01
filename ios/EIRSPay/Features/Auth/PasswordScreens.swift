import SwiftUI
import EIRSDomain

// MARK: - Forgot password

@MainActor
struct ForgotPasswordScreen: View {
    @State private var model: ForgotPasswordModel
    let onBack: () -> Void
    let onSignIn: () -> Void
    let onLinkSent: (String) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onSignIn: @escaping () -> Void,
         onLinkSent: @escaping (String) -> Void) {
        _model = State(initialValue: ForgotPasswordModel(container: container))
        self.onBack = onBack
        self.onSignIn = onSignIn
        self.onLinkSent = onLinkSent
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, spacing: 22) {
                AuthHeading(plain: "Forgot your ", accent: "password?",
                            lead: "Enter the email on your merchant account. We'll send a link to set a new password.",
                            size: 38, leadSpacing: 5)
                LabeledField("Email address") {
                    EirsTextField("you@business.et", text: $model.email, keyboard: .emailAddress, submitLabel: .done)
                }
                Spacer(minLength: 0)
                if let message = model.errorMessage { AuthErrorText(text: message) }
                PrimaryButton("Send reset link", enabled: model.isValid, action: submit)
                PromptLink(prompt: "Remembered it?", action: "Sign in", onTap: onSignIn)
            }
        }
        .onChange(of: model.email) { model.clearError() }
        .authPushedChrome()
    }

    private func submit() {
        Task {
            if let sent = await model.submit() { onLinkSent(sent) }
        }
    }
}

// MARK: - Reset link sent

@MainActor
struct ResetLinkSentScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ResetLinkSentModel
    let email: String
    let onBack: () -> Void
    let onUseAnotherEmail: () -> Void
    let onOpenResetLink: (() -> Void)?

    init(container: AppContainer, email: String, onBack: @escaping () -> Void, onUseAnotherEmail: @escaping () -> Void,
         onOpenResetLink: (() -> Void)?) {
        _model = State(initialValue: ResetLinkSentModel(container: container))
        self.email = email
        self.onBack = onBack
        self.onUseAnotherEmail = onUseAnotherEmail
        self.onOpenResetLink = onOpenResetLink
    }

    var body: some View {
        AuthScaffold(onBack: onBack, spacing: 0) {
            AuthSuccessLayout(
                icon: EirsIcons.mail, plain: "Check your ", accent: "inbox.",
                message: { messageText },
                card: { ResetStepsCard() },
                actions: { actions })
        }
        .authPushedChrome()
    }

    private var messageText: Text {
        let lead = Text("If an account exists for ")
        let address = Text(Masking.email(email)).font(EirsFont.mono(15)).foregroundColor(eirs.ink)
        let tail = Text(", a reset link is on its way. It expires in 30 minutes.")
        return lead + address + tail
    }

    private var actions: some View {
        VStack(spacing: 14) {
            if let open = onOpenResetLink {
                PrimaryButton("Open reset link (prototype)", action: open)
            }
            HStack {
                TextLink(model.resent ? "Sent again" : "Resend link", action: { model.resend(email: email) })
                Spacer(minLength: 8)
                Text("Use another email")
                    .font(EirsFont.sans(14))
                    .foregroundStyle(eirs.muted)
                    .onPlainTap(perform: onUseAnotherEmail)
            }
        }
    }
}

@MainActor
private struct ResetStepsCard: View {
    @Environment(\.eirs) private var eirs

    var body: some View {
        EirsCard(padding: EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0), radius: 14) {
            ForEach(0..<3, id: \.self) { index in
                row(index)
            }
        }
    }

    private func stepText(_ index: Int) -> Text {
        let regular = EirsFont.sans(14)
        let bold = EirsFont.sans(14, .semibold)
        switch index {
        case 0: return Text("Open the email from ").font(regular) + Text("no-reply@eirspay.et").font(bold)
        case 1: return Text("Tap ").font(regular) + Text("Reset password").font(bold)
        default: return Text("Choose a new password").font(regular)
        }
    }

    private func row(_ index: Int) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Text("0\(index + 1)").font(EirsFont.mono(14)).foregroundStyle(eirs.brand)
            stepText(index)
                .foregroundColor(eirs.ink)
                .lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .overlay(alignment: .top) {
            if index > 0 { Rectangle().fill(eirs.line).frame(height: 1) }
        }
    }
}

// MARK: - New password fields (shared by reset + change)

@MainActor
struct AuthNewPasswordFields: View {
    @Binding var password: String
    @Binding var confirm: String
    @Binding var show: Bool
    let check: PasswordCheck
    let matchOk: Bool
    var extraError: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 14) {
                AuthPasswordField(label: "New password", text: $password, visible: show, placeholder: "8+ characters",
                                  onToggle: { show.toggle() })
                AuthPasswordStrength(check: check, password: password)
            }
            VStack(alignment: .leading, spacing: 10) {
                AuthPasswordField(label: "Confirm new password", text: $confirm, visible: show,
                                  placeholder: "Repeat password", submitLabel: .done, onToggle: { show.toggle() })
                if !confirm.isEmpty && !matchOk { AuthErrorText(text: "Passwords don't match.") }
                if let extraError { AuthErrorText(text: extraError) }
            }
        }
    }
}

// MARK: - Reset password (from the emailed link)

@MainActor
struct ResetPasswordScreen: View {
    @State private var model: ResetPasswordModel
    let token: String
    let onBack: () -> Void
    let onPasswordReset: () -> Void

    init(container: AppContainer, token: String, onBack: @escaping () -> Void, onPasswordReset: @escaping () -> Void) {
        _model = State(initialValue: ResetPasswordModel(container: container))
        self.token = token
        self.onBack = onBack
        self.onPasswordReset = onPasswordReset
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, spacing: 20) {
                if let address = model.email { AuthConfirmBanner(text: "Link confirmed for \(address)") }
                AuthHeading(plain: "Set a new ", accent: "password",
                            lead: "Pick something you haven't used on EIRSPay before.", size: 38, leadSpacing: 5)
                AuthNewPasswordFields(password: $model.password, confirm: $model.confirm, show: $model.show,
                                      check: model.check, matchOk: model.matchOk)
                if let message = model.errorMessage { AuthErrorText(text: message) }
                PrimaryButton("Reset password", enabled: model.valid, action: submit)
            }
        }
        .task(id: token) { await model.bind(token: token) }
        .onChange(of: model.editToken) { model.clearError() }
        .authPushedChrome()
    }

    private func submit() {
        if !model.valid { return }
        Task {
            if await model.submit() { onPasswordReset() }
        }
    }
}

// MARK: - Reset done

@MainActor
struct ResetDoneScreen: View {
    let onBack: () -> Void
    let onContinueToSignIn: () -> Void

    init(onBack: @escaping () -> Void, onContinueToSignIn: @escaping () -> Void) {
        self.onBack = onBack
        self.onContinueToSignIn = onContinueToSignIn
    }

    var body: some View {
        AuthScaffold(onBack: onBack, spacing: 0) {
            AuthSuccessLayout(
                icon: EirsIcons.check, plain: "Password ", accent: "updated.",
                message: { Text("You've been signed out of other devices. Sign in with your new password to continue.") },
                actions: { PrimaryButton("Continue to sign in", action: onContinueToSignIn) })
        }
        .authPushedChrome()
    }
}

// MARK: - Change temporary password

@MainActor
struct ChangePasswordScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: ChangePasswordModel
    let onBack: () -> Void
    let onSignOut: () -> Void
    let onPasswordChanged: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onSignOut: @escaping () -> Void,
         onPasswordChanged: @escaping () -> Void) {
        _model = State(initialValue: ChangePasswordModel(container: container))
        self.onBack = onBack
        self.onSignOut = onSignOut
        self.onPasswordChanged = onPasswordChanged
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, spacing: 20) {
                banner
                AuthHeading(plain: "Change your ", accent: "password",
                            lead: "Your temporary password came from your account owner or EIRSPay support.",
                            size: 38, leadSpacing: 5)
                AuthPasswordField(label: "Temporary password", text: $model.current, visible: model.show,
                                  placeholder: "From your invite", onToggle: { model.show.toggle() })
                Hairline()
                AuthNewPasswordFields(password: $model.password, confirm: $model.confirm, show: $model.show,
                                      check: model.check, matchOk: model.matchOk,
                                      extraError: model.same ? "Must differ from the temporary password." : nil)
                if let message = model.errorMessage { AuthErrorText(text: message) }
                PrimaryButton("Save and continue", enabled: model.changeOk, action: submit)
                PromptLink(prompt: "Not you?", action: "Sign out", onTap: signOut)
            }
        }
        .task { await model.load() }
        .onChange(of: model.editToken) { model.clearError() }
        .authPushedChrome()
    }

    private var banner: some View {
        let font = EirsFont.sans(14, .medium)
        let semibold = EirsFont.sans(14, .semibold)
        let lead = Text("Signed in as ").font(font)
        let name = Text(model.userName).font(semibold)
        let tail = Text(" with a temporary password. Set your own to continue.").font(font)
        return Banner(background: eirs.amberTint, content: eirs.amberInk, border: eirs.amberBorder, icon: EirsIcons.lock) {
            (lead + name + tail)
        }
    }

    private func submit() {
        if !model.changeOk { return }
        Task {
            if await model.submit() { onPasswordChanged() }
        }
    }

    private func signOut() {
        Task {
            await model.signOutNow()
            onSignOut()
        }
    }
}

// MARK: - Change done

@MainActor
struct ChangeDoneScreen: View {
    @State private var model: ChangeDoneModel
    let onBack: () -> Void
    let onContinue: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onContinue: @escaping () -> Void) {
        _model = State(initialValue: ChangeDoneModel(container: container))
        self.onBack = onBack
        self.onContinue = onContinue
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, spacing: 0) {
                AuthSuccessLayout(
                    icon: EirsIcons.shield, plain: "You're ", accent: "all set.",
                    message: { Text("Your password is saved. Next time, sign in with it instead of the temporary one.") },
                    card: {
                        AuthInfoRows(rows: [("Account", model.accountName), ("Merchant", model.merchantName)])
                    },
                    actions: { PrimaryButton("Go to dashboard", action: openWorkspace) })
            }
        }
        .task { await model.load() }
        .authPushedChrome()
    }

    private func openWorkspace() {
        Task {
            if await model.openWorkspace() { onContinue() }
        }
    }
}
