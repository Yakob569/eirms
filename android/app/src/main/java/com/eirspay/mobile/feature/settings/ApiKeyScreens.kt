package com.eirspay.mobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.SettingsCatalog
import com.eirspay.domain.usecase.SettingsRules
import com.eirspay.mobile.ui.designsystem.ChipGroup
import com.eirspay.mobile.ui.designsystem.ChipMultiSelect
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.OutlineButton
import com.eirspay.mobile.ui.designsystem.PrimaryButton
import com.eirspay.mobile.ui.designsystem.StatusPill
import com.eirspay.mobile.ui.designsystem.TextLink
import com.eirspay.mobile.ui.designsystem.ViewScaffold
import com.eirspay.mobile.ui.designsystem.bottomHairline
import com.eirspay.mobile.ui.designsystem.dashedBorder
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

private val ListBody = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)

// ---------------------------------------------------------------------------------------------
// List
// ---------------------------------------------------------------------------------------------

@Composable
fun ApiKeysScreen(onBack: () -> Unit, onOpenKey: (String) -> Unit, onCreateKey: () -> Unit) {
    val vm: SettingsListViewModel = hiltViewModel()
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = Eirs.colors
    val active = s.apiKeys.count { it.active }
    ViewScaffold(
        header = { SettingsHeader("SETTINGS · $active ACTIVE", "API ", "keys", onBack) { HeaderPlusButton(onCreateKey) } },
        bodyPadding = ListBody,
    ) {
        Text(
            plural(active, "active key") + " of ${s.apiKeys.size}",
            modifier = Modifier.padding(horizontal = 4.dp), style = EirsType.sans(13.sp).copy(color = c.muted),
        )
        s.apiKeys.forEach { k ->
            val system = s.sourceSystem(k.sourceSystemId)
            val branch = system?.let { s.branch(it.branchId) }
            SettingsListCard(onClick = { onOpenKey(k.id) }, dim = !k.active) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardTitle(k.label, Modifier.weight(1f))
                    StatusPill(if (k.active) "Active" else "Deactivated")
                }
                MonoText(
                    k.masked, size = 11.sp, maxLines = 1,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(horizontal = 10.dp, vertical = 8.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${system?.label ?: "—"} · ${branch?.name ?: "—"}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = EirsType.sans(12.sp).copy(color = c.muted),
                    )
                    Text(k.created, style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1)
                }
                Text(
                    plural(k.operations.size, "operation") + " · " + plural(k.transactionTypes.size, "transaction type"),
                    style = EirsType.sans(12.sp).copy(color = c.muted),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detail / create
// ---------------------------------------------------------------------------------------------

/** [id] null = create a new key. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiKeyDetailScreen(id: String?, onBack: () -> Unit) {
    val vm: ApiKeyDetailViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val s by vm.state.collectAsStateWithLifecycle()
    val copied by vm.copied.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val c = Eirs.colors
    val ceiling = s.ceiling
    val system = ceiling.system
    val branch = ceiling.branch
    val secret = s.revealedSecret
    val key = s.key
    val revoked = key != null && !key.active

    // New: "Create key". Active key: "Revoke key" (deactivates, never deletes). Revoked: no footer.
    val hasFooter = s.isNew || (key != null && key.active)
    val footer: @Composable ColumnScope.() -> Unit = {
        if (s.isNew) {
            PrimaryButton("Create key", vm::create, height = 52.dp, fontSize = 15.sp, horizontalPadding = 18.dp)
        } else {
            OutlineButton(
                "Revoke key", vm::revoke, modifier = Modifier.fillMaxWidth(), height = 52.dp, borderColor = c.red,
                contentColor = c.red, fontWeight = FontWeight.SemiBold,
            )
        }
    }

    ViewScaffold(
        header = {
            if (s.isNew) SettingsHeader("API KEYS · NEW", "New ", "key", onBack)
            else SettingsHeader("API KEY · " + (system?.label ?: "").uppercase(), s.label, "", onBack)
        },
        bottomBar = if (hasFooter) footer else null,
    ) {
        if (secret != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.ink).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Copy this key now", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = Color.White))
                Text(
                    "It’s shown once. After you leave this screen only the last four characters stay visible.",
                    style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = Color.White.copy(alpha = 0.75f)),
                )
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MonoText(secret, size = 11.sp, color = Color.White, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(c.brand)
                            .plainClickable { clipboard.setText(AnnotatedString(secret)); vm.markCopied() }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (copied) "Copied" else "Copy", style = EirsType.sans(12.sp, FontWeight.SemiBold).copy(color = Color.White)) }
                }
            }
        }

        EirsCard(spacing = 12.dp) {
            SettingsField("Key label", s.label, vm::setLabel, placeholder = "e.g. Front Counter · Till 3")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Bound to ${system?.label ?: "—"} · ${branch?.name ?: "—"} branch", modifier = Modifier.weight(1f),
                    style = EirsType.sans(12.sp, lineHeight = 1.4).copy(color = c.muted),
                )
                StatusPill(if (key == null || key.active) "Active" else "Deactivated")
            }
            if (secret == null && key != null) {
                MonoText(
                    key.masked, size = 12.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.background).padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }

        SectionLabel("SOURCE SYSTEM")
        EirsCard(spacing = 8.dp) {
            ChipMultiSelect(
                s.settings.sourceSystems, listOfNotNull(system), onToggle = { vm.bindTo(it.id) },
                label = { x -> x.label + " · " + (s.settings.branch(x.branchId)?.name ?: "—") },
                allowed = { SettingsRules.canBindKey(it, s.isNew, s.sourceSystemId) }, mono = false,
            )
            Text(
                if (s.isNew) "Inactive source systems can’t take new keys." else "A key stays bound to the source system it was created for.",
                style = EirsType.sans(12.sp).copy(color = c.muted),
            )
        }

        SectionLabel("WHAT THIS KEY CAN DO")
        EirsCard(spacing = 18.dp) {
            val branchName = branch?.name ?: "this"
            ChipGroup(
                "Operations", SettingsCatalog.operations, s.operations, vm::toggleOperation,
                allowed = { it in ceiling.operations },
                caption = "Greyed chips are outside what $branchName branch and ${system?.label ?: "its source system"} allow.",
            )
            ChipGroup("Transaction types", SettingsCatalog.transactionTypes, s.transactionTypes, vm::toggleTransactionType, allowed = { it in ceiling.transactionTypes })
            ChipGroup(
                "Tax types", SettingsCatalog.taxTypes + SettingsCatalog.EXCISE, s.taxTypes, vm::toggleTaxType,
                allowed = { it in ceiling.taxTypes },
                caption = if (s.settings.exciseEnabled) null else "Enable merchant excise to grant EXCISE.",
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("USERS", modifier = Modifier.weight(1f, fill = false), top = false)
            TextLink(if (s.assignOpen) "Done" else "+ Assign", vm::toggleAssign, size = 13.sp)
        }
        EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            s.users.forEach { u ->
                Row(
                    Modifier.fillMaxWidth().bottomHairline(c.line).padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(c.tint1), contentAlignment = Alignment.Center) {
                        Text(
                            u.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString(""),
                            style = EirsType.sans(12.sp, FontWeight.SemiBold).copy(color = c.brandDark),
                        )
                    }
                    Text(u, modifier = Modifier.weight(1f), style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).plainClickable { vm.unassign(u) }, contentAlignment = Alignment.Center) {
                        LineIcon(EirsIcons.Close, size = 14.dp, strokeWidth = 2.2f, tint = c.muted)
                    }
                }
            }
            if (s.users.isEmpty()) {
                Text(
                    "No users assigned. Only the owner can use this key.", modifier = Modifier.padding(vertical = 10.dp),
                    style = EirsType.sans(13.sp).copy(color = c.muted),
                )
            }
            if (s.assignOpen) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    s.settings.staff.filter { it !in s.users }.forEach { u ->
                        Box(
                            Modifier.height(34.dp).clip(RoundedCornerShape(999.dp)).background(Color.White).dashedBorder(c.brand, 999.dp)
                                .plainClickable { vm.assign(u) }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("+ $u", style = EirsType.sans(13.sp).copy(color = c.brandDark), maxLines = 1) }
                    }
                }
            }
        }
        if (revoked) {
            Text(
                "This key is deactivated. Requests signed with it are rejected.",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.redTint).padding(horizontal = 14.dp, vertical = 12.dp),
                style = EirsType.sans(13.sp, lineHeight = 1.45).copy(color = c.redInk),
            )
        }
    }
}
