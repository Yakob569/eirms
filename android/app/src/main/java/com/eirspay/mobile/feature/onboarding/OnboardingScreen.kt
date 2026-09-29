package com.eirspay.mobile.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val preferences: PreferencesRepository) : ViewModel() {
    fun complete() = preferences.completeOnboarding()
}

private val ShadowInk = Color(0x660B2A33)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onCreateAccount: () -> Unit, onSignIn: () -> Unit) {
    val vm: OnboardingViewModel = hiltViewModel()
    val c = Eirs.colors
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val toSignup = { vm.complete(); onCreateAccount() }
    val toSignin = { vm.complete(); onSignIn() }
    val slide = pager.currentPage

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                OnboardingPanel(page)
            }
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        val w by animateDpAsState(if (i == slide) 28.dp else 6.dp, tween(300), label = "dotW")
                        val bg by animateColorAsState(if (i == slide) c.brand else c.tint2, tween(300), label = "dotBg")
                        Box(
                            Modifier.width(w).height(6.dp).clip(RoundedCornerShape(6.dp)).background(bg)
                                .plainClickable { scope.launch { pager.animateScrollToPage(i) } },
                        )
                    }
                }
                PrimaryButton(if (slide < 2) "Next" else "Create account", {
                    if (slide < 2) scope.launch { pager.animateScrollToPage(slide + 1) } else toSignup()
                })
                PromptLink("Already have an account?", "Sign in", toSignin)
            }
        }
        // Brand mark + Skip, floating over the illustration.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(c.brand), contentAlignment = Alignment.Center) {
                    LineIcon(EirsIcons.Shield, size = 14.dp, strokeWidth = 2.2f, tint = Color.White)
                }
                Text("EIRSPay", style = EirsType.sans(15.sp, FontWeight.SemiBold))
            }
            Text(
                "Skip",
                modifier = Modifier.plainClickable(onClick = toSignup).padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.muted),
            )
        }
    }
}

private data class PanelCopy(val eyebrow: String, val plain: String, val accent: String, val body: String)

private val Panels = listOf(
    PanelCopy("01 · EIRS-REGISTERED", "Every receipt, ", "compliant.", "Every invoice is submitted to the Ministry of Revenues in real time and returns with a signed IRN."),
    PanelCopy("02 · ETHIOPIAN PHONE FLOWS", "Verify with ", "one code.", "OTP over SMS to +251 numbers, out of the box. Email works too."),
    PanelCopy("03 · QR ON EVERY RECEIPT", "Customers check it ", "in one scan.", "Receipts in Amharic or English, verifiable by anyone with a phone camera."),
)

@Composable
private fun OnboardingPanel(page: Int) {
    val c = Eirs.colors
    val copy = Panels[page]
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 470dp in the design; shrink on short screens so the copy stays visible.
        val blockHeight: Dp = (maxHeight - 230.dp).coerceIn(360.dp, 470.dp)
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().height(blockHeight)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).background(c.tint1),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(300.dp).alpha(.6f).clip(CircleShape).background(c.tint2))
                Box(Modifier.padding(top = 40.dp)) {
                    when (page) {
                        0 -> ReceiptIllustration()
                        1 -> OtpIllustration()
                        else -> QrIllustration()
                    }
                }
            }
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Eyebrow(copy.eyebrow, color = c.brandDark)
                SerifTitle(copy.plain, copy.accent, size = 38.sp, lineHeight = 1.02)
                Text(copy.body, style = EirsType.sans(15.sp, lineHeight = 1.5).copy(color = c.muted))
            }
        }
    }
}

@Composable
private fun ReceiptIllustration() {
    val c = Eirs.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(250.dp).shadow(30.dp, shape, ambientColor = ShadowInk, spotColor = ShadowInk).clip(shape).background(Color.White).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("RECEIPT #004218", color = c.muted, size = 10.sp, tracking = 0.1)
            Badge("Registered", c.tint1, c.brandDark, radius = 999.dp, fontSize = 11.sp, tracking = 0.0)
        }
        Text("Abebe Coffee House", style = EirsType.sans(14.sp, FontWeight.SemiBold))
        Column(
            Modifier.fillMaxWidth().drawBehind {
                drawLine(c.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
            }.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ReceiptLine("Macchiato × 2", "180.00")
            ReceiptLine("Injera combo × 1", "420.00")
            ReceiptLine("VAT 15%", "90.00", c.muted)
        }
        Row(
            Modifier.fillMaxWidth().topHairline(c.line).padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Total", style = EirsType.sans(13.sp, FontWeight.SemiBold))
            MonoText("ETB 690.00", size = 13.sp, weight = FontWeight.Medium)
        }
        MonoText(
            "IRN ETH-2026-0F4A··9C21", size = 11.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.background).padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ReceiptLine(label: String, amount: String, color: Color = Eirs.colors.ink) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = EirsType.sans(12.sp).copy(color = color))
        MonoText(amount, size = 12.sp, color = color)
    }
}

@Composable
private fun OtpIllustration() {
    val c = Eirs.colors
    Column(Modifier.width(270.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val bubble = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
        Column(
            Modifier.align(Alignment.Start).widthIn(max = 230.dp)
                .shadow(20.dp, bubble, ambientColor = ShadowInk, spotColor = ShadowInk).clip(bubble).background(Color.White)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            MonoText("SMS · EIRSPay", size = 10.sp, color = c.muted, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                buildAnnotatedString {
                    append("Your verification code is ")
                    withStyle(SpanStyle(fontFamily = EirsFonts.Mono, fontWeight = FontWeight.Bold)) { append("482 913") }
                    append(". Expires in 15 min.")
                },
                style = EirsType.sans(13.sp, lineHeight = 1.45).copy(color = c.ink),
            )
        }
        val card = RoundedCornerShape(14.dp)
        Row(
            Modifier.fillMaxWidth().shadow(20.dp, card, ambientColor = ShadowInk, spotColor = ShadowInk).clip(card).background(Color.White).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("4", "8", "2", "9", "1", "3").forEach { d ->
                Box(
                    Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(c.background)
                        .border(1.5.dp, c.brand, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) { MonoText(d, size = 18.sp) }
            }
        }
        Pill("+251 verified", background = c.brand, content = Color.White, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
private fun QrIllustration() {
    val c = Eirs.colors
    val scan = rememberInfiniteTransition(label = "scan")
    val y by scan.animateFloat(
        -4f, 118f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scanY",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val shape = RoundedCornerShape(16.dp)
        Box(Modifier.size(150.dp).shadow(30.dp, shape, ambientColor = ShadowInk, spotColor = ShadowInk).clip(shape).background(Color.White)) {
            DecorativeQr(Modifier.fillMaxSize().padding(12.dp))
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp).graphicsLayer { translationY = y.dp.toPx() - 6.dp.toPx() }
                    .height(14.dp).background(Brush.verticalGradient(listOf(Color.Transparent, c.brand.copy(alpha = .35f), Color.Transparent))),
            )
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp).graphicsLayer { translationY = y.dp.toPx() }
                    .height(2.dp).background(c.brand),
            )
        }
        Pill("Authentic · portal.mor.gov.et", background = Color.White, content = c.brandDark)
    }
}

@Composable
private fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(background).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LineIcon(EirsIcons.Check, size = 14.dp, strokeWidth = 2.6f, tint = content)
        Text(text, style = EirsType.sans(13.sp, FontWeight.SemiBold).copy(color = content), maxLines = 1)
    }
}
