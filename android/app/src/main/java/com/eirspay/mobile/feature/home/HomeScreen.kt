package com.eirspay.mobile.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.*
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import java.time.LocalDate
import kotlin.math.roundToInt

/** Home (dashboard) tab body, rendered inside the shell's scrolling content column. */
@Composable
fun HomeTab(
    onOpenInvoice: (String) -> Unit,
    onOpenReceipt: (String) -> Unit,
    onViewAllInvoices: () -> Unit,
) {
    val vm: HomeViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onRange = vm::selectRange,
        onToggleBranch = vm::toggleBranch,
        onOpenActivity = { a -> if (a.kind == ActivityKind.RECEIPT) onOpenReceipt(a.documentId) else onOpenInvoice(a.documentId) },
        onViewAllInvoices = onViewAllInvoices,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onRange: (DashboardRange) -> Unit,
    onToggleBranch: (Branch) -> Unit,
    onOpenActivity: (ActivityItem) -> Unit,
    onViewAllInvoices: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChipRail {
            DashboardRange.entries.forEach { r ->
                EirsChip(r.chipLabel, selected = state.range == r, onClick = { onRange(r) })
            }
        }
        val a = state.analytics
        if (a != null) {
            TotalSalesCard(a)
            Tiles(a)
            SalesTaxCard(a)
            TaxByCodeCard(a)
            TransactionTypeCard(a)
            BranchCard(a, state.branch, state.branches, onToggleBranch)
        }
        RecentActivityCard(state.recent, onOpenActivity, onViewAllInvoices)
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun TotalSalesCard(a: SalesAnalytics) {
    val ring = Color.White.copy(alpha = 0.18f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Eirs.colors.brand)
            .drawBehind {
                // 180dp ring hanging off the top-right corner (right:-60, top:-60).
                val r = 90.dp.toPx()
                drawCircle(ring, radius = r - 0.5.dp.toPx(), center = Offset(size.width + 60.dp.toPx() - r, -60.dp.toPx() + r), style = Stroke(1.dp.toPx()))
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val s = EirsType.sans(13.sp).copy(color = Color.White.copy(alpha = 0.85f))
            Text("Total sales", style = s)
            Text(a.range.label, style = s)
        }
        Text(Fmt.etb(a.totalSales), style = EirsType.serif(40.sp).copy(color = Color.White), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "▲ " + a.delta,
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp),
                style = EirsType.sans(12.sp, FontWeight.SemiBold).copy(color = Color.White),
            )
            Text(a.comparison, style = EirsType.sans(12.sp).copy(color = Color.White))
        }
    }
}

private data class Tile(val label: String, val value: String, val sub: String, val red: Boolean = false)

private fun oneDecimal(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
}

private fun grouped(n: Int) = String.format(java.util.Locale.US, "%,d", n)

@Composable
private fun Tiles(a: SalesAnalytics) {
    val tiles = listOf(
        Tile("Tax collected", Fmt.etb(a.taxCollected), oneDecimal(if (a.totalSales > 0) a.taxCollected / a.totalSales * 100 else 0.0) + "% of gross"),
        Tile("Canceled", a.canceledCount.toString(), Fmt.etb(a.canceledValue) + " lost", red = true),
        Tile("Invoices issued", grouped(a.invoicesIssued), "avg " + Fmt.etb(a.totalSales * .62 / a.invoicesIssued.coerceAtLeast(1))),
        Tile("Receipts issued", grouped(a.receiptsIssued), "avg " + Fmt.etb(a.totalSales * .38 / a.receiptsIssued.coerceAtLeast(1))),
    )
    val c = Eirs.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    EirsCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(14.dp), spacing = 8.dp) {
                        Text(t.label, style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1)
                        Text(
                            t.value, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = EirsType.serif(28.sp).copy(color = if (t.red) c.red else c.ink),
                        )
                        Text(t.sub, maxLines = 1, overflow = TextOverflow.Ellipsis, style = EirsType.sans(11.sp).copy(color = c.muted))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = EirsType.sans(15.sp, FontWeight.SemiBold))
}

@Composable
private fun SalesTaxCard(a: SalesAnalytics) {
    val c = Eirs.colors
    var hi by remember(a.range, a.branch) { mutableStateOf<Int?>(null) }
    val point = hi?.let { a.points.getOrNull(it) }
    EirsCard(spacing = 12.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CardTitle("Sales & tax")
            Text(a.range.granularity + " · " + a.range.label.lowercase(), style = EirsType.sans(12.sp).copy(color = c.muted))
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(point?.label ?: a.range.label, style = EirsType.mono(12.sp).copy(color = c.muted), maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val mono = SpanStyle(fontFamily = EirsType.mono(12.sp).fontFamily, fontWeight = FontWeight.Medium, color = c.ink)
                Text(buildAnnotatedString {
                    append("Sales ")
                    withStyle(mono) { append(Fmt.etb(point?.sales ?: a.totalSales)) }
                }, style = EirsType.sans(12.sp).copy(color = c.ink), maxLines = 1)
                Text(buildAnnotatedString {
                    append("Tax ")
                    withStyle(mono) { append(Fmt.etb(point?.tax ?: a.taxCollected)) }
                }, style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1)
            }
        }
        SalesChart(a.points, hi, onScrub = { hi = it })
        ChartTicks(a.points)
    }
}

@Composable
private fun ShareBar(fraction: Double, height: androidx.compose.ui.unit.Dp) {
    val c = Eirs.colors
    val w by animateFloatAsState(fraction.toFloat().coerceIn(0f, 1f), tween(400), label = "bar")
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(c.background)) {
        Box(Modifier.fillMaxWidth(w).fillMaxHeight().clip(RoundedCornerShape(height)).background(c.brand))
    }
}

@Composable
private fun TaxByCodeCard(a: SalesAnalytics) {
    val c = Eirs.colors
    EirsCard(spacing = 14.dp) {
        CardTitle("Tax by code")
        a.taxByCode.forEach { t ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CodeTag(t.code.name)
                        Text(t.label, style = EirsType.sans(13.sp, FontWeight.Medium), maxLines = 1)
                    }
                    MonoText(Fmt.etb(t.tax), maxLines = 1)
                }
                ShareBar(t.barShare, 6.dp)
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Withholding", style = EirsType.sans(13.sp, FontWeight.SemiBold))
                Text("By B2B / B2G buyers", style = EirsType.sans(11.sp).copy(color = c.muted))
            }
            MonoText(Fmt.etb(a.withholding))
        }
    }
}

private val TypeColors = listOf(null, null, Color(0xFF5FA8AE), Color(0xFF9CC9CD), Color(0xFFC9E5E7))

@Composable
private fun TransactionTypeCard(a: SalesAnalytics) {
    val c = Eirs.colors
    fun color(i: Int) = when (i) { 0 -> c.brand; 1 -> c.ink; else -> TypeColors.getOrNull(i) ?: c.tint2 }
    EirsCard(spacing = 12.dp) {
        CardTitle("By transaction type")
        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            a.byTransactionType.forEachIndexed { i, t ->
                if (t.share > 0) Box(Modifier.weight(t.share.toFloat()).fillMaxHeight().background(color(i)))
            }
        }
        Column {
            a.byTransactionType.forEachIndexed { i, t ->
                Row(
                    Modifier.fillMaxWidth().topHairline(c.line).padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color(i)))
                    MonoText(t.type.name, Modifier.width(36.dp), size = 13.sp, weight = FontWeight.Medium)
                    Text(
                        "${(t.share * 100).roundToInt()}% · ${t.documents} docs", modifier = Modifier.weight(1f),
                        style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1,
                    )
                    MonoText(Fmt.etb(t.amount), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun BranchCard(a: SalesAnalytics, selected: Branch?, infos: List<BranchInfo>, onToggle: (Branch) -> Unit) {
    val c = Eirs.colors
    val maxShare = a.byBranch.maxOfOrNull { it.share }?.takeIf { it > 0 } ?: 1.0
    EirsCard(spacing = 10.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CardTitle("By branch")
            Text("Tap to filter", style = EirsType.sans(12.sp).copy(color = c.muted))
        }
        a.byBranch.forEach { b ->
            val on = selected == b.branch
            val alpha by animateFloatAsState(if (selected == null || on) 1f else 0.45f, tween(200), label = "branchAlpha")
            val shape = RoundedCornerShape(12.dp)
            val systems = infos.firstOrNull { it.branch == b.branch }?.sourceSystems ?: b.sourceSystems
            Column(
                Modifier.fillMaxWidth().alpha(alpha).clip(shape).border(1.dp, if (on) c.brand else c.line, shape)
                    .plainClickable { onToggle(b.branch) }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(b.branch.displayName, modifier = Modifier.weight(1f), style = EirsType.sans(14.sp, FontWeight.SemiBold), maxLines = 1)
                    MonoText(Fmt.etb(b.amount), maxLines = 1)
                }
                ShareBar(b.share / maxShare, 5.dp)
                Text("${b.documents} docs · ${Fmt.plural(systems, "source system")}", style = EirsType.sans(11.sp).copy(color = c.muted))
            }
        }
    }
}

private fun whenLabel(item: ActivityItem): String {
    val today = LocalDate.now()
    val d = item.at.toLocalDate()
    return when (d) {
        today -> "Today, " + Fmt.time(item.at)
        today.minusDays(1) -> "Yesterday, " + Fmt.time(item.at)
        else -> Fmt.dateTime(item.at)
    }
}

@Composable
private fun RecentActivityCard(items: List<ActivityItem>, onOpen: (ActivityItem) -> Unit, onViewAll: () -> Unit) {
    val c = Eirs.colors
    EirsCard(padding = PaddingValues(0.dp)) {
        CardTitle("Recent activity", Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp))
        items.forEach { a ->
            val (bg, fg, tag) = when (a.kind) {
                ActivityKind.INVOICE -> Triple(c.tint1, c.brandDark, "INV")
                ActivityKind.RECEIPT -> Triple(c.neutralChip, c.ink, "RCP")
                ActivityKind.CANCELED -> Triple(c.redTint, c.red, "CXL")
            }
            val canceled = a.kind == ActivityKind.CANCELED
            Row(
                Modifier.fillMaxWidth().pressBackground(Color.White, c.background, RoundedCornerShape(0.dp)) { onOpen(a) }
                    .topHairline(c.line).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg), contentAlignment = Alignment.Center) {
                    Text(tag, style = EirsType.mono(11.sp, FontWeight.Medium).copy(color = fg, fontWeight = FontWeight.SemiBold))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(a.party, style = EirsType.sans(14.sp, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${a.number} · ${whenLabel(a)}", style = EirsType.sans(11.sp).copy(color = c.muted), maxLines = 1)
                }
                MonoText(Fmt.etb(a.amount), color = if (canceled) c.muted else c.ink, strike = canceled, maxLines = 1)
            }
        }
        Text(
            "View all invoices →",
            modifier = Modifier.fillMaxWidth().plainClickable(onClick = onViewAll).topHairline(c.line).padding(horizontal = 16.dp, vertical = 14.dp),
            style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.brand),
        )
    }
}
