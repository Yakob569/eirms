package com.eirspay.mobile.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.Branch
import com.eirspay.mobile.feature.home.HomeTab
import com.eirspay.mobile.feature.more.MoreTab
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

interface MainNavigator {
    fun openInvoice(id: String)
    fun openReceipt(id: String)
    fun registerInvoice()
    fun registerReceipt()
    fun openItems()
    fun openWithholding()
    fun startTerminal(branch: Branch)
    fun signedOut()
}

enum class MainTab { HOME, INVOICES, RECEIPTS, MORE }

internal enum class MainSheet { BRANCH, USER, TERMINAL, NEW }

/**
 * Signed-in shell: white top bar (branch filter, environment pill, avatar), a scrolling content
 * column with the tab's eyebrow + serif title, the Register FAB and the bottom tab bar. Sheets and
 * the terminal-mode loader draw above everything.
 */
@Composable
fun MainScreen(
    navigator: MainNavigator,
    invoicesTab: @Composable (showCanceledRequest: Int) -> Unit,
    receiptsTab: @Composable () -> Unit,
) {
    val vm: MainViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var sheet by rememberSaveable { mutableStateOf<MainSheet?>(null) }
    var canceledRequest by rememberSaveable { mutableIntStateOf(0) }
    var termBranch by rememberSaveable { mutableStateOf<Branch?>(null) }

    val nav by rememberUpdatedState(navigator)
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is MainEvent.TerminalStarted -> nav.startTerminal(e.branch)
                MainEvent.SignedOut -> nav.signedOut()
            }
        }
    }

    fun selectTab(t: MainTab) { tab = t; sheet = null }

    val c = Eirs.colors
    LoadingHost(state.loading) {
        Box(Modifier.fillMaxSize().background(c.background)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    tab = tab,
                    branchName = state.selectedBranch?.displayName ?: "All branches",
                    branchSelected = state.selectedBranch != null,
                    environment = state.session?.merchant?.environment ?: "DEV",
                    initials = state.session?.user?.initials ?: "",
                    onBranch = { sheet = MainSheet.BRANCH },
                    onUser = { sheet = MainSheet.USER },
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val scroll = key(tab) { rememberScrollState() }
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scroll)
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
                    ) {
                        PageTitle(tab, state.selectedBranch)
                        key(tab) {
                            Box(Modifier.fillMaxWidth().riseIn(300)) {
                                when (tab) {
                                    MainTab.HOME -> HomeTab(
                                        onOpenInvoice = navigator::openInvoice,
                                        onOpenReceipt = navigator::openReceipt,
                                        onViewAllInvoices = { selectTab(MainTab.INVOICES) },
                                    )
                                    MainTab.INVOICES -> invoicesTab(canceledRequest)
                                    MainTab.RECEIPTS -> receiptsTab()
                                    MainTab.MORE -> MoreTab(
                                        onStartTerminal = { sheet = MainSheet.TERMINAL },
                                        onOpenItems = navigator::openItems,
                                        onOpenWithholding = navigator::openWithholding,
                                        onOpenCanceledInvoices = { canceledRequest++; selectTab(MainTab.INVOICES) },
                                    )
                                }
                            }
                        }
                    }
                    if (tab != MainTab.MORE) {
                        RegisterFab(
                            onClick = { sheet = MainSheet.NEW },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp),
                        )
                    }
                }
                TabBar(tab, ::selectTab)
            }

            BranchSheet(
                visible = sheet == MainSheet.BRANCH,
                selected = state.selectedBranch,
                branches = state.branches,
                onPick = { vm.selectBranch(it); sheet = null },
                onDismiss = { sheet = null },
            )
            UserSheet(
                visible = sheet == MainSheet.USER,
                session = state.session,
                onAccountSettings = { selectTab(MainTab.MORE) },
                onSignOut = { sheet = null; vm.signOut() },
                onDismiss = { sheet = null },
            )
            val effectiveTermBranch = termBranch ?: state.selectedBranch ?: Branch.MAIN
            TerminalSheet(
                visible = sheet == MainSheet.TERMINAL,
                branch = effectiveTermBranch,
                onPick = { termBranch = it },
                onStart = { sheet = null; vm.startTerminal(effectiveTermBranch) },
                onDismiss = { sheet = null },
            )
            RegisterSheet(
                visible = sheet == MainSheet.NEW,
                onInvoice = { sheet = null; navigator.registerInvoice() },
                onReceipt = { sheet = null; navigator.registerReceipt() },
                onDismiss = { sheet = null },
            )
        }
    }
}

@Composable
private fun TopBar(
    tab: MainTab,
    branchName: String,
    branchSelected: Boolean,
    environment: String,
    initials: String,
    onBranch: () -> Unit,
    onUser: () -> Unit,
) {
    val c = Eirs.colors
    Row(
        Modifier.fillMaxWidth().background(Color.White).bottomHairline(c.line).statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (tab != MainTab.MORE) {
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier.height(40.dp).clip(shape).border(1.dp, if (branchSelected) c.brand else c.line, shape)
                    .plainClickable(onClick = onBranch).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LineIcon(EirsIcons.Building, size = 15.dp, tint = c.muted)
                Text(branchName, style = EirsType.sans(14.sp, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                LineIcon(EirsIcons.ChevronDown, size = 14.dp, tint = c.muted)
            }
        } else {
            Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(c.brand), contentAlignment = Alignment.Center) {
                    LineIcon(EirsIcons.Shield, size = 15.dp, strokeWidth = 2.2f, tint = Color.White)
                }
                Text("EIRSPay", style = EirsType.sans(15.sp, FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.height(28.dp).clip(CircleShape).background(c.tint1).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(c.brand))
            Text(environment, style = EirsType.sans(11.sp, FontWeight.SemiBold).copy(color = c.brandDark))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.ink).plainClickable(onClick = onUser),
            contentAlignment = Alignment.Center,
        ) { Text(initials, style = EirsType.sans(13.sp, FontWeight.SemiBold).copy(color = Color.White)) }
    }
}

@Composable
private fun PageTitle(tab: MainTab, branch: Branch?) {
    val scope = (branch?.displayName ?: "All branches").uppercase()
    val (eyebrow, plain, accent) = when (tab) {
        MainTab.HOME -> Triple("OVERVIEW · $scope", "Sales ", "overview.")
        MainTab.INVOICES -> Triple("BROWSE · $scope", "Sales ", "invoices.")
        MainTab.RECEIPTS -> Triple("BROWSE · $scope", "Your ", "receipts.")
        MainTab.MORE -> Triple("MORE · TOOLS & SETUP", "More ", "tools.")
    }
    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(eyebrow)
        SerifTitle(plain, accent, size = 36.sp)
    }
}

@Composable
private fun RegisterFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.height(56.dp)
            .shadow(16.dp, shape, ambientColor = Color(0x8C0B2A33), spotColor = Color(0x8C0B2A33))
            .pressBackground(c.brand, c.brandDark, shape, onClick = onClick)
            .padding(start = 16.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LineIcon(EirsIcons.Plus, size = 20.dp, strokeWidth = 2.4f, tint = Color.White)
        Text("Register", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = Color.White))
    }
}

@Composable
private fun TabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val c = Eirs.colors
    val items = listOf(
        Triple(MainTab.HOME, "Home", EirsIcons.Grid),
        Triple(MainTab.INVOICES, "Invoices", EirsIcons.FileText),
        Triple(MainTab.RECEIPTS, "Receipts", EirsIcons.Receipt),
        Triple(MainTab.MORE, "More", EirsIcons.Sliders),
    )
    Row(
        Modifier.fillMaxWidth().background(Color.White).topHairline(c.line).navigationBarsPadding()
            .padding(top = 8.dp, bottom = 10.dp),
    ) {
        items.forEach { (t, label, icon) ->
            val on = t == selected
            val fg = if (on) c.brandDark else c.muted
            Column(
                Modifier.weight(1f).height(52.dp).plainClickable { onSelect(t) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier.size(52.dp, 30.dp).clip(CircleShape).background(if (on) c.tint1 else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) { LineIcon(icon, size = 20.dp, strokeWidth = 1.9f, tint = fg) }
                Text(label, style = EirsType.sans(11.sp, if (on) FontWeight.SemiBold else FontWeight.Medium).copy(color = fg), maxLines = 1)
            }
        }
    }
}
