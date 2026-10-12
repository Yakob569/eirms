package com.eirspay.mobile.feature.terminal

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.Branch
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.pressBackground
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/**
 * Terminal mode: a full-screen, locked point-of-sale for sales staff on phones and counter tablets.
 * Back is swallowed; the only way out is "Hold to exit" → confirmation, which wipes all state.
 */
@Composable
fun TerminalScreen(branch: Branch, onExit: () -> Unit) {
    val vm: TerminalViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val latestExit by rememberUpdatedState(onExit)

    BackHandler(enabled = true) { vm.back() }

    LaunchedEffect(state.exited) {
        if (state.exited) {
            vm.consumeExit()
            latestExit()
        }
    }

    TerminalContent(
        state = state,
        branch = branch,
        actions = TerminalActions(
            pickCategory = vm::pickCategory, setQuery = vm::setQuery, add = vm::add, inc = vm::inc, dec = vm::dec,
            clearCart = vm::clearCart, openCart = vm::openCart, closeCart = vm::closeCart, pickBuyer = vm::pickBuyer,
            setTin = vm::setTin, lookup = vm::lookup, setPhone = vm::setPhone, pickPay = vm::pickPay,
            pickTender = vm::pickTender, charge = { vm.charge(branch) }, newSale = vm::newSale,
            holdComplete = vm::holdComplete, closeExit = vm::closeExit, exit = vm::exit,
        ),
    )
}

internal class TerminalActions(
    val pickCategory: (String) -> Unit,
    val setQuery: (String) -> Unit,
    val add: (com.eirspay.domain.model.CatalogItem) -> Unit,
    val inc: (Int) -> Unit,
    val dec: (Int) -> Unit,
    val clearCart: () -> Unit,
    val openCart: () -> Unit,
    val closeCart: () -> Unit,
    val pickBuyer: (BuyerMode) -> Unit,
    val setTin: (String) -> Unit,
    val lookup: () -> Unit,
    val setPhone: (String) -> Unit,
    val pickPay: (TerminalPay) -> Unit,
    val pickTender: (java.math.BigDecimal) -> Unit,
    val charge: () -> Unit,
    val newSale: () -> Unit,
    val holdComplete: () -> Unit,
    val closeExit: () -> Unit,
    val exit: () -> Unit,
)

@Composable
private fun TerminalContent(state: TerminalUiState, branch: Branch, actions: TerminalActions) {
    val c = Eirs.colors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.background)) {
        val tablet = maxWidth >= 840.dp
        val blur by animateDpAsState(if (state.busy != null) 6.dp else 0.dp, label = "termBlur")

        Box(Modifier.fillMaxSize().then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(5.dp).background(c.brand))
                TerminalHeader(state, branch, tablet, onHoldComplete = actions.holdComplete)
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Row(Modifier.fillMaxSize()) {
                        ItemsPane(state, tablet, actions, Modifier.weight(1f).fillMaxHeight())
                        if (tablet) {
                            Box(Modifier.width(1.dp).fillMaxHeight().background(c.line))
                            CartPanel(state, tablet = true, actions = actions, modifier = Modifier.width(389.dp).fillMaxHeight().background(Color.White))
                        }
                    }
                    SaleOverlay(state, tablet, actions)
                }
            }

            if (!tablet) {
                // Floating "Review sale" bar
                AnimatedVisibility(
                    state.hasCart && !state.cartOpen && state.sale == null,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 6 },
                    exit = fadeOut(tween(150)),
                ) {
                    ReviewSaleBar(state, onClick = actions.openCart)
                }
                PhoneCartSheet(state, actions)
            }

            ExitConfirm(state, tablet, actions)
        }

        AnimatedVisibility(state.busy != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            val msg = state.busy
            val last = remember { mutableStateOf(msg) }
            if (msg != null) last.value = msg
            last.value?.let { SpinnerLoader(it.title, it.subtitle) }
        }
    }
}

@Composable
private fun SaleOverlay(state: TerminalUiState, tablet: Boolean, actions: TerminalActions) {
    val sale = state.sale
    AnimatedVisibility(sale != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
        val last = remember { mutableStateOf(sale) }
        if (sale != null) last.value = sale
        last.value?.let { SaleView(it, state.firstName, tablet, onNewSale = actions.newSale) }
    }
}

@Composable
private fun ItemsPane(state: TerminalUiState, tablet: Boolean, actions: TerminalActions, modifier: Modifier) {
    val c = Eirs.colors
    Column(
        modifier.padding(if (tablet) PaddingValues(horizontal = 18.dp, vertical = 16.dp) else PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { cat ->
                    val on = state.category == cat
                    Box(
                        Modifier.height(44.dp).clip(RoundedCornerShape(999.dp))
                            .background(if (on) c.ink else Color.White)
                            .border(1.dp, if (on) c.ink else c.line, RoundedCornerShape(999.dp))
                            .plainClickable { actions.pickCategory(cat) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(cat, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = if (on) Color.White else c.ink), maxLines = 1)
                    }
                }
            }
            if (tablet) FindItemField(state.query, actions.setQuery)
        }
        val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (tablet) 4 else 2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = if (tablet) 0.dp else 110.dp + navBottom),
        ) {
            items(state.tiles, key = { it.code }) { item ->
                ItemTile(item, state.qtyOf(item.code), if (tablet) 124.dp else 112.dp) { actions.add(item) }
            }
        }
    }
}

@Composable
private fun ReviewSaleBar(state: TerminalUiState, onClick: () -> Unit) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 28.dp).fillMaxWidth().height(66.dp)
            .shadow(18.dp, shape, ambientColor = c.ink.copy(alpha = .6f), spotColor = c.ink.copy(alpha = .6f))
            .pressBackground(c.brand, c.brandDark, shape, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.defaultMinSize(minWidth = 30.dp).height(30.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = .2f)).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) { Text(state.count.toString(), style = EirsType.mono(14.sp).copy(color = Color.White)) }
        Text("Review sale", modifier = Modifier.weight(1f), style = EirsType.sans(16.sp, FontWeight.SemiBold).copy(color = Color.White))
        Text(Fmt.etb(state.total), style = EirsType.serif(26.sp).copy(color = Color.White), maxLines = 1)
    }
}

@Composable
private fun PhoneCartSheet(state: TerminalUiState, actions: TerminalActions) {
    val c = Eirs.colors
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(state.cartOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(c.ink.copy(alpha = .4f)).plainClickable(onClick = actions.closeCart))
        }
        AnimatedVisibility(
            state.cartOpen,
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 66.dp),
            enter = slideInVertically(tween(280, easing = CubicBezierEasing(.2f, .8f, .2f, 1f))) { it },
            exit = slideOutVertically(tween(220)) { it },
        ) {
            Column(
                Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(Color.White).plainClickable { },
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 4.dp).size(40.dp, 5.dp)
                        .clip(RoundedCornerShape(5.dp)).background(c.line),
                )
                CartPanel(state, tablet = false, actions = actions, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
