package com.eirspay.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.usecase.EmailFormat
import com.eirspay.domain.usecase.PasswordPolicy
import com.eirspay.domain.util.Masking
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType

// ---------------------------------------------------------------------------------------------
// Forgot password
// ---------------------------------------------------------------------------------------------

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onSignIn: () -> Unit, onLinkSent: (email: String) -> Unit) {
    val vm: ForgotPasswordViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val sent by rememberUpdatedState(onLinkSent)
    LaunchedEffect(vm) { vm.events.collect { sent(it) } }
    var email by rememberSaveable { mutableStateOf("") }

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, spacing = 22.dp) {
            AuthHeading(
                "Forgot your ", "password?", size = 38.sp, lineHeight = 1.02, leadLineHeight = 1.5,
                lead = "Enter the email on your merchant account. We'll send a link to set a new password.",
            )
            LabeledField("Email address") {
                EirsTextField(
                    email, { email = it; vm.clearError() }, placeholder = "you@business.et",
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Done,
                )
            }
            Spacer(Modifier.weight(1f))
            state.error?.let { ErrorText(it) }
            PrimaryButton("Send reset link", { vm.submit(email) }, enabled = EmailFormat.isValid(email))
            PromptLink("Remembered it?", "Sign in", onSignIn)
        }
    }
}

@Composable
fun ResetLinkSentScreen(email: String, onBack: () -> Unit, onUseAnotherEmail: () -> Unit, onOpenResetLink: (() -> Unit)?) {
    val vm: ResetLinkSentViewModel = hiltViewModel()
    val resent by vm.resent.collectAsStateWithLifecycle()
    val c = Eirs.colors

    LoadingHost(null) {
        AuthScaffold(onBack = onBack, spacing = 0.dp) {
            SuccessLayout(
                icon = EirsIcons.Mail, plain = "Check your ", accent = "inbox.",
                body = {
                    Text(buildAnnotatedString {
                        append("If an account exists for ")
                        withStyle(SpanStyle(fontFamily = EirsFonts.Mono, color = c.ink)) { append(Masking.email(email)) }
                        append(", a reset link is on its way. It expires in 30 minutes.")
                    })
                },
                card = { ResetSteps() },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (onOpenResetLink != null) PrimaryButton("Open reset link (prototype)", onOpenResetLink)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextLink(if (resent) "Sent again" else "Resend link", { vm.resend(email) })
                        Text(
                            "Use another email", modifier = Modifier.plainClickable(onClick = onUseAnotherEmail),
                            style = EirsType.sans(14.sp).copy(color = c.muted),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetSteps() {
    val c = Eirs.colors
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold)
    val steps = listOf(
        buildAnnotatedString { append("Open the email from "); withStyle(bold) { append("no-reply@eirspay.et") } },
        buildAnnotatedString { append("Tap "); withStyle(bold) { append("Reset password") } },
        buildAnnotatedString { append("Choose a new password") },
    )
    EirsCard(padding = PaddingValues(0.dp), radius = 14.dp) {
        steps.forEachIndexed { i, s ->
            Row(
                Modifier.fillMaxWidth().then(if (i < steps.lastIndex) Modifier.bottomHairline(c.line) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("0${i + 1}", style = EirsType.mono(14.sp).copy(color = c.brand))
                Text(s, style = EirsType.sans(14.sp, lineHeight = 1.4).copy(color = c.ink))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Reset password (from the emailed link)
// ---------------------------------------------------------------------------------------------

@Composable
fun ResetPasswordScreen(token: String, onBack: () -> Unit, onPasswordReset: () -> Unit) {
    val vm: ResetPasswordViewModel = hiltViewModel()
    LaunchedEffect(token) { vm.bind(token) }
    val state by vm.state.collectAsStateWithLifecycle()
    val email by vm.email.collectAsStateWithLifecycle()
    val done by rememberUpdatedState(onPasswordReset)
    LaunchedEffect(vm) { vm.events.collect { done() } }

    var pw by rememberSaveable { mutableStateOf("") }
    var pw2 by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    val check = PasswordPolicy.evaluate(pw)
    val matchOk = pw2.isNotEmpty() && pw == pw2
    val valid = check.acceptable && matchOk

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, spacing = 20.dp) {
            email?.let { ConfirmBanner("Link confirmed for $it") }
            AuthHeading(
                "Set a new ", "password", size = 38.sp, lineHeight = 1.02, leadLineHeight = 1.5,
                lead = "Pick something you haven't used on EIRSPay before.",
            )
            NewPasswordFields(pw, { pw = it; vm.clearError() }, pw2, { pw2 = it; vm.clearError() }, show, { show = !show }, check, matchOk)
            state.error?.let { ErrorText(it) }
            PrimaryButton("Reset password", { if (valid) vm.submit(pw) }, enabled = valid)
        }
    }
}

/** New + confirm password with the strength meter between them; "Passwords don't match." below. */
@Composable
private fun NewPasswordFields(
    pw: String, onPw: (String) -> Unit,
    pw2: String, onPw2: (String) -> Unit,
    show: Boolean, onToggle: () -> Unit,
    check: com.eirspay.domain.usecase.PasswordCheck,
    matchOk: Boolean,
    extraError: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AuthPasswordField("New password", pw, onPw, show, onToggle, "8+ characters")
        PasswordStrength(check, pw)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AuthPasswordField("Confirm new password", pw2, onPw2, show, onToggle, "Repeat password", imeAction = ImeAction.Done)
        if (pw2.isNotEmpty() && !matchOk) ErrorText("Passwords don't match.")
        if (extraError != null) ErrorText(extraError)
    }
}

@Composable
fun ResetDoneScreen(onBack: () -> Unit, onContinueToSignIn: () -> Unit) {
    LoadingHost(null) {
        AuthScaffold(onBack = onBack, spacing = 0.dp) {
            SuccessLayout(
                icon = EirsIcons.Check, plain = "Password ", accent = "updated.",
                body = { Text("You've been signed out of other devices. Sign in with your new password to continue.") },
            ) {
                PrimaryButton("Continue to sign in", onContinueToSignIn)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Change temporary password
// ---------------------------------------------------------------------------------------------

@Composable
fun ChangePasswordScreen(onBack: () -> Unit, onSignOut: () -> Unit, onPasswordChanged: () -> Unit) {
    val vm: ChangePasswordViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val changed by rememberUpdatedState(onPasswordChanged)
    val signedOut by rememberUpdatedState(onSignOut)
    LaunchedEffect(vm) {
        vm.events.collect { if (it == ChangePasswordViewModel.Event.Changed) changed() else signedOut() }
    }
    val c = Eirs.colors

    var cur by rememberSaveable { mutableStateOf("") }
    var pw by rememberSaveable { mutableStateOf("") }
    var pw2 by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    val check = PasswordPolicy.evaluate(pw)
    val matchOk = pw2.isNotEmpty() && pw == pw2
    val valid = check.acceptable && matchOk
    val same = cur.isNotEmpty() && pw.isNotEmpty() && cur == pw
    val changeOk = valid && cur.length >= 4 && !same

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, spacing = 20.dp) {
            Banner(
                c.amberTint, c.amberInk, border = c.amberBorder, icon = EirsIcons.Lock,
                textStyle = EirsType.sans(14.sp, lineHeight = 1.45),
            ) {
                Text(buildAnnotatedString {
                    append("Signed in as ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(vm.userName) }
                    append(" with a temporary password. Set your own to continue.")
                })
            }
            AuthHeading(
                "Change your ", "password", size = 38.sp, lineHeight = 1.02, leadLineHeight = 1.5,
                lead = "Your temporary password came from your account owner or EIRSPay support.",
            )
            AuthPasswordField("Temporary password", cur, { cur = it; vm.clearError() }, show, { show = !show }, "From your invite")
            Hairline()
            NewPasswordFields(
                pw, { pw = it; vm.clearError() }, pw2, { pw2 = it; vm.clearError() }, show, { show = !show }, check, matchOk,
                extraError = if (same) "Must differ from the temporary password." else null,
            )
            state.error?.let { ErrorText(it) }
            PrimaryButton("Save and continue", { if (changeOk) vm.submit(cur, pw) }, enabled = changeOk)
            PromptLink("Not you?", "Sign out", vm::signOutNow)
        }
    }
}

@Composable
fun ChangeDoneScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    val vm: ChangeDoneViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val cont by rememberUpdatedState(onContinue)
    LaunchedEffect(vm) { vm.events.collect { cont() } }

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, spacing = 0.dp) {
            SuccessLayout(
                icon = EirsIcons.Shield, plain = "You're ", accent = "all set.",
                body = { Text("Your password is saved. Next time, sign in with it instead of the temporary one.") },
                card = { InfoRows(listOf("Account" to vm.accountName, "Merchant" to vm.merchantName)) },
            ) {
                PrimaryButton("Go to dashboard", vm::openWorkspace)
            }
        }
    }
}
