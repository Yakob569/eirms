package com.eirspay.mobile.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.VerificationChannel
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import kotlinx.coroutines.delay

@Composable
fun VerifyMethodScreen(onBack: () -> Unit, onCodeSent: (channel: String, sentTo: String) -> Unit) {
    val vm: VerifyMethodViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val sent by rememberUpdatedState(onCodeSent)
    LaunchedEffect(vm) { vm.events.collect { sent(it.channel.name, it.sentTo) } }

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, step = 2, spacing = 22.dp) {
            ConfirmBanner("Account created for ${vm.accountName}")
            AuthHeading("Verify your account", lead = "Choose where you want to receive your verification code.")
            MethodOption(EirsIcons.Smartphone, "Phone (SMS)", vm.maskedPhone, badge = "Faster") { vm.send(VerificationChannel.SMS) }
            MethodOption(EirsIcons.Mail, "Email", vm.maskedEmail) { vm.send(VerificationChannel.EMAIL) }
            state.error?.let { ErrorText(it) }
        }
    }
}

@Composable
private fun MethodOption(icon: LineIconSpec, title: String, detail: String, badge: String? = null, onClick: () -> Unit) {
    val c = Eirs.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val border by animateColorAsState(if (pressed) c.brand else c.line, label = "methodBorder")
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (pressed) c.background else Color.White).border(1.dp, border, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(c.tint1), contentAlignment = Alignment.Center) {
            LineIcon(icon, size = 20.dp, tint = c.brand)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = EirsType.sans(16.sp, FontWeight.SemiBold))
                if (badge != null) Badge(badge, c.tint1, c.brandDark, radius = 999.dp, fontSize = 11.sp, tracking = 0.0, vertical = 2.dp)
            }
            MonoText(detail, size = 13.sp, color = c.muted)
        }
        LineIcon(EirsIcons.ArrowRight, size = 18.dp, tint = c.muted)
    }
}

@Composable
fun VerifyCodeScreen(
    channel: String,
    sentTo: String,
    onBack: () -> Unit,
    onUseDifferentMethod: () -> Unit,
    onVerified: () -> Unit,
) {
    val vm: VerifyCodeViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val verified by rememberUpdatedState(onVerified)
    LaunchedEffect(vm) { vm.events.collect { verified() } }

    val c = Eirs.colors
    var code by rememberSaveable { mutableStateOf("") }
    var secs by rememberSaveable { mutableIntStateOf(882) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); if (secs > 0) secs -= 1 }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    LoadingHost(state.loading) {
        AuthScaffold(onBack = onBack, step = 2, spacing = 22.dp) {
            AuthHeading(
                "Enter the ", "6-digit", trailing = " code",
                leadContent = buildAnnotatedString {
                    append("Sent to ")
                    withStyle(SpanStyle(fontFamily = EirsFonts.Mono, color = c.ink)) { append(sentTo) }
                },
            )
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { i ->
                        val v = code.getOrNull(i)?.toString() ?: ""
                        val active = i == minOf(code.length, 5) && code.length < 6
                        CodeCell(v, active, Modifier.weight(1f))
                    }
                }
                BasicTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isDigit).take(6); vm.clearError() },
                    modifier = Modifier.matchParentSize().alpha(0f).focusRequester(focus),
                    singleLine = true,
                    textStyle = EirsType.mono(16.sp).copy(color = Color.Transparent),
                    cursorBrush = SolidColor(Color.Transparent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        append("Expires in ")
                        withStyle(SpanStyle(fontFamily = EirsFonts.Mono, color = c.ink)) { append("${secs / 60}:${(secs % 60).toString().padStart(2, '0')}") }
                    },
                    style = EirsType.sans(14.sp).copy(color = c.muted), maxLines = 1,
                )
                TextLink("Resend code", { secs = 900; code = ""; vm.resend(channel) })
            }
            state.error?.let { ErrorText(it) }
            PrimaryButton("Verify", { vm.verify(code) }, enabled = code.length == 6)
            MutedAction("Use a different method", onUseDifferentMethod)
        }
    }
}

@Composable
private fun CodeCell(value: String, active: Boolean, modifier: Modifier) {
    val c = Eirs.colors
    val border by animateColorAsState(if (active) c.brand else if (value.isNotEmpty()) c.ink else c.line, label = "cellBorder")
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(58.dp)
            .drawBehind {
                if (active) {
                    val ring = 4.dp.toPx()
                    drawRoundRect(
                        c.tint1, topLeft = Offset(-ring / 2, -ring / 2), size = Size(size.width + ring, size.height + ring),
                        cornerRadius = CornerRadius(12.dp.toPx() + ring / 2), style = Stroke(ring),
                    )
                }
            }
            .clip(shape).background(if (value.isNotEmpty()) Color.White else c.background).border(1.5.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) { Text(value, style = EirsType.mono(24.sp, FontWeight.Medium).copy(color = c.ink)) }
}

@Composable
fun VerifySuccessScreen(sentTo: String, onBack: () -> Unit, onContinue: () -> Unit) {
    val vm: VerifySuccessViewModel = hiltViewModel()
    AuthScaffold(onBack = onBack, step = 3, spacing = 0.dp) {
        SuccessLayout(
            icon = EirsIcons.Check, plain = "You're ", accent = "verified.", titleSize = 46.sp, iconSize = 34.dp,
            body = { Text("Your merchant account is ready. Add your business details next to start issuing EIRS-registered receipts.") },
            card = { InfoRows(listOf("Account" to vm.accountName, "Verified via" to sentTo), monoLast = true) },
        ) {
            PrimaryButton("Go to dashboard", onContinue)
        }
    }
}
