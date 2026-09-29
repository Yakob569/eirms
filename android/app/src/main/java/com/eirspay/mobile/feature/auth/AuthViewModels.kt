package com.eirspay.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.SignInResult
import com.eirspay.domain.model.SignUpRequest
import com.eirspay.domain.model.VerificationChannel
import com.eirspay.domain.repository.AuthRepository
import com.eirspay.domain.usecase.*
import com.eirspay.domain.util.Masking
import com.eirspay.mobile.ui.designsystem.LoaderMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Prototype sample names, used when the repository has nothing yet.
internal const val SampleNewAccount = "Yishak Girma"
internal const val SampleOwner = "Yakob Girma"
internal const val SampleMerchant = "Addispay Financial Tech."
internal const val SampleMaskedPhone = "+251 92 ••• 2273"
internal const val SampleMaskedEmail = "yi••••@gmail.com"

/** Loader + inline error shared by every auth screen. */
data class AuthUiState(val loading: LoaderMessage? = null, val error: String? = null)

/**
 * Base for the auth view models: runs an action under the brand loader and emits a one-shot
 * navigation event when it succeeds.
 */
abstract class AuthActionViewModel<E : Any> : ViewModel() {
    protected val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    private val eventChannel = Channel<E>(Channel.BUFFERED)
    val events: Flow<E> = eventChannel.receiveAsFlow()

    fun clearError() = mutableState.update { it.copy(error = null) }

    protected fun showError(message: String) = mutableState.update { it.copy(error = message) }

    protected suspend fun emit(event: E) = eventChannel.send(event)

    /** [block] returns the event to emit, or null after reporting its own error. */
    protected fun runWithLoader(title: String, subtitle: String, failure: String = GenericFailure, block: suspend () -> E?) {
        if (mutableState.value.loading != null) return
        mutableState.value = AuthUiState(loading = LoaderMessage(title, subtitle))
        viewModelScope.launch {
            val result = runCatching { block() }
            mutableState.update { it.copy(loading = null, error = if (result.isFailure) failure else it.error) }
            result.getOrNull()?.let { eventChannel.send(it) }
        }
    }

    companion object {
        const val GenericFailure = "Something went wrong. Please try again."
    }
}

// ---------------------------------------------------------------------------------------------

@HiltViewModel
class SignInViewModel @Inject constructor(private val signIn: SignInUseCase) : AuthActionViewModel<SignInViewModel.Event>() {
    data class Event(val mustChangePassword: Boolean)

    fun submit(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) { showError("Enter your email or phone and password."); return }
        runWithLoader("Signing you in", "Temporary password detected") {
            when (val r = signIn(identifier, password)) {
                is SignInResult.Success -> Event(false)
                is SignInResult.PasswordChangeRequired -> Event(true)
                SignInResult.InvalidCredentials -> { showError("Those details don't match an account."); null }
            }
        }
    }
}

@HiltViewModel
class SignUpViewModel @Inject constructor(private val signUp: SignUpUseCase) : AuthActionViewModel<Unit>() {
    fun submit(request: SignUpRequest, confirm: String) {
        val r = request
        when {
            listOf(r.firstName, r.lastName, r.email, r.phone, r.password).any { it.isBlank() } -> showError("Fill in every field to continue.")
            r.password != confirm -> showError("Passwords don't match.")
            else -> runWithLoader("Creating your account", "Registering your merchant profile") {
                signUp(r.copy(firstName = r.firstName.trim(), lastName = r.lastName.trim(), email = r.email.trim(), phone = "+251" + r.phone.filter { it.isDigit() }))
                Unit
            }
        }
    }
}

@HiltViewModel
class VerifyMethodViewModel @Inject constructor(
    auth: AuthRepository,
    private val sendCode: SendVerificationCodeUseCase,
) : AuthActionViewModel<VerifyMethodViewModel.Event>() {
    data class Event(val channel: VerificationChannel, val sentTo: String)

    private val pending = auth.pendingAccount()
    val accountName: String = pending?.fullName?.takeIf { it.isNotBlank() } ?: SampleNewAccount
    val maskedPhone: String = pending?.maskedPhone ?: SampleMaskedPhone
    val maskedEmail: String = pending?.maskedEmail ?: SampleMaskedEmail

    fun send(channel: VerificationChannel) {
        val subtitle = if (channel == VerificationChannel.SMS) "By SMS to $maskedPhone" else "To $maskedEmail"
        runWithLoader("Sending your code", subtitle) { Event(channel, sendCode(channel)) }
    }
}

@HiltViewModel
class VerifyCodeViewModel @Inject constructor(
    private val verifyCode: VerifyCodeUseCase,
    private val sendCode: SendVerificationCodeUseCase,
) : AuthActionViewModel<Unit>() {
    fun verify(code: String) {
        if (code.length != 6) return
        runWithLoader("Verifying your code", "Confirming with EIRS") {
            if (verifyCode(code)) Unit else { showError("That code didn't work. Check it and try again."); null }
        }
    }

    /** Re-sends quietly in the background; the screen resets its timer immediately. */
    fun resend(channel: String) {
        clearError()
        val ch = VerificationChannel.entries.firstOrNull { it.name == channel } ?: VerificationChannel.SMS
        viewModelScope.launch { runCatching { sendCode(ch) } }
    }
}

@HiltViewModel
class VerifySuccessViewModel @Inject constructor(auth: AuthRepository) : ViewModel() {
    val accountName: String = auth.session.value?.user?.fullName
        ?: auth.pendingAccount()?.fullName?.takeIf { it.isNotBlank() }
        ?: SampleNewAccount
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val requestReset: RequestPasswordResetUseCase) : AuthActionViewModel<String>() {
    fun submit(email: String) {
        if (!EmailFormat.isValid(email)) return
        val e = email.trim()
        runWithLoader("Sending reset link", "To " + Masking.email(e)) { requestReset(e); e }
    }
}

@HiltViewModel
class ResetLinkSentViewModel @Inject constructor(private val requestReset: RequestPasswordResetUseCase) : ViewModel() {
    private val mutableResent = MutableStateFlow(false)
    val resent: StateFlow<Boolean> = mutableResent.asStateFlow()

    fun resend(email: String) {
        mutableResent.value = true
        viewModelScope.launch { runCatching { requestReset(email) } }
    }
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val validateToken: ValidateResetTokenUseCase,
    private val resetPassword: ResetPasswordUseCase,
) : AuthActionViewModel<Unit>() {
    private val mutableEmail = MutableStateFlow<String?>(null)
    /** The masked account email once the token validates. */
    val email: StateFlow<String?> = mutableEmail.asStateFlow()
    private var token: String? = null

    fun bind(token: String) {
        if (this.token == token) return
        this.token = token
        mutableState.value = AuthUiState(loading = LoaderMessage("Checking your link", "Validating the reset token"))
        viewModelScope.launch {
            val r = runCatching { validateToken(token) }
            mutableEmail.value = r.getOrNull()?.let { Masking.email(it) }
            mutableState.value = AuthUiState(error = if (r.isFailure) "This reset link is invalid or has expired." else null)
        }
    }

    fun submit(password: String) {
        val t = token ?: return
        runWithLoader("Updating password", "Signing out other sessions") { resetPassword(t, password) }
    }
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    auth: AuthRepository,
    private val change: ChangeTemporaryPasswordUseCase,
    private val signOut: SignOutUseCase,
) : AuthActionViewModel<ChangePasswordViewModel.Event>() {
    enum class Event { Changed, SignedOut }

    val userName: String = auth.session.value?.user?.fullName ?: SampleOwner

    fun submit(current: String, password: String) =
        runWithLoader("Saving your password", "Securing your account") { change(current, password); Event.Changed }

    fun signOutNow() {
        viewModelScope.launch { runCatching { signOut() }; emit(Event.SignedOut) }
    }
}

@HiltViewModel
class ChangeDoneViewModel @Inject constructor(auth: AuthRepository) : AuthActionViewModel<Unit>() {
    private val session = auth.session.value
    val accountName: String = session?.user?.fullName ?: SampleOwner
    val merchantName: String = session?.merchant?.displayName ?: SampleMerchant

    fun openWorkspace() = runWithLoader("Opening your workspace", "Loading your merchant dashboard") { delay(1800); Unit }
}
