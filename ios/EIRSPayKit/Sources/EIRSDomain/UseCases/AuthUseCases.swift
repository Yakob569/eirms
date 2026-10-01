import Foundation

public struct SignInUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(identifier: String, password: String) async -> SignInResult {
        await auth.signIn(identifier: identifier.trimmingCharacters(in: .whitespaces), password: password)
    }
}

public struct SignUpUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(_ request: SignUpRequest) async throws -> PendingAccount { try await auth.signUp(request) }
}

public struct SendVerificationCodeUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(channel: VerificationChannel) async throws -> String {
        try await auth.sendVerificationCode(channel: channel)
    }
}

public struct VerifyCodeUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(_ code: String) async throws -> Bool {
        guard code.count == 6 else { return false }
        return try await auth.verifyCode(code)
    }
}

public struct RequestPasswordResetUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(email: String) async throws {
        try await auth.requestPasswordReset(email: email.trimmingCharacters(in: .whitespaces))
    }
}

public struct ValidateResetTokenUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(token: String) async throws -> String { try await auth.validateResetToken(token) }
}

public struct ResetPasswordUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(token: String, password: String) async throws {
        try await auth.resetPassword(token: token, newPassword: password)
    }
}

public struct ChangeTemporaryPasswordUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction(current: String, password: String) async throws {
        try await auth.changeTemporaryPassword(current: current, newPassword: password)
    }
}

public struct SignOutUseCase: Sendable {
    let auth: AuthRepository
    public init(auth: AuthRepository) { self.auth = auth }
    public func callAsFunction() async { await auth.signOut() }
}
