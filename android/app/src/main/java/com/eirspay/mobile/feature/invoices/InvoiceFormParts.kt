package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.model.InvoicePaymentMode
import com.eirspay.domain.model.NatureOfSupply
import com.eirspay.domain.model.PaymentTerm
import com.eirspay.domain.model.TaxCode
import com.eirspay.mobile.ui.designsystem.EirsChip
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.FormField
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.PrimaryButton
import com.eirspay.mobile.ui.designsystem.SegmentOption
import com.eirspay.mobile.ui.designsystem.SegmentedControl
import com.eirspay.mobile.ui.designsystem.dashedBorder
import com.eirspay.mobile.ui.designsystem.decimalOnly
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.topHairline
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

// Form pieces shared by "Register invoice" and the interim / final invoice page, so the two item
// builders and payment pickers can't drift apart.

private val NatureOptions = listOf(SegmentOption(NatureOfSupply.GOODS, "Goods"), SegmentOption(NatureOfSupply.SERVICE, "Service"))

/**
 * One editable line item: description, quantity × unit price, tax code and the line total.
 * [detailed] also shows item code, unit and nature of supply (the full MoR item fields).
 *
 * @param onRemove null hides the remove control (e.g. the last line of a form that needs one).
 */
@Composable
internal fun LineDraftEditor(
    line: LineDraft,
    onChange: (LineDraft) -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    detailed: Boolean = false,
    number: Int? = null,
) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).border(1.dp, c.line, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (detailed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MonoText("LINE ${number ?: ""}".trim(), size = 11.sp, color = c.muted)
                if (onRemove != null) {
                    Text(
                        "Remove", modifier = Modifier.plainClickable(onClick = onRemove).padding(vertical = 4.dp),
                        style = EirsType.sans(12.sp, FontWeight.SemiBold).copy(color = c.red),
                    )
                }
            }
            FormField(line.description, { onChange(line.copy(description = it)) }, placeholder = "Description")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    line.itemCode.orEmpty(), { onChange(line.copy(itemCode = it.uppercase().ifBlank { null })) },
                    modifier = Modifier.weight(1.3f), placeholder = "Item code", mono = true, fontSize = 14.sp, height = 42.dp,
                )
                FormField(
                    line.unit, { onChange(line.copy(unit = it.uppercase())) }, modifier = Modifier.weight(1f),
                    placeholder = "Unit", mono = true, fontSize = 14.sp, height = 42.dp,
                )
            }
            SegmentedControl(NatureOptions, line.nature, { onChange(line.copy(nature = it)) }, height = 34.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(line.description, { onChange(line.copy(description = it)) }, placeholder = "Item or service", modifier = Modifier.weight(1f))
                if (onRemove != null) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).plainClickable(onClick = onRemove),
                        contentAlignment = Alignment.Center,
                    ) { LineIcon(EirsIcons.Close, size = 15.dp, tint = c.muted) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(
                line.quantity, { onChange(line.copy(quantity = it.decimalOnly())) }, modifier = Modifier.width(if (detailed) 96.dp else 72.dp),
                placeholder = "Qty", mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal, fontSize = 15.sp,
            )
            FormField(
                line.price, { onChange(line.copy(price = it.decimalOnly())) }, modifier = Modifier.weight(1f), placeholder = "Unit price",
                mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal, fontSize = 15.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(c.background)
                    .border(1.dp, c.line, RoundedCornerShape(10.dp)).padding(2.dp),
            ) {
                TaxCode.entries.forEach { code ->
                    val on = code == line.taxCode
                    Box(
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(7.dp)).background(if (on) c.ink else Color.Transparent)
                            .plainClickable { onChange(line.copy(taxCode = code)) },
                        contentAlignment = Alignment.Center,
                    ) { MonoText(code.name, size = 11.sp, color = if (on) Color.White else c.muted, maxLines = 1) }
                }
            }
            MonoText(
                Fmt.f2(line.total), size = 14.sp, weight = FontWeight.Medium, textAlign = TextAlign.End, maxLines = 1,
                modifier = Modifier.widthIn(min = 90.dp),
            )
        }
    }
}

/** Dashed "+ Add line" button under an item builder. */
@Composable
internal fun AddLineButton(onClick: () -> Unit) {
    val c = Eirs.colors
    Box(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).dashedBorder(c.line, 12.dp).plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("+ Add line", style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.brand)) }
}

/** Payment mode chips and payment term chips (invoice payment details). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaymentPicker(
    mode: InvoicePaymentMode,
    term: PaymentTerm,
    onMode: (InvoicePaymentMode) -> Unit,
    onTerm: (PaymentTerm) -> Unit,
    labelled: Boolean = false,
) {
    val c = Eirs.colors
    if (labelled) Text("Mode", style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InvoicePaymentMode.entries.forEach { m ->
            EirsChip(m.displayName, selected = mode == m, onClick = { onMode(m) }, height = 38.dp, radius = 10.dp, horizontalPadding = 13.dp)
        }
    }
    if (labelled) Text("Term", style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PaymentTerm.entries.forEach { t ->
            EirsChip(t.displayName, selected = term == t, onClick = { onTerm(t) }, modifier = Modifier.weight(1f), height = 38.dp, radius = 10.dp)
        }
    }
}

/** Taxable / per-tax / total rows (white card with hairlines). */
@Composable
internal fun TotalsCard(rows: List<Pair<String, String>>) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, c.line, shape).padding(horizontal = 16.dp, vertical = 4.dp)) {
        rows.forEachIndexed { i, (k, v) ->
            Row(
                Modifier.fillMaxWidth().then(if (i > 0) Modifier.topHairline(c.line) else Modifier).padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(k, style = EirsType.sans(13.sp).copy(color = c.muted))
                MonoText(v, size = 13.sp)
            }
        }
    }
}

/**
 * Sticky footer for document pages: context label, readiness dot + hint, the serif total and the
 * submit button (disabled until [ready]).
 */
@Composable
internal fun ColumnScope.DocumentSubmitFooter(
    label: String,
    ready: Boolean,
    hint: String,
    total: String,
    submitLabel: String,
    onSubmit: () -> Unit,
    totalColor: Color = Eirs.colors.ink,
) {
    val c = Eirs.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (ready) c.brand else c.pending))
                Text(hint, style = EirsType.sans(11.sp).copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(total, style = EirsType.serif(28.sp).copy(color = totalColor), maxLines = 1)
    }
    PrimaryButton(submitLabel, onClick = onSubmit, enabled = ready, height = 52.dp, fontSize = 15.sp, horizontalPadding = 18.dp)
}
