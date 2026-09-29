package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.CancelReason
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.Party
import com.eirspay.mobile.ui.designsystem.Badge
import com.eirspay.mobile.ui.designsystem.CodeTag
import com.eirspay.mobile.ui.designsystem.EirsBottomSheet
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Eyebrow
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.KeyValueCard
import com.eirspay.mobile.ui.designsystem.KeyValueRow
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.OutlineButton
import com.eirspay.mobile.ui.designsystem.PrimaryButton
import com.eirspay.mobile.ui.designsystem.QrCode
import com.eirspay.mobile.ui.designsystem.StickyBottomBar
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.topHairline
import com.eirspay.mobile.ui.designsystem.verificationUrl
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType

@Composable
fun InvoiceDetailScreen(id: String, onBack: () -> Unit, onViewReceipt: (String) -> Unit) {
    val vm: InvoiceDetailViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Eirs.colors
    val inv = state.invoice

    LoadingHost(state.loading) {
        Box(Modifier.fillMaxSize().background(c.background)) {
            Column(Modifier.fillMaxSize()) {
                ViewHeader(
                    eyebrow = inv?.let { it.documentType.displayName + " · " + Fmt.dateTime(it.issuedAt) } ?: "",
                    onBack = onBack,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                buildAnnotatedString {
                                    append("No. ")
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = c.brand)) { append(inv?.number.orEmpty()) }
                                },
                                style = EirsType.serif(26.sp).copy(color = c.ink), maxLines = 1,
                            )
                            if (inv != null) {
                                val (bg, fg) = statusColors(inv.status)
                                Badge(inv.status.name, bg, fg, tracking = 0.0)
                            }
                        }
                    },
                )
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (inv != null) DetailBody(inv, state.amended, onCopyKey = "detail")
                }
                if (inv != null) {
                    StickyBottomBar(shadow = false) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!inv.isCanceled) {
                                OutlineButton(
                                    "Cancel", onClick = vm::openCancel, height = 50.dp,
                                    borderColor = c.redBorder, contentColor = c.red, fontWeight = FontWeight.Medium,
                                )
                            }
                            PrimaryButton(
                                "View receipt", onClick = { onViewReceipt(inv.id) }, modifier = Modifier.weight(1f),
                                height = 50.dp, fontSize = 15.sp, horizontalPadding = 18.dp,
                            )
                        }
                    }
                }
            }
            EirsBottomSheet(visible = state.cancelSheetOpen, onDismiss = vm::closeCancel) {
                CancelSheetContent(inv?.number.orEmpty(), state.cancelReason, vm::pickReason, vm::confirmCancel)
            }
        }
    }
}

@Composable
private fun ColumnScope.CancelSheetContent(number: String, selected: CancelReason, onPick: (CancelReason) -> Unit, onConfirm: () -> Unit) {
    val c = Eirs.colors
    Text(
        buildAnnotatedString {
            append("Cancel invoice ")
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = c.red)) { append(number) }
        },
        style = EirsType.serif(26.sp).copy(color = c.ink), modifier = Modifier.padding(horizontal = 6.dp),
    )
    Text(
        "MoR records this against the original IRN. It can't be undone.",
        style = EirsType.sans(13.sp, lineHeight = 1.45).copy(color = c.muted),
        modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
    )
    CancelReason.entries.forEach { r ->
        val on = r == selected
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().clip(shape).background(if (on) c.redTint else Color.White)
                .border(1.dp, if (on) c.redBorder else c.line, shape).plainClickable { onPick(r) }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonoText(r.code, size = 11.sp, color = c.muted)
            Text(r.label, modifier = Modifier.weight(1f), style = EirsType.sans(15.sp).copy(color = c.ink))
            if (on) LineIcon(EirsIcons.Check, size = 16.dp, strokeWidth = 2.6f, tint = c.red)
        }
    }
    PrimaryButton(
        "Cancel with MoR", onClick = onConfirm, modifier = Modifier.padding(top = 8.dp), height = 50.dp, fontSize = 15.sp,
        showArrow = false, containerColor = c.red, pressedColor = c.redInk,
    )
}

@Composable
private fun ColumnScope.DetailBody(inv: Invoice, amended: Invoice?, onCopyKey: String) {
    val c = Eirs.colors
    val t = inv.totals
    val (copied, copy) = rememberCopyFeedback()
    val cancellation = inv.cancellation
    val lineCount = Fmt.plural(t.lines.size, "line")

    if (inv.isCanceled && cancellation != null) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.redTint).border(1.dp, c.redBorder, RoundedCornerShape(14.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.red), contentAlignment = Alignment.Center) {
                LineIcon(EirsIcons.Ban, size = 16.dp, tint = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(cancellation.reason.label, style = EirsType.sans(15.sp, FontWeight.Bold).copy(color = c.red))
                Text(
                    "Code ${cancellation.reason.code} · canceled ${Fmt.dateTime(cancellation.canceledAt)} · was ${cancellation.originalStatus.name}. The IRN stays on record as proof.",
                    style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = c.redInk),
                )
            }
        }
    }

    // Grand total
    EirsCard(spacing = 6.dp) {
        Text("Grand total · ${inv.branch.displayName}", style = EirsType.sans(12.sp).copy(color = c.muted))
        Text(
            Fmt.etb(t.grandTotal),
            style = EirsType.serif(38.sp).copy(color = c.ink, textDecoration = if (inv.isCanceled) TextDecoration.LineThrough else null),
        )
        Text(
            "Tax ${Fmt.f2(t.taxTotal)} · $lineCount · ${inv.sourceSystem.type} ${inv.sourceSystem.number}",
            style = EirsType.sans(12.sp).copy(color = c.muted),
        )
    }

    // Proof of registration
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.ink).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("PROOF OF REGISTRATION", color = c.tint2, size = 10.sp, tracking = 0.12)
            Text(
                if (inv.isCanceled) "Canceled" else "Signed by MoR",
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (inv.isCanceled) c.red else c.brand)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                style = EirsType.sans(11.sp, FontWeight.SemiBold).copy(color = Color.White),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(7.dp)) {
                QrCode(verificationUrl(inv.irn), Modifier.fillMaxSize())
            }
            Text(
                buildAnnotatedString {
                    append("Scan to verify at ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append("portal.mor.gov.et") }
                    append(". Anyone can confirm MoR signed it.")
                },
                style = EirsType.sans(12.sp, lineHeight = 1.5).copy(color = Color.White.copy(alpha = 0.8f)),
            )
        }
        Text(
            inv.irn,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(10.dp)).padding(10.dp),
            style = EirsType.mono(11.sp).copy(color = Color.White, lineHeight = (11 * 1.6).sp),
        )
        Row(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f))
                .plainClickable { copy(onCopyKey, inv.irn) },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LineIcon(EirsIcons.Copy, size = 13.dp, tint = Color.White)
            Text(if (copied.value == onCopyKey) "Copied" else "Copy IRN", style = EirsType.sans(13.sp, FontWeight.SemiBold).copy(color = Color.White))
        }
    }

    // Parties
    val s = inv.seller
    PartyCard("SELLER", s.name, listOf("TIN" to (s.tin ?: "—"), "VAT number" to (s.vatNumber ?: "—"), "Address" to (s.address ?: "—"), "Phone" to (s.phone ?: "—"), "Email" to (s.email ?: "—")))
    PartyCard("BUYER · ${inv.transactionType.name}", inv.buyer.name, buyerRows(inv.buyer))

    // Line items
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, c.line, shape)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Line items", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = c.ink))
            Text(lineCount, style = EirsType.sans(12.sp).copy(color = c.muted))
        }
        t.lines.forEach { l ->
            Column(Modifier.fillMaxWidth().topHairline(c.line).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(l.item.description, modifier = Modifier.weight(1f), style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
                    MonoText(Fmt.f2(l.total), size = 13.sp, weight = FontWeight.Medium, maxLines = 1)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MonoText("${qtyText(l.item.quantity)} ${l.item.unit} × ${Fmt.f2(l.item.unitPrice)}", size = 11.sp, color = c.muted, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CodeTag(l.item.taxCode.name)
                        Text("tax ${Fmt.f2(l.tax)}", style = EirsType.sans(12.sp).copy(color = c.muted))
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().topHairline(c.line).background(c.background).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val body = EirsType.sans(13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Taxable subtotal", style = body.copy(color = c.muted))
                MonoText(Fmt.f2(t.subtotal), size = 13.sp)
            }
            t.taxes.forEach { tx ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        buildAnnotatedString {
                            append(tx.code.label + " ")
                            withStyle(SpanStyle(fontFamily = EirsFonts.Mono, fontSize = 11.sp)) { append("on " + Fmt.f2(tx.taxable)) }
                        },
                        modifier = Modifier.weight(1f), style = body.copy(color = c.muted),
                    )
                    MonoText(Fmt.f2(tx.tax), size = 13.sp)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp).topHairline(c.line).padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
            ) {
                Text("Grand total", style = EirsType.sans(13.sp, FontWeight.Bold).copy(color = c.ink), modifier = Modifier.padding(bottom = 3.dp))
                Text(
                    Fmt.etb(t.grandTotal),
                    style = EirsType.serif(24.sp).copy(color = c.ink, textDecoration = if (inv.isCanceled) TextDecoration.LineThrough else null),
                )
            }
        }
    }

    // Side cards
    KeyValueCard(
        "DOCUMENT",
        listOf(
            "Document no." to inv.number, "Counter" to inv.counter,
            "Type" to "${inv.documentType.name} · ${inv.documentType.displayName}",
            "Transaction" to inv.transactionType.name, "Currency" to "ETB", "Status" to inv.status.name,
        ),
    )
    inv.amendment?.let { a ->
        KeyValueCard(
            "AMENDS",
            listOf(
                "Reason" to a.reason,
                "Previous IRN" to (amended?.let { "ETH-" + it.irn.take(10) + "…" } ?: a.previousInvoiceId),
                "Related doc" to (amended?.number ?: "—"),
            ),
        )
    }
    KeyValueCard(
        "SOURCE SYSTEM",
        listOf("Branch" to inv.branch.displayName, "System type" to inv.sourceSystem.type, "System no." to inv.sourceSystem.number),
    )
    KeyValueCard("PAYMENT", listOf("Mode" to inv.paymentMode.code, "Term" to inv.paymentTerm.code))
}

private fun buyerRows(b: Party): List<Pair<String, String>> =
    if (b.tin != null) listOf(
        "TIN" to b.tin!!, "VAT number" to (b.vatNumber ?: "—"), "Address" to (b.address ?: "—"),
        "Phone" to (b.phone ?: "—"), "Email" to (b.email ?: "—"),
    ) else listOf(
        "ID type" to (b.idType?.name ?: "Walk-in"), "ID number" to (b.idNumber ?: "—"), "Phone" to (b.phone ?: "—"),
    )

@Composable
private fun PartyCard(role: String, name: String, rows: List<Pair<String, String>>) {
    EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), spacing = 6.dp) {
        Eyebrow(role, color = Eirs.colors.muted, size = 10.sp, tracking = 0.12)
        Text(name, style = EirsType.serif(22.sp, lineHeight = 1.1).copy(color = Eirs.colors.ink), modifier = Modifier.padding(bottom = 4.dp))
        rows.forEach { (k, v) -> KeyValueRow(k, v) }
    }
}
