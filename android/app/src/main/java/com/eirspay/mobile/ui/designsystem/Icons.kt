package com.eirspay.mobile.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eirspay.mobile.ui.theme.Eirs

/** A 24×24 stroke icon (Lucide style, as used throughout the design) defined by SVG path data. */
@Immutable
class LineIconSpec(vararg val paths: String)

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"
private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
    "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}a$rx $rx 0 0 1 ${-rx} ${rx}h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}Z"

object EirsIcons {
    val Shield = LineIconSpec(
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        "m9 12 2 2 4-4",
    )
    val ArrowRight = LineIconSpec("M5 12h14M13 6l6 6-6 6")
    val ArrowLeft = LineIconSpec("M19 12H5M11 18l-6-6 6-6")
    val Globe = LineIconSpec(circle(12f, 12f, 10f), "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20", "M2 12h20")
    val Check = LineIconSpec("M20 6 9 17l-5-5")
    val CheckCircle = LineIconSpec(circle(12f, 12f, 10f), "m9 12 2 2 4-4")
    val Smartphone = LineIconSpec(rect(5f, 2f, 14f, 20f, 2f), "M12 18h.01")
    val Mail = LineIconSpec(rect(2f, 4f, 20f, 16f, 2f), "m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7")
    val Eye = LineIconSpec("M2.06 12.35a1 1 0 0 1 0-.7 10.75 10.75 0 0 1 19.88 0 1 1 0 0 1 0 .7 10.75 10.75 0 0 1-19.88 0", circle(12f, 12f, 3f))
    val Lock = LineIconSpec(rect(3f, 11f, 18f, 11f, 2f), "M7 11V7a5 5 0 0 1 10 0v4")
    val Building = LineIconSpec("M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18ZM6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2M10 6h4M10 10h4M10 14h4M10 18h4")
    val ChevronDown = LineIconSpec("m6 9 6 6 6-6")
    val ChevronRight = LineIconSpec("m9 18 6-6-6-6")
    val Search = LineIconSpec(circle(11f, 11f, 8f), "m21 21-4.3-4.3")
    val Copy = LineIconSpec(rect(8f, 8f, 14f, 14f, 2f), "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2")
    val Ban = LineIconSpec(circle(12f, 12f, 10f), "m4.9 4.9 14.2 14.2")
    val Download = LineIconSpec("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3")
    val Close = LineIconSpec("M18 6 6 18M6 6l12 12")
    val Plus = LineIconSpec("M12 5v14M5 12h14")
    val Monitor = LineIconSpec(rect(2f, 3f, 20f, 14f, 2f), "M8 21h8M12 17v4")
    val User = LineIconSpec(circle(12f, 8f, 4f), "M20 21a8 8 0 0 0-16 0")
    val LogOut = LineIconSpec("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9")
    val FilePlus = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M12 11v6M9 14h6")
    val Receipt = LineIconSpec("M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1ZM8 8h8M8 12h8M8 16h5")
    val Grid = LineIconSpec("M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z")
    val FileText = LineIconSpec("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7ZM14 2v4a2 2 0 0 0 2 2h4M16 13H8M16 17H8M10 9H8")
    val Sliders = LineIconSpec("M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4")
    val Package = LineIconSpec("m7.5 4.27 9 5.15M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16ZM3.3 7l8.7 5 8.7-5M12 22V12")
    val Percent = LineIconSpec("M19 5 5 19M9 6.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0M20 17.5a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0")
}

@Composable
fun LineIcon(
    icon: LineIconSpec,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    strokeWidth: Float = 2f,
    tint: Color = Eirs.colors.ink,
) {
    val paths = remember(icon) { icon.paths.map { addPathNodes(it).toPath() } }
    Canvas(modifier.size(size)) {
        val k = this.size.minDimension / 24f
        scale(k, k, pivot = Offset.Zero) {
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { drawPath(it, tint, style = stroke) }
        }
    }
}
