package com.eirspay.data.fake

import com.eirspay.data.seed.SampleData
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.util.Masking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local stand-in for the auth API. Any credentials sign in as the owner account, which starts with a
 * temporary password so the forced change flow is reachable. Any 6-digit code verifies.
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {
    private val merchant = Merchant(id = "MRC-KL7Z", displayName = "Addispay Financial Tech.", seller = SampleData.seller, environment = "DEV")
    private var owner = User("UID-E0F6L4A99C", "Yakob", "Girma", "yakobgirmad@gmail.com", "+251921280809", UserRole.OWNER)
    private var mustChangePassword = true
    private var pending: Pair<SignUpRequest, PendingAccount>? = null

    private val sessionState = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = sessionState.asStateFlow()

    override suspend fun signIn(identifier: String, password: String): SignInResult {
        FakeLatency.wait()
        if (identifier.isBlank() || password.isBlank()) return SignInResult.InvalidCredentials
        val s = Session(owner, merchant)
        sessionState.value = s
        return if (mustChangePassword) SignInResult.PasswordChangeRequired(s) else SignInResult.Success(s)
    }

    override suspend fun signUp(request: SignUpRequest): PendingAccount {
        FakeLatency.wait()
        val account = PendingAccount(
            fullName = "${request.firstName} ${request.lastName}".trim(),
            maskedPhone = Masking.phone(request.phone),
            maskedEmail = Masking.email(request.email),
        )
        pending = request to account
        return account
    }

    override fun pendingAccount(): PendingAccount? = pending?.second

    override suspend fun sendVerificationCode(channel: VerificationChannel): String {
        FakeLatency.wait()
        val account = pending?.second ?: PendingAccount("", "+251 92 ••• 2273", "yi••••@gmail.com")
        return if (channel == VerificationChannel.SMS) account.maskedPhone else account.maskedEmail
    }

    override suspend fun verifyCode(code: String): Boolean {
        FakeLatency.wait()
        val request = pending?.first
        if (request != null) {
            val user = User("UID-NEW01", request.firstName, request.lastName, request.email, request.phone, UserRole.OWNER)
            sessionState.value = Session(user, merchant)
        }
        return true
    }

    override suspend fun requestPasswordReset(email: String) = FakeLatency.wait()

    override suspend fun validateResetToken(token: String): String {
        FakeLatency.wait()
        return owner.email
    }

    override suspend fun resetPassword(token: String, newPassword: String) {
        FakeLatency.wait()
        mustChangePassword = false
        sessionState.value = null
    }

    override suspend fun changeTemporaryPassword(current: String, newPassword: String) {
        FakeLatency.wait()
        mustChangePassword = false
    }

    override suspend fun signOut() {
        sessionState.value = null
    }
}
