package com.eirspay.mobile.feature.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.CatalogItem
import com.eirspay.mobile.ui.designsystem.DashedRule
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.QrCode
import com.eirspay.mobile.ui.designsystem.SegmentOption
import com.eirspay.mobile.ui.designsystem.SegmentedControl
import com.eirspay.mobile.ui.designsystem.SuccessMark
import com.eirspay.mobile.ui.designsystem.bottomHairline
import com.eirspay.mobile.ui.designsystem.dashedBorder
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.pressBackground
import com.eirspay.mobile.ui.designsystem.serifAccent
import com.eirspay.mobile.ui.designsystem.topHairline
import com.eirspay.mobile.ui.designsystem.verificationUrl
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Header + hold to exit
// ---------------------------------------------------------------------------------------------

@Composable
internal fun TerminalHeader(state: TerminalUiState, branch: Branch, tablet: Boolean, onHoldComplete: () -> Unit) {
    val c = Eirs.colors
    val subLine = if (tablet) "Terminal mode · ${branch.displayName} · on shift ${state.shiftFor}" else "${branch.displayName} · ${state.shiftFor}"
    Row(
        Modifier.fillMaxWidth().background(Color.White).bottomHairline(c.line)
            .then(if (tablet) Modifier.padding(horizontal = 20.dp, vertical = 12.dp) else Modifier.statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
            Text(state.initials, style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = Color.White))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.staffName, style = EirsType.sans(16.sp, FontWeight.SemiBold).copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.brand))
                Text(subLine, style = EirsType.sans(12.sp).copy(color = c.brandDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (tablet) {
            Text("${state.staffRole} · ${state.clock}", modifier = Modifier.padding(end = 6.dp), style = EirsType.mono(12.sp).copy(color = c.muted), maxLines = 1)
        }
        HoldToExitButton(onHoldComplete)
    }
}

/** Press and hold for 1.2s while a brand fill sweeps across; releasing early cancels. */
@Composable
private fun HoldToExitButton(onComplete: () -> Unit) {
    val c = Eirs.colors
    val fill = remember { Animatable(0f) }
    var holding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val complete by rememberUpdatedState(onComplete)
    Box(
        Modifier.height(46.dp).clip(RoundedCornerShape(13.dp)).background(c.ink)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    holding = true
                    val job = scope.launch {
                        fill.animateTo(1f, tween(((1f - fill.value) * 1200).toInt().coerceAtLeast(1), easing = LinearEasing))
                        holding = false
                        fill.snapTo(0f)
                        complete()
                    }
                    tryAwaitRelease()
                    if (job.isActive) {
                        job.cancel()
                        holding = false
                        scope.launch { fill.animateTo(0f, tween(150)) }
                    }
                })
            },
    ) {
        Box(Modifier.matchParentSize().graphicsLayer {
            scaleX = fill.value
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
        }.background(c.brand))
        Row(
            Modifier.fillMaxHeight().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LineIcon(EirsIcons.Lock, size = 16.dp, strokeWidth = 2f, tint = Color.White)
            Text(if (holding) "Keep holding…" else "Hold to exit", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = Color.White), maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------------------------

@Composable
internal fun FindItemField(value: String, onChange: (String) -> Unit) {
    val c = Eirs.colors
    Row(
        Modifier.width(200.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LineIcon(EirsIcons.Search, size = 17.dp, tint = c.muted)
        val style = EirsType.sans(16.sp).copy(color = c.ink)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("Find item", style = style.copy(color = c.placeholder), maxLines = 1)
            BasicTextField(value, onChange, singleLine = true, textStyle = style, cursorBrush = SolidColor(c.brand), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ItemTile(item: CatalogItem, qty: Int, height: Dp, onClick: () -> Unit) {
    val c = Eirs.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, tween(120), label = "tileScale")
    val inCart = qty > 0
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().height(height).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape).background(if (inCart) c.tint1 else Color.White)
            .border(1.5.dp, if (inCart) c.brand else c.line, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(13.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                item.description, modifier = Modifier.padding(end = 30.dp),
                style = EirsType.sans(15.sp, FontWeight.Medium, lineHeight = 1.25).copy(color = c.ink),
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(Fmt.f2(item.unitPrice), style = EirsType.mono(16.sp, FontWeight.Medium).copy(color = c.ink), maxLines = 1)
                Text(item.taxCode.name, style = EirsType.mono(10.sp).copy(color = c.muted), maxLines = 1)
            }
        }
        if (inCart) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).defaultMinSize(minWidth = 26.dp).height(26.dp)
                    .clip(RoundedCornerShape(999.dp)).background(c.brand).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text(qty.toString(), style = EirsType.mono(12.sp).copy(color = Color.White)) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Cart (tablet aside + phone sheet body)
// ---------------------------------------------------------------------------------------------

@Composable
internal fun CartPanel(state: TerminalUiState, tablet: Boolean, actions: TerminalActions, modifier: Modifier) {
    val c = Eirs.colors
    val hPad = if (tablet) 20.dp else 18.dp
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(start = hPad, end = hPad, top = if (tablet) 16.dp else 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(serifAccent("Current ", "sale"), style = EirsType.serif(28.sp).copy(color = c.ink))
            if (!tablet || state.hasCart) {
                Text(
                    "Clear", modifier = Modifier.plainClickable(onClick = actions.clearCart).padding(8.dp),
                    style = EirsType.sans(14.sp).copy(color = c.muted),
                )
            } else Spacer(Modifier.height(36.dp))
        }
        Column(Modifier.padding(start = hPad, end = hPad, bottom = if (tablet) 12.dp else 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedControl(
                options = BuyerMode.values().toList().map { SegmentOption(it, it.label) },
                selected = state.buyer, onSelect = actions.pickBuyer, height = 40.dp, fontSize = 14.sp,
            )
            if (state.isBusiness) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TerminalInput(
                        state.tin, actions.setTin, if (tablet) "Buyer TIN (10 digits)" else "Buyer TIN",
                        KeyboardType.Number, Modifier.weight(1f),
                    )
                    Box(
                        Modifier.height(46.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, c.brand, RoundedCornerShape(10.dp))
                            .plainClickable(onClick = actions.lookup).padding(horizontal = 14.dp).alpha(if (state.lookingUp) .6f else 1f),
                        contentAlignment = Alignment.Center,
                    ) { Text("Look up", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.brand)) }
                }
                state.business?.let { b ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.tint1).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(b.name, modifier = Modifier.weight(1f, fill = false), style = EirsType.sans(13.sp, FontWeight.SemiBold).copy(color = c.brandDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("B2B", style = EirsType.sans(13.sp).copy(color = c.brandDark))
                    }
                }
            }
            if (state.buyer == BuyerMode.PHONE) {
                TerminalInput(state.phone, actions.setPhone, "+251 9… for SMS receipt", KeyboardType.Phone, Modifier.fillMaxWidth())
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).topHairline(c.line).verticalScroll(rememberScrollState())) {
            state.cart.forEachIndexed { i, l ->
                val lt = state.totals.lines.getOrNull(i)
                CartLineRow(l, lt?.total?.let(Fmt::f2) ?: "", tablet, onDec = { actions.dec(i) }, onInc = { actions.inc(i) })
            }
            if (state.cart.isEmpty() && tablet) {
                Box(Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("Tap items to start a sale.", style = EirsType.sans(14.sp).copy(color = c.muted), textAlign = TextAlign.Center)
                }
            }
        }
        CartFooter(state, tablet, actions, Modifier.fillMaxWidth().topHairline(c.line).then(if (tablet) Modifier else Modifier.navigationBarsPadding().imePadding()).padding(start = hPad, end = hPad, top = 12.dp, bottom = if (tablet) 16.dp else 30.dp))
    }
}

@Composable
private fun CartLineRow(line: CartLine, total: String, tablet: Boolean, onDec: () -> Unit, onInc: () -> Unit) {
    val c = Eirs.colors
    val hPad = if (tablet) 20.dp else 18.dp
    val gap = if (tablet) 10.dp else 8.dp
    Row(
        Modifier.fillMaxWidth().bottomHairline(c.line).padding(horizontal = hPad, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(line.item.description, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Fmt.f2(line.item.unitPrice) + " · " + line.item.taxCode.name, style = EirsType.mono(11.sp).copy(color = c.muted), maxLines = 1)
        }
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier.width(if (tablet) 124.dp else 116.dp).height(44.dp).clip(shape).border(1.dp, c.line, shape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bw = if (tablet) 42.dp else 40.dp
            Box(Modifier.width(bw).fillMaxHeight().pressBackground(Color.Transparent, c.tint1, RoundedCornerShape(0.dp), onClick = onDec), contentAlignment = Alignment.Center) {
                Text("−", style = EirsType.sans(22.sp).copy(color = c.ink))
            }
            Text(line.qty.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = EirsType.mono(16.sp).copy(color = c.ink))
            Box(Modifier.width(bw).fillMaxHeight().pressBackground(Color.Transparent, c.tint1, RoundedCornerShape(0.dp), onClick = onInc), contentAlignment = Alignment.Center) {
                Text("+", style = EirsType.sans(22.sp).copy(color = c.ink))
            }
        }
        Text(
            total, modifier = Modifier.width(if (tablet) 84.dp else 76.dp), textAlign = TextAlign.End,
            style = EirsType.mono(if (tablet) 13.sp else 12.sp).copy(color = c.ink), maxLines = 1,
        )
    }
}

@Composable
private fun CartFooter(state: TerminalUiState, tablet: Boolean, actions: TerminalActions, modifier: Modifier) {
    val c = Eirs.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val st = EirsType.sans(13.sp).copy(color = c.muted)
            Text("Taxable " + Fmt.f2(state.totals.subtotal), style = st)
            Text("Tax " + Fmt.f2(state.totals.taxTotal), style = st)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TerminalPay.values().forEach { p ->
                val on = state.pay == p
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier.weight(1f).height(if (tablet) 48.dp else 46.dp).clip(shape).background(if (on) c.ink else Color.White)
                        .border(1.5.dp, if (on) c.ink else c.line, shape).plainClickable { actions.pickPay(p) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(p.label, style = EirsType.sans(if (tablet) 13.sp else 12.sp, FontWeight.SemiBold).copy(color = if (on) Color.White else c.ink), maxLines = 1)
                }
            }
        }
        if (state.pay == TerminalPay.CASH && state.tenderOptions.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.tenderOptions.forEachIndexed { i, v ->
                    val on = state.isTendered(v)
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp)).background(if (on) c.tint2 else c.tint1)
                            .dashedBorder(if (on) c.brand else c.tint2, 10.dp).plainClickable { actions.pickTender(v) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (i == 0) "Exact" else Fmt.compact(v), style = EirsType.mono(12.sp).copy(color = c.brandDark), maxLines = 1)
                    }
                }
            }
        }
        val op by animateFloatAsState(if (state.ok) 1f else .45f, tween(200), label = "chargeOp")
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().height(if (tablet) 68.dp else 64.dp).alpha(op)
                .pressBackground(c.brand, c.brandDark, shape, enabled = state.ok, onClick = actions.charge)
                .padding(horizontal = if (tablet) 20.dp else 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Charge", style = EirsType.sans(if (tablet) 17.sp else 16.sp, FontWeight.SemiBold).copy(color = Color.White))
                Text(state.chargeSub, style = EirsType.sans(if (tablet) 12.sp else 11.sp).copy(color = Color.White.copy(alpha = .85f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Text(Fmt.etb(state.total), style = EirsType.serif(if (tablet) 30.sp else 28.sp).copy(color = Color.White), maxLines = 1)
        }
    }
}

@Composable
private fun TerminalInput(value: String, onChange: (String) -> Unit, placeholder: String, keyboard: KeyboardType, modifier: Modifier) {
    val c = Eirs.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) c.brand else c.line, label = "tIn")
    val shape = RoundedCornerShape(10.dp)
    val style = EirsType.mono(16.sp).copy(color = c.ink)
    BasicTextField(
        value = value, onValueChange = onChange, modifier = modifier, singleLine = true, textStyle = style,
        cursorBrush = SolidColor(c.brand), interactionSource = source,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().height(46.dp).clip(shape).background(if (focused) Color.White else c.background)
                    .border(1.dp, border, shape).padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = c.placeholder), maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Success view
// ---------------------------------------------------------------------------------------------

@Composable
internal fun SaleView(sale: TerminalSale, servedBy: String, tablet: Boolean, onNewSale: () -> Unit) {
    val c = Eirs.colors
    val summary: @Composable (Modifier) -> Unit = { m ->
        Column(m.padding(if (tablet) PaddingValues(horizontal = 56.dp, vertical = 40.dp) else PaddingValues(horizontal = 20.dp, vertical = 24.dp)), verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
            SuccessMark(EirsIcons.Check, size = 76.dp, iconSize = 36.dp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(serifAccent("Sale ", "registered."), style = EirsType.serif(if (tablet) 56.sp else 44.sp).copy(color = c.ink))
                Text("MoR signed ${sale.number} at ${sale.time} · ${sale.pay.label}", style = EirsType.sans(15.sp).copy(color = c.muted))
            }
            if (sale.hasChange) {
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(c.ink).padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Change due", modifier = Modifier.alignByBaseline(), style = EirsType.sans(15.sp).copy(color = Color.White.copy(alpha = .75f)))
                    Text(Fmt.etb(sale.change), modifier = Modifier.alignByBaseline(), style = EirsType.serif(44.sp).copy(color = Color.White))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).widthIn(max = 260.dp).height(62.dp).pressBackground(c.brand, c.brandDark, RoundedCornerShape(16.dp), onClick = onNewSale),
                    contentAlignment = Alignment.Center,
                ) { Text("+ New sale", style = EirsType.sans(17.sp, FontWeight.SemiBold).copy(color = Color.White)) }
                Box(
                    Modifier.height(62.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                        .border(1.5.dp, c.line, RoundedCornerShape(16.dp)).plainClickable { /* printing not wired yet */ }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Print", style = EirsType.sans(16.sp, FontWeight.Medium).copy(color = c.ink)) }
            }
        }
    }
    val swallow = Modifier.fillMaxSize().background(c.background).plainClickable { }
    if (tablet) {
        Row(swallow) {
            summary(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
            Box(Modifier.width(1.dp).fillMaxHeight().background(c.line))
            ReceiptPanel(sale, servedBy, Modifier.width(379.dp).fillMaxHeight().verticalScroll(rememberScrollState()))
        }
    } else {
        Column(swallow.verticalScroll(rememberScrollState())) {
            summary(Modifier.fillMaxWidth())
            ReceiptPanel(sale, servedBy, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReceiptPanel(sale: TerminalSale, servedBy: String, modifier: Modifier) {
    val c = Eirs.colors
    val base = EirsType.sans(13.sp).copy(color = c.ink)
    Column(
        modifier.background(Color.White).padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 30.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sale.lines.forEach { l ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${l.qty} × ${l.name}", modifier = Modifier.weight(1f), style = base)
                        Text(l.total, style = EirsType.mono(13.sp).copy(color = c.ink))
                    }
                }
            }
            DashedRule(color = c.ink)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = EirsType.sans(16.sp, FontWeight.Bold).copy(color = c.ink))
            Text(Fmt.etb(sale.total), style = EirsType.mono(16.sp, FontWeight.Medium).copy(color = c.ink, fontWeight = FontWeight.Bold))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Buyer · served by", modifier = Modifier.weight(1f), style = base.copy(color = c.muted))
            Text("${sale.buyer} · $servedBy", style = base.copy(color = c.muted), textAlign = TextAlign.End)
        }
        Column(Modifier.fillMaxWidth()) {
            DashedRule(color = c.ink)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                QrCode(verificationUrl(sale.irn), Modifier.size(96.dp))
                Text("IRN ${sale.irn}", modifier = Modifier.weight(1f), style = EirsType.mono(10.sp).copy(color = c.muted, lineHeight = 15.sp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Exit confirmation
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ExitConfirm(state: TerminalUiState, tablet: Boolean, actions: TerminalActions) {
    val c = Eirs.colors
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(state.confirmOpen, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Box(Modifier.fillMaxSize().background(c.ink.copy(alpha = .45f)).plainClickable(onClick = actions.closeExit))
        }
        AnimatedVisibility(
            state.confirmOpen,
            modifier = Modifier.align(if (tablet) Alignment.Center else Alignment.BottomCenter),
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { if (tablet) 30 else it / 8 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { if (tablet) 30 else it / 8 },
        ) {
            val shape = if (tablet) RoundedCornerShape(22.dp) else RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            Column(
                Modifier.then(if (tablet) Modifier.width(480.dp) else Modifier.fillMaxWidth())
                    .shadow(40.dp, shape, ambientColor = c.ink.copy(alpha = .6f), spotColor = c.ink.copy(alpha = .6f))
                    .clip(shape).background(Color.White).plainClickable { }
                    .then(if (tablet) Modifier else Modifier.navigationBarsPadding())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Exit terminal mode?", style = EirsType.serif(30.sp).copy(color = c.ink))
                    Text(
                        "The app unlocks back to your full workspace. This shift's cart and buyer details are cleared.",
                        style = EirsType.sans(14.sp, lineHeight = 1.45).copy(color = c.muted),
                    )
                }
                val stats = listOf("Sales" to state.sales.size.toString(), "Total" to Fmt.etb(state.shiftTotal), "Cash" to Fmt.etb(state.cashTotal))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(14.dp)).border(1.dp, c.line, RoundedCornerShape(14.dp))) {
                    stats.forEachIndexed { i, (l, v) ->
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(l, style = EirsType.sans(11.sp).copy(color = c.muted))
                            Text(v, style = EirsType.serif(22.sp).copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (i < stats.lastIndex) Box(Modifier.width(1.dp).fillMaxHeight().background(c.line))
                    }
                }
                if (state.hasCart) {
                    Text(
                        "The open sale (${state.cartCount}) hasn't been registered and will be discarded.",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.amberTint).padding(horizontal = 14.dp, vertical = 12.dp),
                        style = EirsType.sans(14.sp).copy(color = c.amberInk),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.line, RoundedCornerShape(14.dp))
                            .plainClickable(onClick = actions.closeExit),
                        contentAlignment = Alignment.Center,
                    ) { Text("Keep selling", style = EirsType.sans(15.sp, FontWeight.Medium).copy(color = c.ink)) }
                    Box(
                        Modifier.weight(1.3f).height(56.dp).clip(RoundedCornerShape(14.dp)).background(c.ink).plainClickable(onClick = actions.exit),
                        contentAlignment = Alignment.Center,
                    ) { Text("Exit terminal", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = Color.White)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Loader: brand tile inside a spinning ring (POS `p-spin`)
// ---------------------------------------------------------------------------------------------

@Composable
internal fun SpinnerLoader(title: String, subtitle: String) {
    val c = Eirs.colors
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    Column(
        Modifier.fillMaxSize().background(Color.White.copy(alpha = .55f)).plainClickable { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize().graphicsLayer { rotationZ = angle }) {
                val sw = 2.dp.toPx()
                val r = CornerRadius(28.dp.toPx() - sw / 2)
                val tl = Offset(sw / 2, sw / 2)
                val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                drawRoundRect(c.tint2, tl, sz, r, style = Stroke(sw))
                // CSS border-top-color: the top edge up to the corner diagonals.
                val top = Path().apply {
                    moveTo(size.width / 2, size.height / 2); lineTo(0f, 0f); lineTo(size.width, 0f); close()
                }
                clipPath(top) { drawRoundRect(c.brand, tl, sz, r, style = Stroke(sw)) }
            }
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)).background(c.brand), contentAlignment = Alignment.Center) {
                LineIcon(EirsIcons.Shield, size = 34.dp, strokeWidth = 1.8f, tint = Color.White)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, textAlign = TextAlign.Center, style = EirsType.serif(32.sp).copy(fontStyle = FontStyle.Italic, color = c.ink))
            Text(subtitle, textAlign = TextAlign.Center, style = EirsType.sans(14.sp).copy(color = c.muted))
        }
    }
}
