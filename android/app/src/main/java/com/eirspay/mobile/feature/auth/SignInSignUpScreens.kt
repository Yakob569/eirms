package com.eirspay.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.SignUpRequest
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignedIn: (mustChangePassword: Boolean) -> Unit,
) {
    val vm: SignInViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(vm) { vm.events.collect { signedIn(it.mustChangePassword) } }

    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, spacing = 24.dp) {
            AuthHeading("Welcome ", "back", lead = "Sign in to your merchant account.")
            LabeledField("Email or phone") {
                EirsTextField(
                    identifier, { identifier = it; vm.clearError() }, placeholder = "you@business.et",
                    keyboardType = KeyboardType.Email,
                )
            }
            LabeledField("Password", trailingLabel = {
                TextLink("Forgot?", onForgotPassword, size = 13.sp, weight = FontWeight.Medium)
            }) {
                EirsTextField(
                    password, { password = it; vm.clearError() }, placeholder = "••••••••",
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
            Spacer(Modifier.weight(1f))
            state.error?.let { ErrorText(it) }
            PrimaryButton("Sign in", { vm.submit(identifier, password) })
            PromptLink("New to EIRSPay?", "Create account", onCreateAccount)
        }
    }
}

@Composable
fun SignUpScreen(onBack: () -> Unit, onSignIn: () -> Unit, onAccountCreated: () -> Unit) {
    val vm: SignUpViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val created by rememberUpdatedState(onAccountCreated)
    LaunchedEffect(vm) { vm.events.collect { created() } }

    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var terms by rememberSaveable { mutableStateOf(true) }
    val c = Eirs.colors
    val edit: (String, (String) -> Unit) -> Unit = { v, set -> set(v); vm.clearError() }

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, step = 1, spacing = 18.dp) {
            AuthHeading(
                "Create your merchant account", size = 36.sp, lineHeight = 1.02,
                lead = "Register with your business contact details to start onboarding.",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SignUpField("First name", first, { edit(it) { v -> first = v } }, "Yishak", Modifier.weight(1f))
                SignUpField("Last name", last, { edit(it) { v -> last = v } }, "Girma", Modifier.weight(1f))
            }
            SignUpField("Email address", email, { edit(it) { v -> email = v } }, "you@business.et", keyboardType = KeyboardType.Email)
            LabeledField("Phone number", labelSize = 13.sp, gap = 7.dp) {
                EirsTextField(
                    phone, { edit(it) { v -> phone = v } }, placeholder = "9 12 34 56 78", height = 50.dp,
                    keyboardType = KeyboardType.Phone, horizontalPadding = 12.dp, focusRing = false,
                    leading = {
                        Box(
                            Modifier.fillMaxHeight().drawBehind {
                                val x = size.width - 0.5.dp.toPx()
                                drawLine(c.line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                            }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("+251", style = EirsType.mono(14.sp).copy(color = c.muted)) }
                    },
                )
            }
            SignUpField("Password", password, { edit(it) { v -> password = v } }, "8+ characters", keyboardType = KeyboardType.Password, secret = true)
            SignUpField("Confirm password", confirm, { edit(it) { v -> confirm = v } }, "Repeat password", keyboardType = KeyboardType.Password, secret = true, last = true)
            Row(
                Modifier.fillMaxWidth().plainClickable { terms = !terms }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EirsCheckbox(terms)
                Text(
                    buildAnnotatedString {
                        append("I agree to the ")
                        withStyle(SpanStyle(color = c.brand)) { append("Terms of Service") }
                        append(" and ")
                        withStyle(SpanStyle(color = c.brand)) { append("Privacy Policy") }
                    },
                    style = EirsType.sans(14.sp, lineHeight = 1.4).copy(color = c.muted),
                )
            }
            state.error?.let { ErrorText(it) }
            PrimaryButton(
                "Create account",
                { vm.submit(SignUpRequest(first, last, email, phone, password), confirm) },
                enabled = terms,
            )
            PromptLink("Already registered?", "Sign in", onSignIn)
        }
    }
}

@Composable
private fun SignUpField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    last: Boolean = false,
) {
    LabeledField(label, modifier, labelSize = 13.sp, gap = 7.dp) {
        EirsTextField(
            value, onValueChange, placeholder = placeholder, height = 50.dp, horizontalPadding = 12.dp,
            keyboardType = keyboardType, imeAction = if (last) ImeAction.Done else ImeAction.Next,
            visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        )
    }
}
