package com.eirspay.mobile.feature.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.PaymentMethod
import com.eirspay.domain.model.ReceiptType
import com.eirspay.domain.model.ReceiptView
import com.eirspay.domain.usecase.ObserveReceiptViewsUseCase
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(observeReceipts: ObserveReceiptViewsUseCase) : ViewModel() {
    private val id = MutableStateFlow<String?>(null)

    val state: StateFlow<ReceiptView?> = combine(id, observeReceipts()) { id, views -> views.find { it.receipt.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun bind(receiptId: String) { id.value = receiptId }
}

@Composable
fun ReceiptDetailScreen(id: String, onBack: () -> Unit, onOpenInvoice: (String) -> Unit, onOpenWithholding: (String) -> Unit) {
    val vm: ReceiptDetailViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val view by vm.state.collectAsStateWithLifecycle()
    val v = view
    val c = Eirs.colors
    if (v == null) {
        ViewScaffold(header = { ViewHeader("RECEIPT", onBack, title = { ViewTitle(id) }) }) {}
        return
    }
    val r = v.receipt
    val refund = r.type == ReceiptType.REFUND
    val tone = r.type.tone()
    ViewScaffold(
        header = {
            ViewHeader(
                eyebrow = "RCP · ${r.type.displayName.uppercase()} · REGISTERED WITH EIRS", onBack = onBack,
                title = { ViewTitle(r.displayNo) },
                trailing = { Badge(r.type.displayName, tone.bg, tone.fg) },
            )
        },
        bodyPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
    ) {
        // Amount + payment
        EirsCard(spacing = 6.dp) {
            CardEyebrow((if (refund) "REFUNDED" else "RECEIVED") + " · " + Fmt.dateTime(r.issuedAt))
            Text(
                (if (refund) "−" else "") + Fmt.etb(v.amount),
                style = EirsType.serif(38.sp).copy(color = if (refund) c.red else c.ink),
            )
            Column(Modifier.padding(top = 6.dp)) { paymentRows(v).forEach { (k, value) -> KvRow(k, value) } }
        }

        // Linked invoice
        val li = v.linkedInvoice
        if (li != null) {
            val st = li.status.tone()
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .pressBackground(Color.White, c.tint1, RoundedCornerShape(16.dp)) { onOpenInvoice(li.id) }
                    .border16(c.line).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    CardEyebrow("LINKED INVOICE")
                    Badge(li.status.name, st.bg, st.fg)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(li.displayNo, style = EirsType.serif(26.sp))
                    MonoText(Fmt.etb(li.totals.grandTotal), size = 13.sp)
                }
                Text("${li.buyer.name} · ${Fmt.dateTime(li.issuedAt)}", style = EirsType.sans(13.sp).copy(color = c.muted))
                val w = v.withholding
                if (w != null) {
                    Text(
                        "Buyer withheld ${Fmt.etb(v.withheldAmount)} (${w.rate.percentText()}) · ${w.id} ↗",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.warnTint)
                            .plainClickable { onOpenWithholding(w.id) }.padding(horizontal = 11.dp, vertical = 9.dp),
                        style = EirsType.sans(12.sp, lineHeight = 1.4).copy(color = c.amberInk),
                    )
                }
            }
        }

        // Line items or note
        if (v.totals.lines.isNotEmpty()) {
            EirsCard(padding = PaddingValues(0.dp)) {
                Text("Line items", Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), style = EirsType.sans(15.sp, FontWeight.SemiBold))
                v.totals.lines.forEach { lt ->
                    val it = lt.item
                    Column(
                        Modifier.fillMaxWidth().topHairline(c.line).padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(it.description, Modifier.weight(1f), style = EirsType.sans(14.sp, FontWeight.Medium))
                            MonoText(Fmt.f2(lt.total), size = 13.sp, maxLines = 1)
                        }
                        MonoText("${it.quantity.qtyText()} ${it.unit} × ${Fmt.f2(it.unitPrice)} · ${it.itemCode}", size = 11.sp, color = c.muted)
                    }
                }
                Column(
                    Modifier.fillMaxWidth().topHairline(c.line).background(c.background).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    TotalsLine("Taxable", Fmt.f2(v.totals.subtotal))
                    v.totals.taxes.forEach { t -> TotalsLine(t.code.label, Fmt.f2(t.tax)) }
                }
            }
        } else {
            Text(
                if (r.type == ReceiptType.PAYMENT) "VAT was registered on the linked invoice. This receipt records the payment only."
                else "Refund against the linked invoice. MoR reverses the tax on the refunded amount.",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.tint1).padding(horizontal = 16.dp, vertical = 14.dp),
                style = EirsType.sans(13.sp, lineHeight = 1.5).copy(color = c.brandDark),
            )
        }

        ProofCard(r.irn)

        EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            CardEyebrow("RECEIVED FROM", Modifier.padding(bottom = 6.dp))
            (listOf("Name" to v.buyer.name) + v.buyer.detailRows()).forEach { (k, value) -> KvRow(k, value) }
        }
        EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            CardEyebrow("DOCUMENT", Modifier.padding(bottom = 6.dp))
            listOf(
                "Receipt no." to r.displayNo, "Type" to r.type.displayName, "Currency" to "ETB",
                "Branch" to r.branch.displayName, "System no." to r.sourceSystem.number,
            ).forEach { (k, value) -> KvRow(k, value) }
        }
    }
}

private fun paymentRows(v: ReceiptView): List<Pair<String, String>> {
    val r = v.receipt
    return when (r.method) {
        PaymentMethod.CASH -> listOf("Method" to "Cash", "Tendered" to Fmt.f2(r.tendered ?: v.amount), "Change" to Fmt.f2(v.change))
        PaymentMethod.MOBILE_MONEY -> listOf("Method" to "Mobile money · ${r.provider.orEmpty()}", "Transaction ID" to (r.reference ?: "—"))
        PaymentMethod.BANK_TRANSFER -> listOf("Method" to "Bank transfer", "Reference" to (r.reference ?: "—"))
        PaymentMethod.CHEQUE -> listOf("Method" to "Cheque", "Cheque no." to (r.reference ?: "—"))
        PaymentMethod.CARD -> listOf("Method" to "Card", "Card" to (r.reference ?: "—"))
    }
}

@Composable
private fun TotalsLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = EirsType.sans(13.sp).copy(color = Eirs.colors.muted))
        MonoText(value, size = 13.sp)
    }
}

/** Dark "PROOF OF REGISTRATION" card with the real verification QR, the IRN and a copy button. */
@Composable
internal fun ProofCard(irn: String) {
    val c = Eirs.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1400); copied = false } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.ink).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Eyebrow("PROOF OF REGISTRATION", color = c.tint2, size = 10.sp, tracking = 0.12)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(7.dp)) {
                QrCode(verificationUrl(irn), Modifier.fillMaxSize())
            }
            Text(irn, style = EirsType.mono(10.sp).copy(color = Color.White.copy(alpha = 0.85f), lineHeight = 16.sp))
        }
        Box(
            Modifier.fillMaxWidth().height(40.dp)
                .pressBackground(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)) {
                    clipboard.setText(AnnotatedString(irn)); copied = true
                },
            contentAlignment = Alignment.Center,
        ) { Text(if (copied) "Copied" else "Copy IRN", style = EirsType.sans(13.sp, FontWeight.SemiBold).copy(color = Color.White)) }
    }
}

private fun Modifier.border16(color: Color) = this.then(
    Modifier.border(1.dp, color, RoundedCornerShape(16.dp)),
)
