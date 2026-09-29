package com.eirspay.domain.usecase

import com.eirspay.domain.model.*
import com.eirspay.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(identifier: String, password: String): SignInResult =
        auth.signIn(identifier.trim(), password)
}

class SignUpUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(request: SignUpRequest): PendingAccount = auth.signUp(request)
}

class SendVerificationCodeUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(channel: VerificationChannel): String = auth.sendVerificationCode(channel)
}

class VerifyCodeUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(code: String): Boolean = code.length == 6 && auth.verifyCode(code)
}

class RequestPasswordResetUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String) = auth.requestPasswordReset(email.trim())
}

class ValidateResetTokenUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(token: String): String = auth.validateResetToken(token)
}

class ResetPasswordUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(token: String, password: String) = auth.resetPassword(token, password)
}

class ChangeTemporaryPasswordUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke(current: String, password: String) = auth.changeTemporaryPassword(current, password)
}

class SignOutUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.signOut()
}

object EmailFormat {
    private val regex = Regex(".+@.+\\..+")
    fun isValid(email: String) = regex.matches(email.trim())
}
