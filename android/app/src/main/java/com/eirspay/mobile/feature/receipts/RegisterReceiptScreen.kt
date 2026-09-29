package com.eirspay.mobile.feature.receipts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.MobileMoneyProviders
import com.eirspay.domain.model.PaymentMethod
import com.eirspay.domain.model.ReceiptType
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

@Composable
fun RegisterReceiptScreen(onBack: () -> Unit, onRegistered: (String) -> Unit) {
    val vm: RegisterReceiptViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val latestOnRegistered by rememberUpdatedState(onRegistered)
    LaunchedEffect(vm) { vm.registered.collect { latestOnRegistered(it) } }
    val c = Eirs.colors
    val f = s.form

    LoadingHost(s.loading) {
        ViewScaffold(
            header = { ViewHeader("NEW RECEIPT · ${s.branchCaps}", onBack, title = { ViewTitle("Register ", "receipt") }) },
            bottomBar = { RegisterBar(s, vm::submit) },
        ) {
            // 1 · Receipt type
            FormCard {
                StepHeading(1, "Receipt type")
                Grid2(ReceiptType.entries) { t, mod ->
                    val on = f.type == t
                    Column(
                        mod.fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(if (on) c.tint1 else Color.White)
                            .border(1.5.dp, if (on) c.brand else c.line, RoundedCornerShape(12.dp))
                            .plainClickable { vm.pickType(t) }.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(t.displayName, style = EirsType.sans(15.sp, FontWeight.SemiBold))
                        Text(t.description, style = EirsType.sans(11.sp, lineHeight = 1.35).copy(color = c.muted))
                    }
                }
            }

            // 2 · Items or linked invoice
            if (s.needsItems) {
                FormCard {
                    StepHeading(2, "Items") { Text(s.catalogNote, style = EirsType.sans(11.sp).copy(color = c.muted)) }
                    SearchField(f.query, vm::onQuery, "Search catalog")
                    Grid2(s.catalog, alignFill = false) { tile, mod ->
                        Column(
                            mod.pressBackground(if (tile.inCart) c.tint1 else Color.White, c.tint1, RoundedCornerShape(12.dp)) { vm.addItem(tile.code) }
                                .border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(tile.description, style = EirsType.sans(13.sp, FontWeight.Medium, lineHeight = 1.3), maxLines = 1, overflow = ellipsis)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                MonoText(tile.price, Modifier.weight(1f), size = 12.sp, maxLines = 1)
                                MonoText(tile.meta, size = 9.sp, color = c.muted, maxLines = 1)
                            }
                        }
                    }
                    s.lines.forEach { line ->
                        Row(
                            Modifier.fillMaxWidth().topHairline(c.line).padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(line.description, style = EirsType.sans(14.sp, FontWeight.Medium), maxLines = 1, overflow = ellipsis)
                                MonoText(line.total, size = 11.sp, color = c.muted)
                            }
                            QtyStepper(line.quantity, { vm.decrement(line.index) }, { vm.increment(line.index) })
                        }
                    }
                    if (s.lines.isEmpty()) {
                        Text(
                            "Tap items above to add them.",
                            Modifier.fillMaxWidth().dashedBorder(c.line, 12.dp).padding(14.dp),
                            style = EirsType.sans(13.sp).copy(color = c.muted), textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                FormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StepHeading(2, "Linked invoice")
                        Text(s.linkNote, style = EirsType.sans(12.sp).copy(color = c.muted))
                    }
                    s.linkOptions.forEach { o ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (o.selected) c.tint1 else Color.White)
                                .border(1.5.dp, if (o.selected) c.brand else c.line, RoundedCornerShape(12.dp))
                                .plainClickable { vm.pickLink(o.id) }.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(18.dp).clip(CircleShape).border(if (o.selected) 5.dp else 1.5.dp, if (o.selected) c.brand else c.line, CircleShape))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    MonoText(o.number, size = 13.sp, weight = FontWeight.Medium)
                                    MonoText(o.total, size = 12.sp)
                                }
                                Text("${o.party} · ${o.status}", style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1, overflow = ellipsis)
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(s.amountLabel) {
                            FormField(f.amount, vm::onAmount, placeholder = "0.00", mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal)
                        }
                        Text(s.amountHint, style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = c.muted))
                    }
                }
            }

            // 3 · Customer
            FormCard {
                StepHeading(3, "Customer")
                if (s.needsLink && s.linkedNo != null) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = c.muted)) { append("From ${s.linkedNo}:") }
                            append(" ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(s.linkedParty) }
                        },
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.background).padding(12.dp),
                        style = EirsType.sans(14.sp),
                    )
                }
                if (s.needsItems) {
                    Row(
                        Modifier.fillMaxWidth().border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EirsSwitch(f.walkIn, vm::toggleWalkIn)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Walk-in customer", style = EirsType.sans(14.sp, FontWeight.Medium))
                            Text("No details on the receipt", style = EirsType.sans(12.sp).copy(color = c.muted))
                        }
                    }
                    if (!f.walkIn) {
                        FieldLabel("Name") { FormField(f.name, vm::onName, placeholder = "Customer or company") }
                        FieldLabel("Phone or TIN") { FormField(f.phone, vm::onPhone, placeholder = "+251 9… or TIN", mono = true, keyboardType = KeyboardType.Phone) }
                    }
                }
            }

            // 4 · Payment method
            FormCard {
                StepHeading(4, "Payment method")
                Wrap {
                    PaymentMethod.entries.forEach { m ->
                        val on = f.method == m
                        Box(
                            Modifier.height(42.dp).clip(RoundedCornerShape(11.dp)).background(if (on) c.ink else Color.White)
                                .border(1.5.dp, if (on) c.ink else c.line, RoundedCornerShape(11.dp))
                                .plainClickable { vm.pickMethod(m) }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(m.displayName, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = if (on) Color.White else c.ink)) }
                    }
                }
                if (f.method == PaymentMethod.CASH) {
                    FieldLabel("Amount tendered") {
                        FormField(f.tendered, vm::onTendered, placeholder = s.tenderPlaceholder, mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal)
                    }
                    if (s.tenderChips.isNotEmpty()) Wrap {
                        s.tenderChips.forEach { chip ->
                            Box(
                                Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(c.tint1).dashedBorder(c.tint2, 17.dp)
                                    .plainClickable { vm.pickTender(chip.value) }.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) { MonoText(chip.label, size = 12.sp, color = c.brandDark) }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.tint1).padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Change due", style = EirsType.sans(14.sp).copy(color = c.brandDark))
                        MonoText(s.change, size = 14.sp, weight = FontWeight.Medium, color = c.brandDark)
                    }
                }
                if (f.method == PaymentMethod.MOBILE_MONEY) Wrap {
                    MobileMoneyProviders.forEach { p ->
                        val on = f.provider == p
                        Box(
                            Modifier.height(36.dp).clip(CircleShape).background(if (on) c.tint1 else Color.White)
                                .border(1.dp, if (on) c.brand else c.line, CircleShape)
                                .plainClickable { vm.pickProvider(p) }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(p, style = EirsType.sans(13.sp).copy(color = if (on) c.brandDark else c.ink)) }
                    }
                }
                if (f.method != PaymentMethod.CASH) {
                    FieldLabel(s.referenceLabel) { FormField(f.reference, vm::onReference, placeholder = s.referencePlaceholder, mono = true) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RegisterBar(s: RegisterReceiptUiState, onSubmit: () -> Unit) {
    val c = Eirs.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${s.fromLabel} ${s.billTo}", style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1, overflow = ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (s.ready) c.brand else c.pending))
                    Text(s.typeCaps, style = EirsType.sans(11.sp).copy(color = c.muted), maxLines = 1)
                }
            }
            Text(s.grandText, style = EirsType.serif(28.sp).copy(color = if (s.refund) c.red else c.ink), maxLines = 1)
        }
        PrimaryButton("Register with MoR", onSubmit, enabled = s.ready, height = 52.dp, fontSize = 15.sp, horizontalPadding = 18.dp)
    }
}

@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    EirsCard(spacing = 12.dp, content = content)
}

/** Stacked 13sp label over a compact form input. */
@Composable
internal fun FieldLabel(label: String, content: @Composable () -> Unit) {
    LabeledField(label, labelSize = 13.sp, gap = 6.dp, content = content)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun Wrap(spacing: Dp = 6.dp, content: @Composable () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) { content() }
}

/** Two-column grid (CSS `grid-template-columns: 1fr 1fr; gap: 8px`); cells in a row share its height. */
@Composable
internal fun <T> Grid2(items: List<T>, gap: Dp = 8.dp, alignFill: Boolean = true, cell: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().then(if (alignFill) Modifier.height(IntrinsicSize.Min) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                pair.forEach { cell(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QtyStepper(qty: String, onDec: () -> Unit, onInc: () -> Unit) {
    val c = Eirs.colors
    Row(
        Modifier.width(112.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.line, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(38.dp).fillMaxHeight().plainClickable(onClick = onDec), contentAlignment = Alignment.Center) {
            Text("−", style = EirsType.sans(20.sp))
        }
        MonoText(qty, Modifier.weight(1f), size = 15.sp, textAlign = TextAlign.Center)
        Box(Modifier.width(38.dp).fillMaxHeight().plainClickable(onClick = onInc), contentAlignment = Alignment.Center) {
            Text("+", style = EirsType.sans(20.sp))
        }
    }
}
