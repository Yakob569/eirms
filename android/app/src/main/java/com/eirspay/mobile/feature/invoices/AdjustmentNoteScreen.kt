package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.Invoice
import com.eirspay.mobile.ui.designsystem.CodeTag
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Eyebrow
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.FormField
import com.eirspay.mobile.ui.designsystem.LabeledField
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.TextAreaField
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.ViewScaffold
import com.eirspay.mobile.ui.designsystem.ViewTitle
import com.eirspay.mobile.ui.designsystem.decimalOnly
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.topHairline
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/**
 * Credit or debit note for an invoice. Reads as mostly read-only: seller, buyer and branch are
 * locked text (the buyer must match the original), and only ticked lines get two number fields.
 */
@Composable
fun AdjustmentNoteScreen(invoiceId: String, kind: DocumentType, onClose: () -> Unit, onRegistered: (String) -> Unit) {
    val vm: AdjustmentNoteViewModel = hiltViewModel()
    LaunchedEffect(invoiceId, kind) { vm.bind(invoiceId, kind) }
    val s by vm.state.collectAsStateWithLifecycle()
    val registered by rememberUpdatedState(onRegistered)
    LaunchedEffect(vm) { vm.registered.collect { registered(it) } }
    val c = Eirs.colors
    val inv = s.original
    val srcNo = inv?.docNo.orEmpty()

    LoadingHost(s.loading) {
        ViewScaffold(
            header = {
                ViewHeader(
                    eyebrow = (if (s.isCredit) "CREDIT NOTE" else "DEBIT NOTE") + " · AMENDS " + srcNo,
                    onBack = onClose, backIcon = EirsIcons.Close,
                    title = { ViewTitle(kind.displayName + " ", srcNo) },
                )
            },
            bottomBar = {
                DocumentSubmitFooter(
                    label = if (s.isCredit) "Reduces $srcNo by" else "Adds to $srcNo",
                    ready = s.ready, hint = s.hint,
                    total = (if (s.isCredit) "−" else "+") + Fmt.etb(s.grandTotal),
                    totalColor = if (s.isCredit) c.red else c.ink,
                    submitLabel = "Submit " + kind.displayName.lowercase(),
                    onSubmit = vm::submit,
                )
            },
        ) {
            if (inv != null) LockedSummary(inv)

            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
            ) {
                Eyebrow("LINES TO ADJUST", color = c.muted)
                Text("${s.picks.size} of ${s.lines.size} lines", style = EirsType.sans(12.sp).copy(color = c.muted))
            }
            s.lines.forEach { line ->
                AdjustLineCard(
                    line, warn = line.index in s.exceeding, isCredit = s.isCredit,
                    onToggle = { vm.toggle(line.index) },
                    onQuantity = { vm.setQuantity(line.index, it.decimalOnly()) },
                    onPrice = { vm.setPrice(line.index, it.decimalOnly()) },
                )
            }

            Eyebrow("REASON *", color = c.muted, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp))
            TextAreaField(s.reason, vm::setReason, placeholder = "e.g. Returned 1 unit, damaged in delivery")

            TotalsCard(listOf("Taxable" to Fmt.f2(s.subtotal), "Tax" to Fmt.f2(s.taxTotal)))
        }
    }
}

@Composable
private fun LockedSummary(inv: Invoice) {
    val c = Eirs.colors
    val b = inv.buyer
    val cells = listOf(
        Triple("SELLER", inv.seller.name, "TIN " + (inv.seller.tin ?: "—")),
        Triple("BUYER", b.name, partyIdLabel(b)),
        Triple("BRANCH", inv.branch.displayName, "${inv.sourceSystem.type} ${inv.sourceSystem.number}"),
        Triple("ORIGINAL", Fmt.etb(inv.totals.grandTotal), "${inv.transactionType.name} · ${Fmt.isoDate(inv.issuedAt)}"),
    )
    EirsCard(spacing = 12.dp) {
        cells.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (k, a, sub) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Eyebrow(k, color = c.muted, size = 10.sp, tracking = 0.1)
                        Text(a, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MonoText(sub, size = 11.sp, color = c.muted, maxLines = 1)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().topHairline(c.line).padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LineIcon(EirsIcons.Lock, size = 13.dp, tint = c.muted)
            Text("Locked to the original invoice", style = EirsType.sans(12.sp).copy(color = c.muted))
        }
    }
}

@Composable
private fun AdjustLineCard(
    line: AdjustLineUi,
    warn: Boolean,
    isCredit: Boolean,
    onToggle: () -> Unit,
    onQuantity: (String) -> Unit,
    onPrice: (String) -> Unit,
) {
    val c = Eirs.colors
    val on = line.selected
    val shape = RoundedCornerShape(16.dp)
    val item = line.item
    Column(
        Modifier.fillMaxWidth().clip(shape).background(if (on) Color.White else c.background)
            .border(1.5.dp, if (on) c.brand else c.line, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().plainClickable(onClick = onToggle), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.padding(top = 1.dp).size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (on) c.brand else Color.White)
                    .border(1.5.dp, if (on) c.brand else Color(0xFFB9CBCE), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { if (on) LineIcon(EirsIcons.Check, size = 13.dp, strokeWidth = 3f, tint = Color.White) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.description, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
                MonoText("${item.itemCode} · ${qtyText(item.quantity)} ${item.unit} × ${Fmt.f2(item.unitPrice)}", size = 11.sp, color = c.muted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CodeTag(item.taxCode.name)
                MonoText(Fmt.f2(line.originalTotal), size = 12.sp, color = c.muted)
            }
        }
        if (on) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField("Quantity (${item.unit})", modifier = Modifier.weight(1f), labelSize = 12.sp, gap = 6.dp) {
                    FormField(line.quantity, onQuantity, mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal)
                }
                LabeledField("Unit price", modifier = Modifier.weight(1.4f), labelSize = 12.sp, gap = 6.dp) {
                    FormField(line.price, onPrice, mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal)
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.tint1).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("New line total", style = EirsType.sans(13.sp).copy(color = c.brandDark), maxLines = 1)
                MonoText(Fmt.etb(line.total), size = 13.sp, weight = FontWeight.Medium, color = c.brandDark, maxLines = 1)
            }
            if (warn && isCredit) {
                Text("A credit note can’t exceed the original line.", style = EirsType.sans(12.sp, FontWeight.Medium).copy(color = c.red))
            }
        }
    }
}
