package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.InvoiceStatus
import com.eirspay.domain.model.TransactionType
import com.eirspay.mobile.ui.designsystem.Badge
import com.eirspay.mobile.ui.designsystem.ChipRail
import com.eirspay.mobile.ui.designsystem.ChipStyle
import com.eirspay.mobile.ui.designsystem.EirsChip
import com.eirspay.mobile.ui.designsystem.EmptyState
import com.eirspay.mobile.ui.designsystem.KebabButton
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.ListCard
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.SearchField
import com.eirspay.mobile.ui.designsystem.riseIn
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType

private val StatusChips = listOf(
    null to "All",
    InvoiceStatus.ISSUED to "Issued",
    InvoiceStatus.PAID to "Paid",
    InvoiceStatus.WITHHELD to "Withheld",
    InvoiceStatus.CANCELED to "Canceled",
)

private val TypeChips = listOf<TransactionType?>(null, TransactionType.B2B, TransactionType.B2C, TransactionType.B2G, TransactionType.G2B, TransactionType.G2C)

/** Invoices tab body, embedded in the shell's scrolling column (the shell shows the page title). */
@Composable
fun InvoicesTabContent(showCanceledRequest: Int, onOpenInvoice: (String) -> Unit) {
    val vm: InvoicesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(showCanceledRequest) { vm.onCanceledRequest(showCanceledRequest) }

    val c = Eirs.colors
    val f = state.filters
    val (copied, copy) = rememberCopyFeedback()
    val currentStatus = if (f.canceledMode) InvoiceStatus.CANCELED else f.status

    Column(Modifier.fillMaxWidth().riseIn(300), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchField(f.query, vm::setQuery, placeholder = "Number, IRN, customer, TIN")
        ChipRail {
            StatusChips.forEach { (s, label) ->
                EirsChip(label, selected = currentStatus == s, onClick = { vm.pickStatus(s) })
            }
        }
        ChipRail {
            TypeChips.forEach { tt ->
                EirsChip(
                    tt?.name ?: "Any type", selected = f.transactionType == tt, onClick = { vm.pickTransactionType(tt) },
                    style = ChipStyle.Tint, height = 30.dp, radius = 8.dp, mono = true, fontSize = 12.sp, horizontalPadding = 11.dp,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            val n = state.rows.size
            Text(
                Fmt.plural(n, "document") + " · " + (state.branch?.displayName ?: "All branches"),
                style = EirsType.sans(13.sp).copy(color = c.muted), modifier = Modifier.weight(1f, fill = false),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = EirsFonts.Mono, fontWeight = FontWeight.Medium, color = c.ink)) { append(Fmt.etb(state.sum)) }
                    append(if (f.canceledMode) " voided" else " gross")
                },
                style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1,
            )
        }
        state.rows.forEach { inv ->
            InvoiceCard(
                inv, canceledMode = f.canceledMode, copied = copied.value == inv.id,
                onCopy = { copy(inv.id, inv.irn) }, onOpen = { onOpenInvoice(inv.id) },
                onActions = { vm.openActions(inv.id) },
            )
        }
        if (state.loaded && state.rows.isEmpty()) EmptyState("No documents match these filters.")
    }
}

@Composable
private fun InvoiceCard(
    inv: Invoice,
    canceledMode: Boolean,
    copied: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onActions: () -> Unit,
) {
    val c = Eirs.colors
    ListCard(onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(inv.number, style = EirsType.sans(16.sp, FontWeight.SemiBold).copy(color = c.ink), maxLines = 1)
                Text(
                    inv.documentType.name, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).border(1.dp, c.line, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                    style = EirsType.mono(10.sp).copy(color = c.muted),
                )
                val (bg, fg) = statusColors(inv.status)
                Badge(inv.status.name, bg, fg)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MonoText(
                    Fmt.etb(inv.totals.grandTotal), size = 13.sp, weight = FontWeight.Medium, strike = canceledMode,
                    color = if (canceledMode) c.muted else c.ink, maxLines = 1,
                )
                KebabButton(onClick = onActions)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                inv.buyer.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink),
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = EirsFonts.Mono, fontWeight = FontWeight.Medium, color = c.ink)) { append(inv.transactionType.name) }
                    append(" · " + partyIdLabel(inv.buyer))
                },
                style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1,
            )
        }
        val cancellation = inv.cancellation
        if (!canceledMode) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MonoText(Fmt.irnShort(inv.irn), size = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                CopyChip(copied, onCopy)
            }
        } else if (cancellation != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.redTint).padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(cancellation.reason.label, style = EirsType.sans(13.sp, FontWeight.SemiBold).copy(color = c.red))
                MonoText("Code ${cancellation.reason.code} · was ${cancellation.originalStatus.name}", size = 10.sp, color = c.redInk)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val muted = EirsType.sans(11.sp).copy(color = c.muted)
            if (canceledMode && cancellation != null) {
                Text(Fmt.dateTime(cancellation.canceledAt), style = muted)
                Text("Issued " + Fmt.day(inv.issuedAt), style = muted)
            } else {
                Text(Fmt.dateTime(inv.issuedAt), style = muted)
                Text(inv.branch.displayName, style = muted)
            }
        }
    }
}
