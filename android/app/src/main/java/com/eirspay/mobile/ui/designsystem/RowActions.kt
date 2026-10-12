package com.eirspay.mobile.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/**
 * ⋮ button on list cards. Its 36dp touch area overhangs the card's header row (CSS negative
 * margins) so the row keeps its height; it consumes its own tap so the card doesn't open.
 */
@Composable
fun KebabButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    Box(
        modifier
            .layout { measurable, _ ->
                val side = 36.dp.roundToPx()
                val p = measurable.measure(Constraints.fixed(side, side))
                val w = side - 10.dp.roundToPx()
                val h = 20.dp.roundToPx()
                layout(w, h) { p.place(0, (h - side) / 2) }
            }
            .pressBackground(Color.Transparent, c.tint1, RoundedCornerShape(10.dp), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val r = size.minDimension * 1.8f / 24f
            listOf(5f, 12f, 19f).forEach { y -> drawCircle(c.muted, r, Offset(size.width / 2, size.height * y / 24f)) }
        }
    }
}

/** One option in a row-actions sheet. */
data class SheetAction(
    val label: String,
    val icon: LineIconSpec,
    val onClick: () -> Unit,
    val danger: Boolean = false,
    /** Draws a hairline above this option (separates destructive actions). */
    val separated: Boolean = false,
)

/** Body of the row-actions bottom sheet: mono eyebrow, serif document number, one line of context, options. */
@Composable
fun ColumnScope.ActionSheetContent(eyebrow: String, title: String, subtitle: String, actions: List<SheetAction>) {
    val c = Eirs.colors
    Column(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Eyebrow(eyebrow, size = 10.sp, tracking = 0.12)
        Text(title, style = EirsType.serif(28.sp).copy(color = c.ink), maxLines = 1)
        Text(subtitle, style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    actions.forEach { a ->
        if (a.separated) Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).height(1.dp).background(c.line))
        val fg = if (a.danger) c.red else c.ink
        Row(
            Modifier.fillMaxWidth().height(52.dp).pressBackground(Color.White, c.background, RoundedCornerShape(12.dp), onClick = a.onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LineIcon(a.icon, size = 18.dp, strokeWidth = 1.9f, tint = fg)
            Text(a.label, modifier = Modifier.weight(1f), style = EirsType.sans(15.sp, FontWeight.Medium).copy(color = fg), maxLines = 1)
            LineIcon(EirsIcons.ChevronRight, size = 15.dp, tint = c.muted)
        }
    }
}
