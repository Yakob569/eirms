package com.eirspay.mobile.feature.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType
import kotlinx.coroutines.delay

/** Teal brand splash; advances after 2.4s, tap to skip. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LightStatusBarIcons()
    val c = Eirs.colors
    val finished by rememberUpdatedState(onFinished)
    var done by remember { mutableStateOf(false) }
    val finish = { if (!done) { done = true; finished() } }
    LaunchedEffect(Unit) { delay(2400); finish() }

    Box(Modifier.fillMaxSize().background(c.brand).plainClickable { finish() }) {
        // Decorative ring outlines bleeding off the corners.
        Canvas(Modifier.fillMaxSize()) {
            val px = 1.dp.toPx()
            drawCircle(Color.White.copy(alpha = .18f), 210.dp.toPx(), Offset(size.width - 50.dp.toPx(), 90.dp.toPx()), style = Stroke(px))
            drawCircle(Color.White.copy(alpha = .18f), 130.dp.toPx(), Offset(size.width - 50.dp.toPx(), 90.dp.toPx()), style = Stroke(px))
            drawCircle(Color.White.copy(alpha = .12f), 180.dp.toPx(), Offset(60.dp.toPx(), size.height - 20.dp.toPx()), style = Stroke(px))
        }
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier.popIn().size(84.dp).clip(RoundedCornerShape(22.dp)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) { LineIcon(EirsIcons.Shield, size = 40.dp, strokeWidth = 1.8f, tint = c.brand) }
                Column(
                    Modifier.riseIn(durationMillis = 600, delayMillis = 150),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("EIRSPay", style = EirsType.serif(52.sp).copy(color = Color.White))
                    Text(
                        buildAnnotatedString {
                            append("Electronic invoicing for ")
                            withStyle(SpanStyle(fontFamily = EirsFonts.Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp)) { append("Ethiopia") }
                        },
                        style = EirsType.sans(15.sp).copy(color = Color.White.copy(alpha = .85f)),
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 48.dp, end = 48.dp, bottom = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FillBar(Modifier.width(140.dp), durationMillis = 2000, delayMillis = 200, track = Color.White.copy(alpha = .25f), fill = Color.White)
                Eyebrow("SECURED BY EIRS", color = Color.White.copy(alpha = .75f), size = 10.sp, tracking = 0.2)
            }
        }
    }
}
