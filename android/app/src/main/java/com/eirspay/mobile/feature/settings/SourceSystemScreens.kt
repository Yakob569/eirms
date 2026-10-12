package com.eirspay.mobile.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.SettingsCatalog
import com.eirspay.domain.usecase.SettingsRules
import com.eirspay.mobile.ui.designsystem.ChipGroup
import com.eirspay.mobile.ui.designsystem.ChipMultiSelect
import com.eirspay.mobile.ui.designsystem.ChipSize
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.EirsSwitch
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.StatusPill
import com.eirspay.mobile.ui.designsystem.TextLink
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.ViewScaffold
import com.eirspay.mobile.ui.designsystem.ViewTitle
import com.eirspay.mobile.ui.designsystem.pressBackground
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

private val ListBody = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)

// ---------------------------------------------------------------------------------------------
// List (with the merchant-wide excise switch on top)
// ---------------------------------------------------------------------------------------------

@Composable
fun SourceSystemsScreen(onBack: () -> Unit, onOpenSourceSystem: (String) -> Unit) {
    val vm: SettingsListViewModel = hiltViewModel()
    val s by vm.settings.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenSourceSystem)
    LaunchedEffect(vm) { vm.created.collect { open(it) } }
    val c = Eirs.colors

    ViewScaffold(
        header = {
            SettingsHeader("SETTINGS · " + plural(s.sourceSystems.size, "system").uppercase(), "Source ", "systems", onBack) {
                HeaderPlusButton { vm.addSourceSystem() }
            }
        },
        bodyPadding = ListBody,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.tint1).border(1.dp, c.tint2, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Merchant excise", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = c.ink))
                        StatusPill(if (s.exciseEnabled) "Enabled" else "Not enabled", background = Color.White)
                    }
                    Text("Business-wide", style = EirsType.sans(12.sp).copy(color = c.brandDark))
                }
                EirsSwitch(s.exciseEnabled, onToggle = { vm.setExcise(!s.exciseEnabled) })
            }
            Text(
                if (s.exciseEnabled) "EXCISE is merged into every source system’s tax types and can be granted to API keys."
                else "Applies to your whole business, not per till. When on, EXCISE joins every source system’s tax types.",
                style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = c.brandDark),
            )
        }
        Text(
            plural(s.sourceSystems.size, "source system") + " · " + s.sourceSystems.count { it.verification.label == "Verified" } + " verified",
            modifier = Modifier.padding(horizontal = 4.dp), style = EirsType.sans(13.sp).copy(color = c.muted),
        )
        s.sourceSystems.forEach { x ->
            val taxes = SettingsRules.taxTypes(x, s.exciseEnabled)
            SettingsListCard(onClick = { onOpenSourceSystem(x.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardTitle(x.label, Modifier.weight(1f, fill = false))
                        Text(
                            x.type, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).border(1.dp, c.line, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp),
                            style = EirsType.mono(10.sp).copy(color = c.muted),
                        )
                    }
                    StatusPill(x.verification.label)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.branch(x.branchId)?.name ?: "—", style = EirsType.sans(13.sp).copy(color = c.muted))
                    StatusPill(if (x.active) "Active" else "Inactive")
                }
                ChipMultiSelect(taxes, taxes, onToggle = null, derived = { it == SettingsCatalog.EXCISE }, size = ChipSize.Small)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detail
// ---------------------------------------------------------------------------------------------

@Composable
fun SourceSystemDetailScreen(id: String, onBack: () -> Unit, onOpenBranch: (String) -> Unit) {
    val vm: SourceSystemDetailViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val s by vm.state.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    LaunchedEffect(vm) { vm.closed.collect { back() } }
    val c = Eirs.colors
    val x = s.draft
    if (x == null) {
        ViewScaffold(header = { ViewHeader("SOURCE SYSTEM", onBack, title = { ViewTitle("Source system") }) }) {}
        return
    }
    val branch = s.settings.branch(x.branchId)
    val excise = s.settings.exciseEnabled
    val taxOptions = if (excise) SettingsCatalog.taxTypes + SettingsCatalog.EXCISE else SettingsCatalog.taxTypes

    LoadingHost(s.loading) {
        ViewScaffold(
            header = { SettingsHeader(x.type + " · " + (branch?.name ?: "").uppercase(), x.label, "", onBack) },
            bottomBar = { DeleteSaveFooter(s.deleteBlocker, vm::delete, vm::save) },
        ) {
            EirsCard(spacing = 12.dp) {
                SettingsField("Label", x.label, { v -> vm.edit { it.copy(label = v) } })
                SwitchRow(
                    title = "Status", note = if (x.active) "Can issue documents" else "Paused",
                    checked = x.active, onToggle = { vm.edit { it.copy(active = !it.active) } },
                ) {
                    if (branch != null) TextLink(branch.name + " →", { onOpenBranch(branch.id) }, size = 12.sp)
                }
            }

            SectionLabel("SYSTEM TYPE")
            EirsCard { ChipMultiSelect(SettingsCatalog.systemTypes, listOf(x.type), onToggle = { t -> vm.edit { it.copy(type = t) } }) }

            SectionLabel("BRANCH")
            EirsCard(spacing = 8.dp) {
                ChipMultiSelect(
                    s.settings.branches, listOfNotNull(branch), onToggle = { b -> vm.edit { it.copy(branchId = b.id) } },
                    label = { it.name }, allowed = { SettingsRules.canHostSourceSystem(it, x) }, mono = false,
                )
                Text("Inactive branches can’t take source systems.", style = EirsType.sans(12.sp).copy(color = c.muted))
            }

            SectionLabel("STAFF")
            EirsCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsField("Cashier", x.cashier, { v -> vm.edit { it.copy(cashier = v) } }, modifier = Modifier.weight(1f), placeholder = "Optional", fontSize = 15)
                    SettingsField("Sales person", x.salesPerson, { v -> vm.edit { it.copy(salesPerson = v) } }, modifier = Modifier.weight(1f), placeholder = "Optional", fontSize = 15)
                }
            }

            EirsCard {
                ChipGroup(
                    "Tax types", taxOptions, x.taxTypes,
                    onToggle = { t -> if (t != SettingsCatalog.EXCISE) vm.edit { it.copy(taxTypes = if (t in it.taxTypes) it.taxTypes - t else it.taxTypes + t) } },
                    derived = { it == SettingsCatalog.EXCISE },
                    caption = if (excise) "EXCISE is derived from your merchant excise status, not set here."
                    else "EXCISE is added here automatically once excise is enabled for your business.",
                )
            }

            SectionLabel("VERIFICATION")
            EirsCard(spacing = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill(x.verification.label)
                    Text(x.verificationNote, style = EirsType.sans(12.sp, lineHeight = 1.4).copy(color = c.muted))
                }
                SettingsField("HSN for the probe", s.hsn, vm::setHsn, placeholder = "8471", mono = true, keyboardType = KeyboardType.Number)
                Row(
                    Modifier.fillMaxWidth().height(46.dp).border(1.5.dp, c.brand, RoundedCornerShape(12.dp))
                        .pressBackground(Color.White, c.tint1, RoundedCornerShape(12.dp), onClick = vm::verify).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Run verification", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.brand))
                    LineIcon(EirsIcons.ArrowRight, size = 16.dp, tint = c.brand)
                }
            }
            KvCard(
                listOf("System ID" to x.systemId, "Seller TIN" to s.settings.seller.tin, "Last document no." to x.lastDocument),
                monoValues = true,
            )
        }
    }
}
