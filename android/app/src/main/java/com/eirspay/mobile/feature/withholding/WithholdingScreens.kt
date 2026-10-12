package com.eirspay.mobile.feature.withholding

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.WithholdingView
import com.eirspay.domain.model.ZeroMoney
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.MarkCertificateReceivedUseCase
import com.eirspay.domain.usecase.ObserveWithholdingUseCase
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

enum class CertFilter(val label: String) { ALL("All"), AWAITING("Awaiting certificate"), RECEIVED("Certificate received") }

data class WithholdingListUiState(
    val eyebrow: String = "TAX WITHHELD",
    val filter: CertFilter = CertFilter.ALL,
    val records: List<WithholdingView> = emptyList(),
    val totalWithheld: BigDecimal = ZeroMoney,
    val awaiting: Int = 0,
    val net: BigDecimal = ZeroMoney,
)

@HiltViewModel
class WithholdingViewModel @Inject constructor(
    observeWithholding: ObserveWithholdingUseCase,
    preferences: PreferencesRepository,
    private val markReceived: MarkCertificateReceivedUseCase,
) : ViewModel() {
    private val filter = MutableStateFlow(CertFilter.ALL)
    private val selectedId = MutableStateFlow<String?>(null)
    private val all = observeWithholding()

    val list: StateFlow<WithholdingListUiState> = combine(all, preferences.selectedBranch, filter) { views, branch, f ->
        val scoped = views.filter { branch == null || it.invoice.branch == branch }
        val rows = scoped.filter { f == CertFilter.ALL || (f == CertFilter.RECEIVED) == it.record.certificateReceived }
        WithholdingListUiState(
            eyebrow = "TAX WITHHELD · " + (branch?.displayName ?: "All branches").uppercase(),
            filter = f, records = rows,
            totalWithheld = rows.map { it.amount }.sumMoney(),
            awaiting = rows.count { !it.record.certificateReceived },
            net = rows.map { it.netPaid }.sumMoney(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WithholdingListUiState())

    val detail: StateFlow<WithholdingView?> = combine(all, selectedId) { views, id -> views.find { it.record.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onFilter(f: CertFilter) { filter.value = f }
    fun bind(id: String) { selectedId.value = id }
    fun markCertificateReceived(id: String) { viewModelScope.launch { markReceived(id) } }
}

private fun pct(rate: BigDecimal) = rate.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%"

@Composable
fun WithholdingScreen(onBack: () -> Unit, onOpenRecord: (String) -> Unit, onOpenInvoice: (String) -> Unit) {
    val vm: WithholdingViewModel = hiltViewModel()
    val s by vm.list.collectAsStateWithLifecycle()
    val c = Eirs.colors
    ViewScaffold(
        header = { ViewHeader(s.eyebrow, onBack, title = { ViewTitle("Withholding") }) },
        bodyPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
    ) {
        Text(
            "Buyers who withheld part of a payment and remitted it to MoR for you. Collect each certificate to claim it.",
            style = EirsType.sans(13.sp, lineHeight = 1.5).copy(color = c.muted),
        )
        val stats = listOf(
            Triple("Records", s.records.size.toString(), c.ink),
            Triple("Total withheld", Fmt.etb(s.totalWithheld), c.ink),
            Triple("Awaiting certificate", s.awaiting.toString(), c.warnInk),
            Triple("Net paid to you", Fmt.etb(s.net), c.ink),
        )
        stats.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, value, color) ->
                    EirsCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), spacing = 5.dp) {
                        Text(label, style = EirsType.sans(11.sp).copy(color = c.muted))
                        Text(value, style = EirsType.serif(22.sp).copy(color = color), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
        ChipRail { CertFilter.entries.forEach { f -> EirsChip(f.label, s.filter == f, { vm.onFilter(f) }) } }
        s.records.forEach { w ->
            val st = certTone(w.record.certificateReceived)
            ListCard(onClick = { onOpenRecord(w.record.id) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    MonoText(w.record.id, size = 13.sp, weight = FontWeight.Medium)
                    MonoText(Fmt.etb(w.amount), size = 14.sp, weight = FontWeight.Medium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(w.invoice.buyer.name, Modifier.weight(1f), style = EirsType.sans(13.sp, FontWeight.Medium), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text("${pct(w.record.rate)} of ${Fmt.etb(w.base)}", style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Badge(st.label, st.bg, st.fg)
                    InvoiceChip("INV-${w.invoice.number}") { onOpenInvoice(w.invoice.id) }
                }
            }
        }
    }
}

private data class CertTone(val label: String, val bg: Color, val fg: Color)

@Composable
private fun certTone(received: Boolean): CertTone {
    val c = Eirs.colors
    return if (received) CertTone("Certificate received", c.paidTint, c.paidInk) else CertTone("Awaiting certificate", c.warnTint, c.warnInk)
}

@Composable
private fun InvoiceChip(text: String, onClick: () -> Unit) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(6.dp)
    Text(
        "$text ↗", maxLines = 1,
        modifier = Modifier.clip(shape).background(c.tint1).border(1.dp, c.tint2, shape).plainClickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        style = EirsType.mono(11.sp).copy(color = c.brand),
    )
}

@Composable
private fun DetailRows(title: String, rows: List<Pair<String, String>>, headline: String? = null) {
    val c = Eirs.colors
    EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Eyebrow(title, color = c.muted, size = 10.sp, tracking = 0.12, modifier = Modifier.padding(bottom = if (headline == null) 6.dp else 0.dp))
        if (headline != null) Text(headline, Modifier.padding(top = 6.dp, bottom = 8.dp), style = EirsType.serif(22.sp, 1.1))
        rows.forEach { (k, v) -> KeyValueRow(k, v) }
    }
}

@Composable
fun WithholdingDetailScreen(id: String, onBack: () -> Unit, onOpenInvoice: (String) -> Unit, onOpenReceipt: (String) -> Unit) {
    val vm: WithholdingViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val view by vm.detail.collectAsStateWithLifecycle()
    val w = view
    val c = Eirs.colors
    if (w == null) {
        ViewScaffold(header = { ViewHeader("WITHHOLDING", onBack, title = { ViewTitle(id) }) }) {}
        return
    }
    val inv = w.invoice
    val st = certTone(w.record.certificateReceived)
    val agent = inv.buyer
    ViewScaffold(
        header = {
            ViewHeader("WHT · ${inv.transactionType.name} · ${st.label.uppercase()}", onBack, title = { ViewTitle(w.record.id) })
        },
        bodyPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
    ) {
        if (!w.record.certificateReceived) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.amberTint)
                    .border(1.dp, c.amberBorder, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "No certificate yet. Ask ${agent.name} for it so you can claim ${Fmt.etb(w.amount)} against your tax.",
                    style = EirsType.sans(13.sp, lineHeight = 1.45).copy(color = c.amberInk),
                )
                Box(
                    Modifier.fillMaxWidth().height(42.dp)
                        .pressBackground(c.amberInk, c.amberInk.copy(alpha = 0.85f), RoundedCornerShape(10.dp)) { vm.markCertificateReceived(w.record.id) },
                    contentAlignment = Alignment.Center,
                ) { Text("Mark certificate received", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = Color.White)) }
            }
        }

        EirsCard {
            Eyebrow("CALCULATION", color = c.muted, size = 10.sp, tracking = 0.12, modifier = Modifier.padding(bottom = 6.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Taxable base", style = EirsType.sans(14.sp).copy(color = c.muted))
                MonoText(Fmt.etb(w.base), size = 14.sp)
            }
            Row(Modifier.fillMaxWidth().bottomHairline(c.line).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("× Rate", style = EirsType.sans(14.sp).copy(color = c.muted))
                MonoText(pct(w.record.rate), size = 14.sp)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Withheld", style = EirsType.sans(14.sp, FontWeight.SemiBold))
                Text(Fmt.etb(w.amount), style = EirsType.serif(32.sp).copy(color = c.brand))
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.background).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Invoice total", style = EirsType.sans(13.sp).copy(color = c.muted))
                    MonoText(Fmt.etb(inv.totals.grandTotal), size = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net paid to you", style = EirsType.sans(13.sp, FontWeight.SemiBold))
                    MonoText(Fmt.etb(w.netPaid), size = 13.sp, weight = FontWeight.Medium)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().pressBackground(Color.White, c.tint1, RoundedCornerShape(16.dp)) { onOpenInvoice(inv.id) }
                .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("RELATED INVOICE", color = c.muted, size = 10.sp, tracking = 0.12)
                Text("INV-${inv.number}", style = EirsType.serif(24.sp))
                Text("${Fmt.dateTime(inv.issuedAt)} · ${inv.transactionType.name}", style = EirsType.sans(12.sp).copy(color = c.muted))
            }
            LineIcon(EirsIcons.ChevronRight, size = 15.dp, tint = c.muted)
        }

        val pay = w.paymentReceipt
        if (pay != null) {
            Row(
                Modifier.fillMaxWidth().pressBackground(Color.White, c.tint1, RoundedCornerShape(16.dp)) { onOpenReceipt(pay.receipt.id) }
                    .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = c.muted)) { append("Paid on") }
                        append(" ")
                        withStyle(SpanStyle(fontFamily = EirsType.mono(13.sp).fontFamily, fontWeight = FontWeight.Medium)) { append("RCP-${pay.receipt.number}") }
                    },
                    style = EirsType.sans(13.sp),
                )
                MonoText(Fmt.etb(pay.amount) + " ↗", size = 13.sp)
            }
        }

        DetailRows(
            "WITHHOLDING AGENT",
            listOf("TIN" to (agent.tin ?: "—"), "VAT number" to (agent.vatNumber ?: "—"), "Address" to (agent.address ?: "—"), "Email" to (agent.email ?: "—")),
            headline = agent.name,
        )
        DetailRows(
            "RECORD",
            listOf(
                "Certificate no." to w.record.id, "Withheld on" to Fmt.day(w.record.withheldOn), "Tax period" to w.record.taxPeriod,
                "Related IRN" to ("ETH-" + inv.irn.take(12) + "…"), "Status" to st.label,
            ),
        )
    }
}
