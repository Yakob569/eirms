package com.eirspay.mobile.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.model.SalesPoint
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import kotlin.math.roundToInt

/**
 * The dashboard's "Sales & tax" plot: three grid lines, a brand area + line for sales and a dashed
 * ink line for tax (drawn ×4 so it reads on the same scale, as in the design). Tap or drag to scrub;
 * [onScrub] receives the nearest point index.
 */
@Composable
internal fun SalesChart(points: List<SalesPoint>, highlighted: Int?, onScrub: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    val scrub by rememberUpdatedState(onScrub)
    val n = points.size
    Canvas(
        modifier.fillMaxWidth().height(150.dp).pointerInput(n) {
            if (n == 0) return@pointerInput
            fun index(x: Float) = ((x / size.width) * (n - 1)).roundToInt().coerceIn(0, n - 1)
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                scrub(index(down.position.x))
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    change.consume()
                    scrub(index(change.position.x))
                }
            }
        },
    ) {
        val w = size.width
        val h = size.height
        val hair = 1.dp.toPx()
        // Grid: top, middle, bottom.
        listOf(hair / 2, h / 2, h - hair / 2).forEach { y -> drawLine(c.line, Offset(0f, y), Offset(w, y), hair) }
        if (n < 2) return@Canvas
        val max = (points.maxOf { it.sales } * 1.12).takeIf { it > 0 } ?: 1.0
        fun x(i: Int) = i.toFloat() / (n - 1) * w
        fun y(v: Double) = (h - (v / max) * h).toFloat()
        val sales = points.mapIndexed { i, p -> Offset(x(i), y(p.sales)) }
        val tax = points.mapIndexed { i, p -> Offset(x(i), (h - (p.tax / max) * h * 4).toFloat()) }
        fun path(pts: List<Offset>) = Path().apply { pts.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }

        val area = path(sales).apply { lineTo(w, h); lineTo(0f, h); close() }
        drawPath(area, Brush.verticalGradient(listOf(c.brand.copy(alpha = 0.28f), c.brand.copy(alpha = 0f)), startY = 0f, endY = h))
        drawPath(path(sales), c.brand, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
        drawPath(
            path(tax), c.ink,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )

        highlighted?.takeIf { it in 0 until n }?.let { i ->
            val p = sales[i]
            drawLine(c.ink.copy(alpha = 0.25f), Offset(p.x, 0f), Offset(p.x, h), hair)
            drawCircle(Color.White, radius = 6.dp.toPx(), center = p)
            drawCircle(c.brand, radius = 4.5.dp.toPx(), center = p, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

/** X-axis labels at 0, ⅓, ⅔ and the last point; the first is left-aligned, the last right-aligned. */
@Composable
internal fun ChartTicks(points: List<SalesPoint>, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    val n = points.size
    val idx = remember(n) {
        if (n == 0) emptyList() else listOf(0, ((n - 1) / 3.0).roundToInt(), (2 * (n - 1) / 3.0).roundToInt(), n - 1)
    }
    Layout(
        content = { idx.forEach { Text(points[it].label, style = EirsType.mono(10.sp).copy(color = c.muted), maxLines = 1, softWrap = false) } },
        modifier = modifier.fillMaxWidth().height(14.dp),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        layout(width, constraints.maxHeight) {
            placeables.forEachIndexed { j, p ->
                val frac = if (n > 1) idx[j].toFloat() / (n - 1) else 0f
                val left = frac * width
                val x = when (j) { 0 -> left; placeables.lastIndex -> left - p.width; else -> left - p.width / 2f }
                p.place(x.roundToInt(), 0)
            }
        }
    }
}
