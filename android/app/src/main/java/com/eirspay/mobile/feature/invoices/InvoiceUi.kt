package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.model.InvoiceStatus
import com.eirspay.domain.model.Party
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import kotlinx.coroutines.delay
import java.math.BigDecimal

/** Badge background/content for an invoice status (prototype `STC`). */
@Composable
internal fun statusColors(status: InvoiceStatus): Pair<Color, Color> {
    val c = Eirs.colors
    return when (status) {
        InvoiceStatus.ISSUED -> c.tint1 to c.brandDark
        InvoiceStatus.PAID -> c.paidTint to c.paidInk
        InvoiceStatus.WITHHELD -> c.warnTint to c.warnInk
        InvoiceStatus.CANCELED -> c.redTint to c.red
    }
}

/** "TIN …", "NID 123…" or "Walk-in" — the prototype's partyId. */
internal fun partyIdLabel(p: Party): String = when {
    p.tin != null -> "TIN ${p.tin}"
    p.idType != null -> "${p.idType!!.name} ${p.idNumber ?: "—"}"
    else -> "Walk-in"
}

internal fun qtyText(q: BigDecimal): String = q.stripTrailingZeros().toPlainString()

/**
 * Remembers which key was copied last and clears it after 1.4s (the "Copied" feedback).
 * Returns the state plus a copy function that also writes the text to the clipboard.
 */
@Composable
internal fun rememberCopyFeedback(): Pair<MutableState<String?>, (key: String, text: String) -> Unit> {
    val clipboard = LocalClipboardManager.current
    val copied = remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(tick) {
        if (copied.value != null) {
            delay(1400)
            copied.value = null
        }
    }
    val copy: (String, String) -> Unit = { key, text ->
        clipboard.setText(AnnotatedString(text))
        copied.value = key
        tick++
    }
    return copied to copy
}

/** Small "Copy" chip used on list cards. */
@Composable
internal fun CopyChip(copied: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(7.dp)
    val fg = if (copied) c.brandDark else c.ink
    Row(
        modifier.height(26.dp).clip(shape).background(if (copied) c.tint1 else Color.White).border(1.dp, c.line, shape)
            .plainClickable(onClick = onClick).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        LineIcon(EirsIcons.Copy, size = 11.dp, tint = fg)
        Text(if (copied) "Copied" else "Copy", style = EirsType.sans(11.sp, FontWeight.SemiBold).copy(color = fg), maxLines = 1)
    }
}
