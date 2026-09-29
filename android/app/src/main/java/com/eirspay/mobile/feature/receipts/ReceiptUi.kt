package com.eirspay.mobile.feature.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.InvoiceStatus
import com.eirspay.domain.model.Party
import com.eirspay.domain.model.Receipt
import com.eirspay.domain.model.ReceiptType
import com.eirspay.mobile.ui.designsystem.Eyebrow
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.topHairline
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import java.math.BigDecimal
import java.math.RoundingMode

/** Background / foreground pair for a badge. */
internal data class Tone(val bg: Color, val fg: Color)

@Composable
internal fun ReceiptType.tone(): Tone {
    val c = Eirs.colors
    return when (this) {
        ReceiptType.SALES -> Tone(c.tint1, c.brandDark)
        ReceiptType.PAYMENT -> Tone(c.neutralChip, c.ink)
        ReceiptType.ADVANCE -> Tone(c.warnTint, c.warnInk)
        ReceiptType.REFUND -> Tone(c.redTint, c.red)
    }
}

@Composable
internal fun InvoiceStatus.tone(): Tone {
    val c = Eirs.colors
    return when (this) {
        InvoiceStatus.ISSUED -> Tone(c.tint1, c.brandDark)
        InvoiceStatus.PAID -> Tone(c.paidTint, c.paidInk)
        InvoiceStatus.WITHHELD -> Tone(c.warnTint, c.warnInk)
        InvoiceStatus.CANCELED -> Tone(c.redTint, c.red)
    }
}

internal val Receipt.displayNo: String get() = "RCP-$number"
internal val Invoice.displayNo: String get() = "INV-$number"

/** "1", "2.5" — quantities without trailing zeros. */
internal fun BigDecimal.qtyText(): String = stripTrailingZeros().toPlainString()

/** 0.02 → "2%". */
internal fun BigDecimal.percentText(): String = movePointRight(2).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%"

/** Mono section label inside cards ("LINKED INVOICE", "DOCUMENT"). */
@Composable
internal fun CardEyebrow(text: String, modifier: Modifier = Modifier) {
    Eyebrow(text, modifier = modifier, color = Eirs.colors.muted, size = 10.sp, tracking = 0.12)
}

/** Brand chip that opens a linked document ("INV-100 ↗"); consumes its own click. */
@Composable
internal fun LinkChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(6.dp)
    Text(
        "$text ↗", maxLines = 1,
        modifier = modifier.clip(shape).background(c.tint1).border(1.dp, c.tint2, shape).plainClickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = EirsType.mono(11.sp).copy(color = c.brand),
    )
}

/** k/v row with a hairline on top; value in mono 12 (CSS rows inside party/document cards). */
@Composable
internal fun KvRow(key: String, value: String, modifier: Modifier = Modifier, keyWeight: Boolean = false) {
    val c = Eirs.colors
    Row(
        modifier.fillMaxWidth().topHairline(c.line).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top,
    ) {
        Text(key, style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1)
        Text(value, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = EirsType.mono(12.sp).copy(color = c.ink))
    }
}

/** Two texts on one line: left one ellipsizes, right one keeps its width. */
@Composable
internal fun SplitRow(modifier: Modifier = Modifier, left: @Composable RowScope.() -> Unit, right: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        left(); right()
    }
}

/** Buyer rows for "RECEIVED FROM" (without the name row). */
internal fun Party.detailRows(): List<Pair<String, String>> = when {
    tin != null -> listOf("TIN" to tin!!, "VAT number" to (vatNumber ?: "—"), "Address" to (address ?: "—"))
    phone != null -> listOf("Phone" to phone!!)
    idType != null -> listOf("ID" to "${idType!!.name} ${idNumber.orEmpty()}".trim())
    else -> listOf("Type" to "Walk-in, no details")
}

internal val ellipsis = TextOverflow.Ellipsis
