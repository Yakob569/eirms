package com.eirspay.mobile.ui.designsystem

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/** CSS `m-up`: fade in while rising 12dp. Runs once when the element enters composition. */
@Composable
fun Modifier.riseIn(durationMillis: Int = 400, delayMillis: Int = 0, distance: Dp = 12.dp): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(durationMillis, delayMillis, FastOutSlowInEasing)) }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * distance.toPx()
    }
}

/** CSS `m-pop`: scale from 0.6 with a slight overshoot. */
@Composable
fun Modifier.popIn(durationMillis: Int = 600): Modifier {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durationMillis, easing = CubicBezierEasing(.2f, .9f, .3f, 1.2f))) }
    return graphicsLayer { val s = 0.6f + 0.4f * p.value; scaleX = s; scaleY = s; alpha = p.value.coerceIn(0f, 1f) }
}

/** Progress bar that fills left→right once over [durationMillis] (splash + loader). */
@Composable
fun FillBar(
    modifier: Modifier = Modifier,
    durationMillis: Int = 2000,
    delayMillis: Int = 0,
    track: Color,
    fill: Color,
    height: Dp = 3.dp,
) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(durationMillis, delayMillis, CubicBezierEasing(.4f, 0f, .2f, 1f))) }
    Box(modifier.height(height).clip(RoundedCornerShape(height)).background(track)) {
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = p.value; transformOrigin = TransformOrigin(0f, 0.5f) }.background(fill))
    }
}

/** Expanding, fading ring behind success marks (CSS `m-ring`). */
@Composable
fun PulseRing(modifier: Modifier = Modifier, color: Color, shapeRadius: Dp? = null, filled: Boolean = true, iterations: Int = 2, delayMillis: Int = 0, durationMillis: Int = 1600) {
    val t = rememberInfiniteTransition(label = "ring")
    val finite = remember { Animatable(0f) }
    if (iterations > 0) LaunchedEffect(Unit) { repeat(iterations) { finite.snapTo(0f); finite.animateTo(1f, tween(durationMillis, easing = LinearOutSlowInEasing)) } }
    val infinite by t.animateFloat(0f, 1f, infiniteRepeatable(tween(durationMillis, delayMillis, LinearOutSlowInEasing)), label = "ringP")
    val p = if (iterations > 0) finite.value else infinite
    val shape = shapeRadius?.let { RoundedCornerShape(it) } ?: CircleShape
    Box(
        modifier.graphicsLayer { val s = 0.8f + 1.3f * p; scaleX = s; scaleY = s; alpha = if (iterations > 0 && p >= 1f) 0f else 0.7f * (1f - p) }
            .then(if (filled) Modifier.background(color, shape) else Modifier.border(1.5.dp, color, shape)),
    )
}

/** Brand success mark: filled circle, white icon, two pulses. */
@Composable
fun SuccessMark(icon: LineIconSpec = EirsIcons.Check, modifier: Modifier = Modifier, size: Dp = 76.dp, iconSize: Dp = 34.dp) {
    val c = Eirs.colors
    Box(modifier.size(size)) {
        PulseRing(Modifier.matchParentSize(), c.brand)
        Box(Modifier.matchParentSize().clip(CircleShape).background(c.brand), contentAlignment = Alignment.Center) {
            LineIcon(icon, size = iconSize, strokeWidth = 2.4f, tint = Color.White)
        }
    }
}

data class LoaderMessage(val title: String, val subtitle: String)

/**
 * Wraps a screen and, while [loading] is set, shows the translucent brand loader over it:
 * the page stays visible behind a 55% white, blurred layer (API 31+ blurs; older devices just fade).
 */
@Composable
fun LoadingHost(loading: LoaderMessage?, modifier: Modifier = Modifier, durationMillis: Int = 1800, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize()) {
        val blur by animateDpAsState(if (loading != null) 6.dp else 0.dp, label = "blur")
        Box(Modifier.fillMaxSize().then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)) { content() }
        AnimatedVisibility(loading != null, enter = fadeIn(tween(250)) + slideInVertically { it / 40 }, exit = fadeOut(tween(200))) {
            val msg = loading ?: remember { LoaderMessage("", "") }
            BrandLoader(msg, durationMillis)
        }
    }
}

@Composable
private fun BrandLoader(msg: LoaderMessage, durationMillis: Int) {
    val c = Eirs.colors
    BackHandler(enabled = true) { /* hold the user on the loader until the work finishes */ }
    Column(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.55f)).plainClickable { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(76.dp)) {
            PulseRing(Modifier.matchParentSize(), c.brand, shapeRadius = 20.dp, filled = false, iterations = 0, durationMillis = 1400)
            PulseRing(Modifier.matchParentSize(), c.brand, shapeRadius = 20.dp, filled = false, iterations = 0, delayMillis = 700, durationMillis = 1400)
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp)).background(c.brand), contentAlignment = Alignment.Center) {
                LineIcon(EirsIcons.Shield, size = 34.dp, strokeWidth = 1.8f, tint = Color.White)
            }
        }
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(msg.title, textAlign = TextAlign.Center, style = EirsType.serif(34.sp).copy(fontStyle = FontStyle.Italic, color = c.ink))
            Text(msg.subtitle, textAlign = TextAlign.Center, style = EirsType.sans(14.sp).copy(color = c.muted))
        }
        key(msg) { FillBar(Modifier.width(200.dp), durationMillis = durationMillis, track = c.tint2, fill = c.brand) }
    }
}

/**
 * Bottom sheet matching the design: ink scrim, white sheet with 26dp top corners and a grab handle,
 * slides up (CSS `m-sheet`). Back press and scrim tap dismiss it.
 */
@Composable
fun EirsBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Eirs.colors
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(c.scrim).plainClickable(onClick = onDismiss))
        }
        AnimatedVisibility(
            visible, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(280, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) { it },
            exit = slideOutVertically(tween(220)) { it },
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White)
                    .plainClickable { }.navigationBarsPadding().imePadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp).size(40.dp, 5.dp).clip(RoundedCornerShape(5.dp)).background(c.line))
                content()
            }
        }
    }
}
