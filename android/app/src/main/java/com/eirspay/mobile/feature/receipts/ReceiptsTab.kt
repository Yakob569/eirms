package com.eirspay.mobile.feature.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.eirspay.domain.model.sumMoney
import com.eirspay.domain.repository.PreferencesRepository
import com.eirspay.domain.usecase.ObserveReceiptViewsUseCase
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ReceiptRowUi(
    val id: String,
    val number: String,
    val type: ReceiptType,
    val amount: String,
    val refund: Boolean,
    val party: String,
    val method: String,
    val methodSub: String,
    val linkedInvoiceId: String?,
    val linkedNo: String?,
    val whenText: String,
    val branch: String,
)

data class ReceiptsTabUiState(
    val query: String = "",
    val type: ReceiptType? = null,
    val method: PaymentMethod? = null,
    val rows: List<ReceiptRowUi> = emptyList(),
    val summary: String = "",
)

private data class ReceiptFilters(val query: String = "", val type: ReceiptType? = null, val method: PaymentMethod? = null)

@HiltViewModel
class ReceiptsTabViewModel @Inject constructor(
    observeReceipts: ObserveReceiptViewsUseCase,
    preferences: PreferencesRepository,
) : ViewModel() {
    private val filters = MutableStateFlow(ReceiptFilters())

    val state: StateFlow<ReceiptsTabUiState> =
        combine(observeReceipts(), preferences.selectedBranch, filters) { views, branch, f ->
            val q = f.query.trim().lowercase()
            val rows = views.filter { v ->
                val r = v.receipt
                (branch == null || r.branch == branch) &&
                    (f.type == null || r.type == f.type) &&
                    (f.method == null || r.method == f.method) &&
                    (q.isEmpty() || listOf(r.displayNo, v.buyer.name, r.reference.orEmpty(), v.linkedInvoice?.displayNo.orEmpty())
                        .joinToString(" ").lowercase().contains(q))
            }
            val collected = rows.filter { it.receipt.type != ReceiptType.REFUND }.map { it.amount }.sumMoney()
            ReceiptsTabUiState(
                query = f.query, type = f.type, method = f.method,
                rows = rows.map(::toRow),
                summary = Fmt.plural(rows.size, "receipt") + " · " + Fmt.etb(collected) + " collected",
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptsTabUiState())

    fun onQuery(q: String) = filters.update { it.copy(query = q) }
    fun onType(t: ReceiptType?) = filters.update { it.copy(type = t) }
    fun onMethod(m: PaymentMethod?) = filters.update { it.copy(method = m) }

    private fun toRow(v: ReceiptView): ReceiptRowUi {
        val r = v.receipt
        val refund = r.type == ReceiptType.REFUND
        val sub = when (r.method) {
            PaymentMethod.MOBILE_MONEY -> "${r.provider.orEmpty()} · ${r.reference.orEmpty()}"
            PaymentMethod.CASH -> "Tendered " + Fmt.f2(r.tendered ?: v.amount)
            else -> r.reference ?: "—"
        }
        return ReceiptRowUi(
            id = r.id, number = r.displayNo, type = r.type,
            amount = (if (refund) "−" else "") + Fmt.etb(v.amount), refund = refund,
            party = v.buyer.name, method = r.method.displayName, methodSub = sub,
            linkedInvoiceId = v.linkedInvoice?.id, linkedNo = v.linkedInvoice?.displayNo,
            whenText = Fmt.dateTime(r.issuedAt), branch = r.branch.displayName,
        )
    }
}

private val TypeFilters = listOf<Pair<ReceiptType?, String>>(null to "All") + ReceiptType.entries.map { it to it.displayName }
private val MethodFilters = listOf<Pair<PaymentMethod?, String>>(null to "Any method") + PaymentMethod.entries.map { it to it.displayName }

/** Receipts tab body, embedded in the shell's scrolling column (the shell draws the page title). */
@Composable
fun ReceiptsTabContent(onOpenReceipt: (String) -> Unit, onOpenInvoice: (String) -> Unit, onNewReceipt: () -> Unit) {
    val vm: ReceiptsTabViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Eirs.colors
    Column(Modifier.fillMaxWidth().riseIn(300), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SearchField(state.query, vm::onQuery, "Receipt no., customer, reference")
        ChipRail {
            TypeFilters.forEach { (t, label) -> EirsChip(label, state.type == t, { vm.onType(t) }, style = ChipStyle.Solid) }
        }
        ChipRail {
            MethodFilters.forEach { (m, label) -> EirsChip(label, state.method == m, { vm.onMethod(m) }, style = ChipStyle.Tint) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(state.summary, style = EirsType.sans(13.sp).copy(color = c.muted), modifier = Modifier.weight(1f, fill = false))
            TextLink("+ New receipt", onNewReceipt, size = 13.sp)
        }
        state.rows.forEach { row -> ReceiptCard(row, { onOpenReceipt(row.id) }, onOpenInvoice) }
        if (state.rows.isEmpty()) EmptyState("No receipts match these filters.")
    }
}

@Composable
private fun ReceiptCard(row: ReceiptRowUi, onOpen: () -> Unit, onOpenInvoice: (String) -> Unit) {
    val c = Eirs.colors
    val tone = row.type.tone()
    ListCard(onClick = onOpen) {
        SplitRow(
            left = {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(row.number, style = EirsType.sans(16.sp, FontWeight.SemiBold), maxLines = 1)
                    Badge(row.type.displayName, tone.bg, tone.fg)
                }
            },
            right = { MonoText(row.amount, size = 13.sp, weight = FontWeight.Medium, color = if (row.refund) c.red else c.ink, maxLines = 1) },
        )
        SplitRow(
            left = { Text(row.party, Modifier.weight(1f), style = EirsType.sans(13.sp, FontWeight.Medium), maxLines = 1, overflow = ellipsis) },
            right = { Text(row.method, style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1) },
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoText(row.methodSub, Modifier.weight(1f), size = 11.sp, color = c.muted, maxLines = 1)
            if (row.linkedInvoiceId != null && row.linkedNo != null) LinkChip(row.linkedNo, { onOpenInvoice(row.linkedInvoiceId) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.whenText, style = EirsType.sans(11.sp).copy(color = c.muted))
            Text(row.branch, style = EirsType.sans(11.sp).copy(color = c.muted))
        }
    }
}
