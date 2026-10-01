import SwiftUI
import EIRSDomain

// MARK: - Sign in

@MainActor
struct SignInScreen: View {
    @State private var model: SignInModel
    let onBack: () -> Void
    let onForgotPassword: () -> Void
    let onCreateAccount: () -> Void
    let onSignedIn: (Bool) -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onForgotPassword: @escaping () -> Void,
         onCreateAccount: @escaping () -> Void, onSignedIn: @escaping (Bool) -> Void) {
        _model = State(initialValue: SignInModel(container: container))
        self.onBack = onBack
        self.onForgotPassword = onForgotPassword
        self.onCreateAccount = onCreateAccount
        self.onSignedIn = onSignedIn
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, spacing: 24) {
                AuthHeading(plain: "Welcome ", accent: "back", lead: "Sign in to your merchant account.")
                LabeledField("Email or phone") {
                    EirsTextField("you@business.et", text: $model.identifier, keyboard: .emailAddress)
                }
                LabeledField(label: "Password", trailingLabel: {
                    TextLink("Forgot?", size: 13, weight: .medium, action: onForgotPassword)
                }, content: {
                    EirsTextField("••••••••", text: $model.password, submitLabel: .done, secure: true, onSubmit: submit)
                })
                Spacer(minLength: 0)
                if let message = model.errorMessage { AuthErrorText(text: message) }
                PrimaryButton("Sign in", action: submit)
                PromptLink(prompt: "New to EIRSPay?", action: "Create account", onTap: onCreateAccount)
            }
        }
        .onChange(of: model.editToken) { model.clearError() }
        .authPushedChrome()
    }

    private func submit() {
        Task {
            if let mustChange = await model.submit() { onSignedIn(mustChange) }
        }
    }
}

// MARK: - Sign up

@MainActor
struct SignUpScreen: View {
    @Environment(\.eirs) private var eirs
    @State private var model: SignUpModel
    let onBack: () -> Void
    let onSignIn: () -> Void
    let onAccountCreated: () -> Void

    init(container: AppContainer, onBack: @escaping () -> Void, onSignIn: @escaping () -> Void,
         onAccountCreated: @escaping () -> Void) {
        _model = State(initialValue: SignUpModel(container: container))
        self.onBack = onBack
        self.onSignIn = onSignIn
        self.onAccountCreated = onAccountCreated
    }

    var body: some View {
        LoadingHost(loading: model.loading) {
            AuthScaffold(onBack: onBack, step: 1, spacing: 18) {
                AuthHeading(plain: "Create your merchant account", lead: "Register with your business contact details to start onboarding.", size: 36)
                HStack(alignment: .top, spacing: 12) {
                    field("First name", "Yishak", $model.first, capitalization: .words)
                    field("Last name", "Girma", $model.last, capitalization: .words)
                }
                field("Email address", "you@business.et", $model.email, keyboard: .emailAddress)
                phoneField
                field("Password", "8+ characters", $model.password, secure: true)
                field("Confirm password", "Repeat password", $model.confirm, secure: true, last: true)
                termsRow
                if let message = model.errorMessage { AuthErrorText(text: message) }
                PrimaryButton("Create account", enabled: model.terms, action: submit)
                PromptLink(prompt: "Already registered?", action: "Sign in", onTap: onSignIn)
            }
        }
        .onChange(of: model.editToken) { model.clearError() }
        .authPushedChrome()
    }

    private func field(_ label: String, _ placeholder: String, _ text: Binding<String>,
                       keyboard: UIKeyboardType = .default, capitalization: TextInputAutocapitalization = .never,
                       secure: Bool = false, last: Bool = false) -> some View {
        LabeledField(label, labelSize: 13, gap: 7) {
            EirsTextField(placeholder, text: text, height: 50, keyboard: keyboard, capitalization: capitalization,
                          submitLabel: last ? .done : .next, secure: secure, horizontalPadding: 12)
        }
    }

    private var phoneField: some View {
        LabeledField("Phone number", labelSize: 13, gap: 7) {
            EirsTextField(placeholder: "9 12 34 56 78", text: $model.phone, height: 50, keyboard: .phonePad,
                          focusRing: false, horizontalPadding: 12,
                          leading: { phonePrefix },
                          trailing: { EmptyView() })
        }
    }

    private var phonePrefix: some View {
        Text("+251")
            .font(EirsFont.mono(14))
            .foregroundStyle(eirs.muted)
            .padding(.horizontal, 12)
            .frame(maxHeight: .infinity)
            .overlay(alignment: .trailing) { Rectangle().fill(eirs.line).frame(width: 1) }
    }

    private var termsRow: some View {
        let lead = Text("I agree to the ").font(EirsFont.sans(14)).foregroundColor(eirs.muted)
        let terms = Text("Terms of Service").font(EirsFont.sans(14)).foregroundColor(eirs.brand)
        let joiner = Text(" and ").font(EirsFont.sans(14)).foregroundColor(eirs.muted)
        let privacy = Text("Privacy Policy").font(EirsFont.sans(14)).foregroundColor(eirs.brand)
        return HStack(alignment: .top, spacing: 10) {
            EirsCheckbox(checked: model.terms)
            (lead + terms + joiner + privacy)
                .lineSpacing(3)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
        .onPlainTap { model.terms.toggle() }
    }

    private func submit() {
        Task {
            if await model.submit() { onAccountCreated() }
        }
    }
}
