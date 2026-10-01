import Foundation
import Observation
import EIRSDomain

/// Copy and prototype sample values shared by the auth models.
enum AuthCopy {
    static let genericFailure = "Something went wrong. Please try again."
    // Prototype sample names, used when the repository has nothing yet.
    static let sampleNewAccount = "Yishak Girma"
    static let sampleOwner = "Yakob Girma"
    static let sampleMerchant = "Addispay Financial Tech."
    static let sampleMaskedPhone = "+251 92 ••• 2273"
    static let sampleMaskedEmail = "yi••••@gmail.com"
}

private extension String {
    var isBlankText: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}

// MARK: - Sign in

@MainActor @Observable
final class SignInModel {
    var identifier = ""
    var password = ""
    var loading: LoaderMessage?
    var errorMessage: String?
    private let signIn: SignInUseCase

    init(container: AppContainer) { signIn = container.signIn }

    var editToken: String { identifier + "\u{1}" + password }
    func clearError() { errorMessage = nil }

    /// Returns `mustChangePassword` on success, nil when the sign-in did not go through.
    func submit() async -> Bool? {
        if loading != nil { return nil }
        if identifier.isBlankText || password.isBlankText {
            errorMessage = "Enter your email or phone and password."
            return nil
        }
        loading = LoaderMessage(title: "Signing you in", subtitle: "Temporary password detected")
        let result = await signIn(identifier: identifier, password: password)
        loading = nil
        switch result {
        case .success:
            return false
        case .passwordChangeRequired:
            return true
        case .invalidCredentials:
            errorMessage = "Those details don't match an account."
            return nil
        }
    }
}

// MARK: - Sign up

@MainActor @Observable
final class SignUpModel {
    var first = ""
    var last = ""
    var email = ""
    var phone = ""
    var password = ""
    var confirm = ""
    var terms = true
    var loading: LoaderMessage?
    var errorMessage: String?
    private let signUp: SignUpUseCase

    init(container: AppContainer) { signUp = container.signUp }

    var editToken: String { [first, last, email, phone, password, confirm].joined(separator: "\u{1}") }
    func clearError() { errorMessage = nil }

    /// True when the account was created.
    func submit() async -> Bool {
        if loading != nil { return false }
        let fields = [first, last, email, phone, password]
        if fields.contains(where: { $0.isBlankText }) {
            errorMessage = "Fill in every field to continue."
            return false
        }
        if password != confirm {
            errorMessage = "Passwords don't match."
            return false
        }
        let digits = phone.filter { $0.isNumber }
        let request = SignUpRequest(
            firstName: first.trimmingCharacters(in: .whitespacesAndNewlines),
            lastName: last.trimmingCharacters(in: .whitespacesAndNewlines),
            email: email.trimmingCharacters(in: .whitespacesAndNewlines),
            phone: "+251" + digits,
            password: password)
        loading = LoaderMessage(title: "Creating your account", subtitle: "Registering your merchant profile")
        do {
            _ = try await signUp(request)
            loading = nil
            return true
        } catch {
            loading = nil
            errorMessage = AuthCopy.genericFailure
            return false
        }
    }
}

// MARK: - Verify method

@MainActor @Observable
final class VerifyMethodModel {
    var accountName = AuthCopy.sampleNewAccount
    var maskedPhone = AuthCopy.sampleMaskedPhone
    var maskedEmail = AuthCopy.sampleMaskedEmail
    var loading: LoaderMessage?
    var errorMessage: String?
    private let auth: AuthRepository
    private let sendCode: SendVerificationCodeUseCase

    init(container: AppContainer) {
        auth = container.auth
        sendCode = container.sendVerificationCode
    }

    func load() async {
        guard let pending = await auth.pendingAccount() else { return }
        if !pending.fullName.isBlankText { accountName = pending.fullName }
        maskedPhone = pending.maskedPhone
        maskedEmail = pending.maskedEmail
    }

    /// Returns the channel and masked destination once the code is on its way.
    func send(_ channel: VerificationChannel) async -> (channel: VerificationChannel, sentTo: String)? {
        if loading != nil { return nil }
        let subtitle = channel == .sms ? "By SMS to \(maskedPhone)" : "To \(maskedEmail)"
        loading = LoaderMessage(title: "Sending your code", subtitle: subtitle)
        do {
            let sentTo = try await sendCode(channel: channel)
            loading = nil
            return (channel: channel, sentTo: sentTo)
        } catch {
            loading = nil
            errorMessage = AuthCopy.genericFailure
            return nil
        }
    }
}

// MARK: - Verify code

@MainActor @Observable
final class VerifyCodeModel {
    var code = ""
    var secs = 882
    var loading: LoaderMessage?
    var errorMessage: String?
    private let channel: String
    private let verifyCode: VerifyCodeUseCase
    private let sendCode: SendVerificationCodeUseCase

    init(container: AppContainer, channel: String) {
        self.channel = channel
        verifyCode = container.verifyCode
        sendCode = container.sendVerificationCode
    }

    var countdown: String {
        let seconds = secs % 60
        let padded = seconds < 10 ? "0\(seconds)" : "\(seconds)"
        return "\(secs / 60):\(padded)"
    }

    func setCode(_ raw: String) {
        code = String(raw.filter { $0.isNumber }.prefix(6))
        errorMessage = nil
    }

    func tick() { if secs > 0 { secs -= 1 } }

    /// True when the code was accepted.
    func verify() async -> Bool {
        if code.count != 6 || loading != nil { return false }
        loading = LoaderMessage(title: "Verifying your code", subtitle: "Confirming with EIRS")
        do {
            let ok = try await verifyCode(code)
            loading = nil
            if !ok { errorMessage = "That code didn't work. Check it and try again." }
            return ok
        } catch {
            loading = nil
            errorMessage = AuthCopy.genericFailure
            return false
        }
    }

    /// Re-sends quietly in the background and resets the timer immediately.
    func resend() {
        errorMessage = nil
        secs = 900
        code = ""
        let target = VerificationChannel(rawValue: channel) ?? .sms
        let send = sendCode
        Task { _ = try? await send(channel: target) }
    }
}

// MARK: - Verify success

@MainActor @Observable
final class VerifySuccessModel {
    var accountName = AuthCopy.sampleNewAccount
    private let auth: AuthRepository

    init(container: AppContainer) { auth = container.auth }

    func load() async {
        if let session = await auth.currentSession() {
            accountName = session.user.fullName
        } else if let pending = await auth.pendingAccount(), !pending.fullName.isBlankText {
            accountName = pending.fullName
        }
    }
}

// MARK: - Forgot password

@MainActor @Observable
final class ForgotPasswordModel {
    var email = ""
    var loading: LoaderMessage?
    var errorMessage: String?
    private let requestReset: RequestPasswordResetUseCase

    init(container: AppContainer) { requestReset = container.requestPasswordReset }

    var isValid: Bool { EmailFormat.isValid(email) }
    func clearError() { errorMessage = nil }

    /// Returns the trimmed email once the link has been requested.
    func submit() async -> String? {
        if !isValid || loading != nil { return nil }
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        loading = LoaderMessage(title: "Sending reset link", subtitle: "To " + Masking.email(trimmed))
        do {
            try await requestReset(email: trimmed)
            loading = nil
            return trimmed
        } catch {
            loading = nil
            errorMessage = AuthCopy.genericFailure
            return nil
        }
    }
}

// MARK: - Reset link sent

@MainActor @Observable
final class ResetLinkSentModel {
    var resent = false
    private let requestReset: RequestPasswordResetUseCase

    init(container: AppContainer) { requestReset = container.requestPasswordReset }

    func resend(email: String) {
        resent = true
        let request = requestReset
        Task { _ = try? await request(email: email) }
    }
}

// MARK: - Reset password

@MainActor @Observable
final class ResetPasswordModel {
    var password = ""
    var confirm = ""
    var show = false
    /// The masked account email once the token validates.
    var email: String?
    var loading: LoaderMessage?
    var errorMessage: String?
    private var token: String?
    private let validateToken: ValidateResetTokenUseCase
    private let resetPassword: ResetPasswordUseCase

    init(container: AppContainer) {
        validateToken = container.validateResetToken
        resetPassword = container.resetPassword
    }

    var check: PasswordCheck { PasswordPolicy.evaluate(password) }
    var matchOk: Bool { !confirm.isEmpty && password == confirm }
    var valid: Bool { check.acceptable && matchOk }
    var editToken: String { password + "\u{1}" + confirm }
    func clearError() { errorMessage = nil }

    func bind(token newToken: String) async {
        if token == newToken { return }
        token = newToken
        loading = LoaderMessage(title: "Checking your link", subtitle: "Validating the reset token")
        do {
            let address = try await validateToken(token: newToken)
            email = Masking.email(address)
            errorMessage = nil
        } catch {
            email = nil
            errorMessage = "This reset link is invalid or has expired."
        }
        loading = nil
    }

    /// True once the password has been reset.
    func submit() async -> Bool {
        guard let current = token, valid, loading == nil else { return false }
        loading = LoaderMessage(title: "Updating password", subtitle: "Signing out other sessions")
        do {
            try await resetPassword(token: current, password: password)
            loading = nil
            return true
        } catch {
            loading = nil
            errorMessage = AuthCopy.genericFailure
            return false
        }
    }
}

// MARK: - Change temporary password

@MainActor @Observable
final class ChangePasswordModel {
    var current = ""
    var password = ""
    var confirm = ""
    var show = false
    var userName = AuthCopy.sampleOwner
    var loading: LoaderMessage?
    var errorMessage: String?
    private let auth: AuthRepository
    private let change: ChangeTemporaryPasswordUseCase
    private let signOutUseCase: SignOutUseCase

    init(container: AppContainer) {
        auth = container.auth
        change = container.changeTemporaryPassword
        signOutUseCase = container.signOut
    }

    var check: PasswordCheck { PasswordPolicy.evaluate(password) }
    var matchOk: Bool { !confirm.isEmpty && password == confirm }
    var same: Bool { !current.isEmpty && !password.isEmpty && current == password }
    var changeOk: Bool { check.acceptable && matchOk && current.count >= 4 && !same }
    var editToken: String { [current, password, confirm].joined(separator: "\u{1}") }
    func clearError() { errorMessage = nil }

    func load() async {
        if let session = await auth.currentSession() { userName = session.user.fullName }
    }

    /// True once the password has been saved.
    func submit() async -> Bool {
        if !changeOk || loading != nil { return false }
        loading = LoaderMessage(title: "Saving your password", subtitle: "Securing your account")
        do {
            try await change(current: current, password: password)
            loading = nil
            return true
        } catch {
            loading = nil
            errorMessage = AuthCopy.genericFailure
            return false
        }
    }

    func signOutNow() async { await signOutUseCase() }
}

// MARK: - Change done

@MainActor @Observable
final class ChangeDoneModel {
    var accountName = AuthCopy.sampleOwner
    var merchantName = AuthCopy.sampleMerchant
    var loading: LoaderMessage?
    private let auth: AuthRepository

    init(container: AppContainer) { auth = container.auth }

    func load() async {
        guard let session = await auth.currentSession() else { return }
        accountName = session.user.fullName
        merchantName = session.merchant.displayName
    }

    /// True after the short "opening workspace" loader.
    func openWorkspace() async -> Bool {
        if loading != nil { return false }
        loading = LoaderMessage(title: "Opening your workspace", subtitle: "Loading your merchant dashboard")
        try? await Task.sleep(nanoseconds: 1_800_000_000)
        loading = nil
        return true
    }
}
