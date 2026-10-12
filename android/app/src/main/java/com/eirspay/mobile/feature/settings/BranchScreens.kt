package com.eirspay.mobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.BranchAddress
import com.eirspay.domain.model.SettingsCatalog
import com.eirspay.domain.usecase.SettingsRules
import com.eirspay.mobile.ui.designsystem.ChipGroup
import com.eirspay.mobile.ui.designsystem.ChipMultiSelect
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.SelectChip
import com.eirspay.mobile.ui.designsystem.StatusPill
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.ViewScaffold
import com.eirspay.mobile.ui.designsystem.ViewTitle
import com.eirspay.mobile.ui.designsystem.bottomHairline
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.pressBackground
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

private val ListBody = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)

private fun <T> List<T>.toggled(v: T): List<T> = if (v in this) this - v else this + v

// ---------------------------------------------------------------------------------------------
// List
// ---------------------------------------------------------------------------------------------

@Composable
fun BranchesScreen(onBack: () -> Unit, onOpenBranch: (String) -> Unit) {
    val vm: SettingsListViewModel = hiltViewModel()
    val s by vm.settings.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenBranch)
    LaunchedEffect(vm) { vm.created.collect { open(it) } }
    val c = Eirs.colors

    ViewScaffold(
        header = {
            SettingsHeader("SETTINGS · ${s.branches.size} BRANCHES", "", "Branches", onBack) { HeaderPlusButton { vm.addBranch() } }
        },
        bodyPadding = ListBody,
    ) {
        Text(
            "${s.branches.count { it.active }} of ${s.branches.size} active",
            modifier = Modifier.padding(horizontal = 4.dp), style = EirsType.sans(13.sp).copy(color = c.muted),
        )
        s.branches.forEach { b ->
            val systems = s.sourceSystemsOf(b.id).size
            SettingsListCard(onClick = { onOpenBranch(b.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CardTitle(b.name, Modifier.weight(1f, fill = false), size = 16)
                        if (b.isMain) StatusPill("Main branch")
                    }
                    StatusPill(if (b.active) "Active" else "Inactive")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonoText(b.code, size = 13.sp, color = c.muted)
                    Text(
                        b.manager ?: "Unassigned", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End, style = EirsType.sans(13.sp).copy(color = c.muted),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${b.address.city.ifBlank { "—" }} · ${b.address.region.ifBlank { "—" }}", style = EirsType.sans(12.sp).copy(color = c.ink))
                    Text(plural(systems, "source system"), style = EirsType.sans(12.sp).copy(color = c.muted))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detail
// ---------------------------------------------------------------------------------------------

private data class AddressSpec(val label: String, val get: (BranchAddress) -> String, val set: (BranchAddress, String) -> BranchAddress)

private val AddressSpecs = listOf(
    AddressSpec("City", { it.city }, { a, v -> a.copy(city = v) }),
    AddressSpec("Sub-city", { it.subCity }, { a, v -> a.copy(subCity = v) }),
    AddressSpec("Wereda", { it.wereda }, { a, v -> a.copy(wereda = v) }),
    AddressSpec("Kebele", { it.kebele }, { a, v -> a.copy(kebele = v) }),
    AddressSpec("House no.", { it.houseNumber }, { a, v -> a.copy(houseNumber = v) }),
    AddressSpec("Locality", { it.locality }, { a, v -> a.copy(locality = v) }),
    AddressSpec("Zone", { it.zone }, { a, v -> a.copy(zone = v) }),
    AddressSpec("Region", { it.region }, { a, v -> a.copy(region = v) }),
    AddressSpec("Country", { it.country }, { a, v -> a.copy(country = v) }),
)

private const val RulesCaption = "Source systems, API keys and users under this branch are limited to what’s selected here."

@Composable
fun BranchDetailScreen(
    id: String,
    onBack: () -> Unit,
    onOpenSourceSystem: (String) -> Unit,
    onOpenApiKey: (String) -> Unit,
) {
    val vm: BranchDetailViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val s by vm.state.collectAsStateWithLifecycle()
    val back by rememberUpdatedState(onBack)
    val openSystem by rememberUpdatedState(onOpenSourceSystem)
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                DetailEvent.Closed -> back()
                is DetailEvent.OpenSourceSystem -> openSystem(e.id)
            }
        }
    }
    val c = Eirs.colors
    val b = s.draft
    if (b == null) {
        ViewScaffold(header = { ViewHeader("BRANCH", onBack, title = { ViewTitle("Branch") }) }) {}
        return
    }
    val linked = s.settings.sourceSystemsOf(b.id)
    val eligible = SettingsRules.eligibleTaxTypes(b.id, s.settings)

    ViewScaffold(
        header = { SettingsHeader(b.code + if (b.isMain) " · MAIN BRANCH" else "", b.name, "", onBack) },
        bottomBar = { DeleteSaveFooter(s.deleteBlocker, vm::delete, vm::save) },
    ) {
        EirsCard(spacing = 12.dp) {
            SettingsField("Branch name", b.name, { v -> vm.edit { it.copy(name = v) } })
            SettingsField("Branch code", b.code, { v -> vm.edit { it.copy(code = v) } }, mono = true)
            SwitchRow(
                title = "Branch status",
                note = when {
                    b.isMain -> "The main branch is always active"
                    b.active -> "Accepting documents"
                    else -> "Paused — its source systems can’t issue"
                },
                checked = b.active, onToggle = { vm.edit { it.copy(active = !it.active) } }, enabled = !b.isMain,
            )
        }

        SectionLabel("MANAGER")
        EirsCard {
            ChipMultiSelect(
                s.settings.staff, listOfNotNull(b.manager), onToggle = { name -> vm.edit { it.copy(manager = name) } }, mono = false,
            )
        }

        SectionLabel("ADDRESS")
        EirsCard {
            FieldGrid(AddressSpecs) { spec, m ->
                SettingsField(spec.label, spec.get(b.address), { v -> vm.edit { it.copy(address = spec.set(it.address, v)) } }, modifier = m, fontSize = 15)
            }
        }

        SectionLabel("WHAT THIS BRANCH CAN ISSUE")
        EirsCard(spacing = 18.dp) {
            ChipGroup("Transaction types", SettingsCatalog.transactionTypes, b.transactionTypes, { v -> vm.edit { it.copy(transactionTypes = it.transactionTypes.toggled(v)) } })
            ChipGroup("Document types", SettingsCatalog.documentTypes, b.documentTypes, { v -> vm.edit { it.copy(documentTypes = it.documentTypes.toggled(v)) } })
            ChipGroup("Nature of supplies", SettingsCatalog.natures, b.natures, { v -> vm.edit { it.copy(natures = it.natures.toggled(v)) } })
            ChipGroup(
                "Operations", SettingsCatalog.operations, b.operations, { v -> vm.edit { it.copy(operations = it.operations.toggled(v)) } },
                caption = RulesCaption,
            )
        }

        SectionLabel("ELIGIBLE TAX TYPES")
        EirsCard(spacing = 8.dp) {
            if (eligible.isEmpty()) {
                SelectChip("None yet", selected = false, onClick = null)
            } else {
                ChipMultiSelect(eligible, eligible, onToggle = null, derived = { it == SettingsCatalog.EXCISE })
            }
            Text("Derived from this branch’s source systems.", style = EirsType.sans(12.sp).copy(color = c.muted))
        }

        SectionLabel("PERMISSIONS")
        EirsCard {
            ChipGroup(
                "Permission ceiling", SettingsCatalog.permissions, b.permissions, { v -> vm.edit { it.copy(permissions = it.permissions.toggled(v)) } },
                caption = "Per-user grants on this branch can’t go beyond this list.", mono = false,
            )
        }

        SectionLabel("SOURCE SYSTEMS")
        EirsCard(padding = PaddingValues(0.dp)) {
            linked.forEach { x ->
                val keys = s.settings.keysOf(x.id)
                val expanded = s.expandedSystem == x.id
                Column(Modifier.fillMaxWidth().bottomHairline(c.line)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(32.dp).pressBackground(Color.Transparent, c.background, RoundedCornerShape(9.dp)) {
                                vm.toggleExpanded(x.id)
                            },
                            contentAlignment = Alignment.Center,
                        ) { LineIcon(EirsIcons.ChevronRight, size = 14.dp, strokeWidth = 2.2f, tint = c.muted, modifier = Modifier.rotate(if (expanded) 90f else 0f)) }
                        Column(Modifier.weight(1f).plainClickable { onOpenSourceSystem(x.id) }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(x.label, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${x.type} · ${plural(keys.size, "API key")}", style = EirsType.sans(12.sp).copy(color = c.muted))
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusPill(if (x.active) "Active" else "Inactive", small = true)
                            StatusPill(x.verification.label, small = true)
                        }
                    }
                    if (expanded) {
                        Column(Modifier.fillMaxWidth().padding(start = 56.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            keys.forEach { k ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background)
                                        .plainClickable { onOpenApiKey(k.id) }.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(k.label, style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink))
                                        MonoText("eirs_live_••••${k.tail}", size = 10.sp, color = c.muted)
                                    }
                                    StatusPill(if (k.active) "Active" else "Deactivated", small = true)
                                }
                            }
                            if (keys.isEmpty()) Text("No API keys yet.", style = EirsType.sans(12.sp).copy(color = c.muted))
                        }
                    }
                }
            }
            if (linked.isEmpty()) {
                Text("No source systems on this branch yet.", modifier = Modifier.padding(16.dp), style = EirsType.sans(13.sp).copy(color = c.muted))
            }
            Row(
                Modifier.fillMaxWidth().height(50.dp)
                    .pressBackground(Color.White, c.background, RoundedCornerShape(0.dp), onClick = vm::addSourceSystem)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LineIcon(EirsIcons.Plus, size = 16.dp, strokeWidth = 2.4f, tint = c.brand)
                Text("Add source system", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.brand))
            }
        }
    }
}
