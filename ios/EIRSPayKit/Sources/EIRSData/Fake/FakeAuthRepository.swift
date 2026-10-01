import Foundation
import EIRSDomain

/// Local stand-in for the auth API. Any credentials sign in as the owner account, which starts with a
/// temporary password so the forced change flow is reachable. Any 6-digit code verifies.
public final class FakeAuthRepository: AuthRepository, @unchecked Sendable {
    private struct State {
        var mustChangePassword = true
        var pending: (request: SignUpRequest, account: PendingAccount)?
    }

    private let merchant = Merchant(id: "MRC-KL7Z", displayName: "Addispay Financial Tech.", seller: SampleData.seller, environment: "DEV")
    private let owner = User(id: "UID-E0F6L4A99C", firstName: "Yakob", lastName: "Girma", email: "yakobgirmad@gmail.com",
                             phone: "+251921280809", role: .owner)
    private let state = AsyncStore(State())
    private let sessionStore = AsyncStore<Session?>(nil)

    public init() {}

    public func currentSession() async -> Session? { sessionStore.value }
    public func observeSession() -> AsyncStream<Session?> { sessionStore.stream() }

    public func signIn(identifier: String, password: String) async -> SignInResult {
        await FakeLatency.wait()
        if identifier.isEmpty || password.isEmpty { return .invalidCredentials }
        let session = Session(user: owner, merchant: merchant)
        sessionStore.set(session)
        return state.value.mustChangePassword ? .passwordChangeRequired(session) : .success(session)
    }

    public func signUp(_ request: SignUpRequest) async throws -> PendingAccount {
        await FakeLatency.wait()
        let account = PendingAccount(fullName: "\(request.firstName) \(request.lastName)".trimmingCharacters(in: .whitespaces),
                                     maskedPhone: Masking.phone(request.phone), maskedEmail: Masking.email(request.email))
        state.update { $0.pending = (request, account) }
        return account
    }

    public func pendingAccount() async -> PendingAccount? { state.value.pending?.account }

    public func sendVerificationCode(channel: VerificationChannel) async throws -> String {
        await FakeLatency.wait()
        let account = state.value.pending?.account
            ?? PendingAccount(fullName: "", maskedPhone: "+251 92 ••• 2273", maskedEmail: "yi••••@gmail.com")
        return channel == .sms ? account.maskedPhone : account.maskedEmail
    }

    public func verifyCode(_ code: String) async throws -> Bool {
        await FakeLatency.wait()
        if let request = state.value.pending?.request {
            let user = User(id: "UID-NEW01", firstName: request.firstName, lastName: request.lastName,
                            email: request.email, phone: request.phone, role: .owner)
            sessionStore.set(Session(user: user, merchant: merchant))
        }
        return true
    }

    public func requestPasswordReset(email: String) async throws { await FakeLatency.wait() }

    public func validateResetToken(_ token: String) async throws -> String {
        await FakeLatency.wait()
        return owner.email
    }

    public func resetPassword(token: String, newPassword: String) async throws {
        await FakeLatency.wait()
        state.update { $0.mustChangePassword = false }
        sessionStore.set(nil)
    }

    public func changeTemporaryPassword(current: String, newPassword: String) async throws {
        await FakeLatency.wait()
        state.update { $0.mustChangePassword = false }
    }

    public func signOut() async { sessionStore.set(nil) }
}
