package com.eirspay.mobile.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

// ---------------------------------------------------------------------------------------------
// Chip multi-select — the ONE chip picker used by every settings page (branch rules, source
// system tax types, API key capabilities, managers, pickers). Single-select pickers use it with a
// one-item selection.
// ---------------------------------------------------------------------------------------------

enum class ChipSize { Regular, Small }

/**
 * One chip of [ChipMultiSelect]. Selected: tinted with a brand border. Not allowed: greyed to 45%
 * and not tappable. Derived (e.g. EXCISE from the merchant's excise status): dashed border, read-only.
 */
@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    derived: Boolean = false,
    mono: Boolean = true,
    size: ChipSize = ChipSize.Regular,
) {
    val c = Eirs.colors
    val small = size == ChipSize.Small
    val shape = RoundedCornerShape(999.dp)
    val bg = if (selected) c.tint1 else Color.White
    val fg = if (selected) c.brandDark else c.muted
    val bd = if (selected) c.brand else c.line
    val fontSize = when {
        small -> 10.sp
        mono -> 12.sp
        else -> 13.sp
    }
    val textStyle = (if (mono) EirsType.mono(fontSize) else EirsType.sans(fontSize)).copy(color = fg)
    val clickable = enabled && !derived && onClick != null
    Box(
        modifier
            .then(if (small) Modifier else Modifier.height(34.dp))
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(bg)
            .then(if (derived) Modifier.dashedBorder(bd, 999.dp) else Modifier.border(1.dp, bd, shape))
            .then(if (clickable) Modifier.plainClickable(onClick = onClick!!) else Modifier)
            .padding(horizontal = if (small) 7.dp else 12.dp, vertical = if (small) 3.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = textStyle, maxLines = 1) }
}

/**
 * Wrapping chip multi-select.
 *
 * @param selected values currently on. A value that isn't [allowed] never shows as selected.
 * @param onToggle null for a read-only chip list.
 * @param allowed false greys the chip out (outside what the branch / source system allows).
 * @param derived dashed, read-only chips that are on because of a merchant-wide rule.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipMultiSelect(
    options: List<T>,
    selected: Collection<T>,
    onToggle: ((T) -> Unit)?,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    allowed: (T) -> Boolean = { true },
    derived: (T) -> Boolean = { false },
    mono: Boolean = true,
    size: ChipSize = ChipSize.Regular,
    spacing: Dp = if (size == ChipSize.Small) 5.dp else 6.dp,
) {
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing), verticalArrangement = Arrangement.spacedBy(spacing)) {
        options.forEach { o ->
            val ok = allowed(o)
            val isDerived = derived(o)
            SelectChip(
                label = label(o),
                selected = isDerived || (o in selected && ok),
                onClick = onToggle?.let { toggle -> { toggle(o) } },
                enabled = ok,
                derived = isDerived,
                mono = mono,
                size = size,
            )
        }
    }
}

/** A titled [ChipMultiSelect] with an "n of m" count and an optional caption underneath. */
@Composable
fun <T> ChipGroup(
    title: String,
    options: List<T>,
    selected: Collection<T>,
    onToggle: ((T) -> Unit)?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    label: (T) -> String = { it.toString() },
    allowed: (T) -> Boolean = { true },
    derived: (T) -> Boolean = { false },
    mono: Boolean = true,
) {
    val c = Eirs.colors
    val on = options.count { derived(it) || (it in selected && allowed(it)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.ink), modifier = Modifier.weight(1f, fill = false))
            Text("$on of ${options.size}", style = EirsType.mono(11.sp).copy(color = c.muted))
        }
        ChipMultiSelect(options, selected, onToggle, label = label, allowed = allowed, derived = derived, mono = mono)
        if (!caption.isNullOrEmpty()) Text(caption, style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = c.muted))
    }
}

// ---------------------------------------------------------------------------------------------
// Status pill — the ONE status badge for settings (Active / Inactive, Verified / Failed /
// Unverified, Deactivated, Main branch, This device, Enabled …).
// ---------------------------------------------------------------------------------------------

enum class PillTone { Positive, Warning, Danger, Strong, Neutral }

fun pillTone(label: String): PillTone = when (label) {
    "Active", "Verified", "This device", "Enabled", "Email verified" -> PillTone.Positive
    "Unverified" -> PillTone.Warning
    "Failed" -> PillTone.Danger
    "Main branch", "OWNER", "ADMIN", "CASHIER" -> PillTone.Strong
    else -> PillTone.Neutral
}

@Composable
fun StatusPill(
    label: String,
    modifier: Modifier = Modifier,
    tone: PillTone = pillTone(label),
    small: Boolean = false,
    background: Color? = null,
) {
    val c = Eirs.colors
    val (bg, fg) = when (tone) {
        PillTone.Positive -> c.tint1 to c.brandDark
        PillTone.Warning -> c.warnTint to c.warnInk
        PillTone.Danger -> c.redTint to c.red
        PillTone.Strong -> c.ink to Color.White
        PillTone.Neutral -> c.neutralChip to c.muted
    }
    Badge(
        label, background ?: bg, fg, modifier = modifier, radius = 999.dp, fontSize = if (small) 10.sp else 11.sp,
        tracking = 0.0, horizontal = if (small) 7.dp else 8.dp, vertical = if (small) 2.dp else 3.dp,
    )
}
