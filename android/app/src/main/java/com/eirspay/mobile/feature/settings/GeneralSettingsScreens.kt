package com.eirspay.mobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.MorEnvironment
import com.eirspay.domain.model.SecurityEventKind
import com.eirspay.domain.model.SellerProfile
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.PrimaryButton
import com.eirspay.mobile.ui.designsystem.StatusPill
import com.eirspay.mobile.ui.designsystem.TextLink
import com.eirspay.mobile.ui.designsystem.ViewScaffold
import com.eirspay.mobile.ui.designsystem.bottomHairline
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.serifAccent
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

private val ListPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)

// ---------------------------------------------------------------------------------------------
// Account
// ---------------------------------------------------------------------------------------------

@Composable
fun AccountSettingsScreen(onBack: () -> Unit) {
    val vm: AccountViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val c = Eirs.colors
    val user = s.session?.user
    ViewScaffold(header = { SettingsHeader("SETTINGS · ACCOUNT", "Your ", "account", onBack) }, bodyPadding = ListPadding) {
        EirsCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
                    Text(user?.initials.orEmpty(), style = EirsType.sans(18.sp, FontWeight.SemiBold).copy(color = Color.White))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(user?.fullName.orEmpty(), style = EirsType.sans(17.sp, FontWeight.SemiBold).copy(color = c.ink))
                    Text(user?.email.orEmpty(), style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill("Email verified")
                        StatusPill(user?.role?.name ?: "OWNER")
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, c.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("User ID", style = EirsType.sans(13.sp).copy(color = c.muted))
            MonoText(user?.id.orEmpty(), size = 13.sp, weight = FontWeight.Medium)
        }

        SectionLabel("SECURITY")
        EirsCard(spacing = 12.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Password", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = c.ink))
                    Text(s.settings.passwordNote, style = EirsType.sans(12.sp).copy(color = c.muted))
                }
                TextLink(if (s.password.open) "Cancel" else "Change", vm::togglePassword, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (s.password.open) {
                SettingsField("Current password", s.password.current, vm::setCurrent, placeholder = "••••••••", secret = true)
                SettingsField("New password", s.password.new, vm::setNew, placeholder = "At least 8 characters", secret = true)
                SettingsField("Confirm new password", s.password.confirm, vm::setConfirm, placeholder = "Repeat it", secret = true)
                PrimaryButton("Update password", vm::savePassword, enabled = s.password.valid, height = 48.dp, radius = 12.dp, fontSize = 15.sp, horizontalPadding = 16.dp)
            }
        }

        SectionLabel("SIGNED-IN DEVICES")
        EirsCard(padding = PaddingValues(0.dp)) {
            s.settings.devices.forEachIndexed { i, d ->
                Row(
                    Modifier.fillMaxWidth().then(if (i < s.settings.devices.lastIndex) Modifier.bottomHairline(c.line) else Modifier)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(d.device, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
                        MonoText("${d.fingerprint} · ${d.location} · ${d.lastActive}", size = 10.sp, color = c.muted, maxLines = 1)
                    }
                    if (d.current) StatusPill("This device")
                    else TextLink("Sign out", { vm.signOut(d.id) }, size = 13.sp, color = c.red, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
        if (s.settings.devices.size > 1) {
            Box(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.line, RoundedCornerShape(12.dp))
                    .background(Color.White).plainClickable { vm.signOutOthers() }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text("Sign out all other devices", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.red)) }
        }

        SectionLabel("RECENT ACTIVITY")
        EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            s.settings.securityEvents.forEachIndexed { i, e ->
                val bad = e.kind == SecurityEventKind.FAILED
                val icon = when (e.kind) {
                    SecurityEventKind.SIGN_IN -> EirsIcons.LogIn
                    SecurityEventKind.FAILED -> EirsIcons.Alert
                    SecurityEventKind.SIGN_OUT -> EirsIcons.LogOut
                    SecurityEventKind.PASSWORD_RESET -> EirsIcons.RotateCcw
                }
                Row(
                    Modifier.fillMaxWidth().then(if (i < s.settings.securityEvents.lastIndex) Modifier.bottomHairline(c.line) else Modifier)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(if (bad) c.redTint else c.tint1),
                        contentAlignment = Alignment.Center,
                    ) { LineIcon(icon, size = 15.dp, tint = if (bad) c.red else c.brand) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(e.title, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
                        Text(e.detail, style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(e.whenText, style = EirsType.sans(11.sp).copy(color = c.muted), maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// MoR credentials
// ---------------------------------------------------------------------------------------------

@Composable
fun MorCredentialsScreen(onBack: () -> Unit, onVerifySetup: () -> Unit) {
    val vm: LegalSettingsViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val c = Eirs.colors
    val cred = s.credentials
    val editing = s.editingCredentials
    ViewScaffold(
        header = {
            SettingsHeader("SETTINGS · EIRMS " + cred.environment.name, "MoR ", "credentials", onBack) {
                HeaderEditButton(editing, vm::toggleCredentialsEdit)
            }
        },
        bodyPadding = ListPadding,
    ) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, c.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(if (s.verification.isVerified) "Verified" else "Not verified")
                Text("with MoR", style = EirsType.sans(13.sp).copy(color = c.ink))
            }
            TextLink("Verify setup →", onVerifySetup, size = 13.sp)
        }
        EirsCard(spacing = 10.dp) {
            Text("Environment", style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.background).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(3.dp),
            ) {
                MorEnvironment.entries.forEach { env ->
                    val on = env == cred.environment
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(9.dp)).background(if (on) c.brand else Color.Transparent)
                            .plainClickable(enabled = editing) { vm.editCredentials { it.copy(environment = env) } },
                        contentAlignment = Alignment.Center,
                    ) { MonoText(env.name, size = 13.sp, weight = FontWeight.Medium, color = if (on) Color.White else c.muted) }
                }
            }
            Text(
                if (cred.environment == MorEnvironment.DEV) "Test documents only. Nothing is filed with MoR." else "Live. Every document is filed with MoR.",
                style = EirsType.sans(12.sp).copy(color = c.muted),
            )
        }
        EirsCard(spacing = 12.dp) {
            SettingsField("Client ID", cred.clientId, { v -> vm.editCredentials { it.copy(clientId = v) } }, mono = true, readOnly = !editing, editingStyle = editing, fontSize = 14)
            SettingsField("TIN", cred.tin, { v -> vm.editCredentials { it.copy(tin = v) } }, mono = true, readOnly = !editing, editingStyle = editing, fontSize = 14)
        }
        EirsCard(spacing = 12.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Secrets", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = c.ink))
                TextLink(if (s.showSecrets) "Hide secrets" else "Show secrets", vm::toggleSecrets, size = 13.sp)
            }
            SettingsField(
                "Client secret", cred.clientSecret, { v -> vm.editCredentials { it.copy(clientSecret = v) } },
                mono = true, readOnly = !editing, editingStyle = editing, secret = !s.showSecrets, fontSize = 14,
            )
            SettingsField(
                "API key", cred.apiKey, { v -> vm.editCredentials { it.copy(apiKey = v) } },
                mono = true, readOnly = !editing, editingStyle = editing, secret = !s.showSecrets, fontSize = 14,
            )
        }
        Note(if (editing) "Editing. Tap Save when you’re done." else "Locked. Tap Edit to change credentials.")
    }
}

// ---------------------------------------------------------------------------------------------
// Seller profile
// ---------------------------------------------------------------------------------------------

private data class SellerFieldSpec(val label: String, val get: (SellerProfile) -> String, val set: (SellerProfile, String) -> SellerProfile, val mono: Boolean = false)

private val LegalFields = listOf(
    SellerFieldSpec("TIN *", { it.tin }, { p, v -> p.copy(tin = v) }, mono = true),
    SellerFieldSpec("Legal name *", { it.legalName }, { p, v -> p.copy(legalName = v) }),
    SellerFieldSpec("VAT number *", { it.vatNumber }, { p, v -> p.copy(vatNumber = v) }, mono = true),
)
private val ContactFields = listOf(
    SellerFieldSpec("Email *", { it.email }, { p, v -> p.copy(email = v) }),
    SellerFieldSpec("Phone *", { it.phone }, { p, v -> p.copy(phone = v) }, mono = true),
)
private val AddressFields = listOf(
    SellerFieldSpec("Region *", { it.region }, { p, v -> p.copy(region = v) }),
    SellerFieldSpec("City", { it.city }, { p, v -> p.copy(city = v) }),
    SellerFieldSpec("Wereda *", { it.wereda }, { p, v -> p.copy(wereda = v) }),
    SellerFieldSpec("Sub city", { it.subCity }, { p, v -> p.copy(subCity = v) }),
    SellerFieldSpec("Zone", { it.zone }, { p, v -> p.copy(zone = v) }),
    SellerFieldSpec("Kebele", { it.kebele }, { p, v -> p.copy(kebele = v) }),
    SellerFieldSpec("Locality", { it.locality }, { p, v -> p.copy(locality = v) }),
    SellerFieldSpec("House no.", { it.houseNumber }, { p, v -> p.copy(houseNumber = v) }),
    SellerFieldSpec("Country", { it.country }, { p, v -> p.copy(country = v) }),
)

@Composable
fun SellerProfileScreen(onBack: () -> Unit) {
    val vm: LegalSettingsViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val editing = s.editingSeller
    val profile = s.seller

    @Composable
    fun field(spec: SellerFieldSpec, modifier: Modifier = Modifier) = SettingsField(
        spec.label, spec.get(profile), { v -> vm.editSeller { spec.set(it, v) } }, modifier = modifier,
        mono = spec.mono, readOnly = !editing, editingStyle = editing, fontSize = 15,
    )

    ViewScaffold(
        header = {
            SettingsHeader("SETTINGS · LEGAL ENTITY", "Seller ", "profile", onBack) { HeaderEditButton(editing, vm::toggleSellerEdit) }
        },
        bodyPadding = ListPadding,
    ) {
        SectionLabel("LEGAL", top = false)
        EirsCard(spacing = 12.dp) { LegalFields.forEach { field(it) } }
        SectionLabel("CONTACT")
        EirsCard(spacing = 12.dp) { ContactFields.forEach { field(it) } }
        SectionLabel("ADDRESS")
        EirsCard { FieldGrid(AddressFields) { spec, m -> field(spec, m) } }
        Note("Printed on every invoice and receipt. Fields marked * are required by MoR.")
    }
}

// ---------------------------------------------------------------------------------------------
// Verify setup
// ---------------------------------------------------------------------------------------------

@Composable
fun VerifySetupScreen(onBack: () -> Unit) {
    val vm: VerifySetupViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val c = Eirs.colors
    val v = s.verification
    val ok = v.isVerified
    val seller = s.settings.seller
    val probeSystem = s.settings.sourceSystems.firstOrNull()

    LoadingHost(s.loading) {
        ViewScaffold(header = { SettingsHeader("SETTINGS · MOR PROBE", "Verify ", "setup", onBack) }, bodyPadding = ListPadding) {
            EirsCard(radius = 20.dp, padding = PaddingValues(18.dp), spacing = 14.dp) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(if (ok) c.brand else c.tint1),
                    contentAlignment = Alignment.Center,
                ) { LineIcon(EirsIcons.Shield, size = 22.dp, tint = if (ok) Color.White else c.brand) }
                Text(
                    serifAccent(if (ok) "Verified with " else "Ready to verify with ", "MoR."),
                    style = EirsType.serif(32.sp).copy(color = c.ink),
                )
                Text(
                    when {
                        ok -> "Your merchant is cleared for live invoicing. API keys are unlocked."
                        v.canRun -> "Sends a signed probe invoice built from your credentials, seller profile and source system."
                        else -> "Finish MoR credentials, seller profile and a source system first."
                    },
                    style = EirsType.sans(14.sp, lineHeight = 1.5).copy(color = c.muted),
                )
                PrimaryButton(
                    if (ok) "Re-run verification" else "Run verification", vm::run, enabled = v.canRun,
                    height = 52.dp, fontSize = 15.sp, horizontalPadding = 18.dp,
                )
            }
            SectionLabel("PROBE INVOICE · SELLER")
            KvCard(listOf("Legal name" to seller.legalName, "TIN" to seller.tin, "VAT number" to seller.vatNumber, "Region" to seller.region, "Wereda" to seller.wereda))
            SectionLabel("SOURCE SYSTEM")
            KvCard(
                listOf(
                    "System type" to (probeSystem?.type ?: ""), "System number" to (probeSystem?.systemId ?: ""),
                    "Cashier" to (probeSystem?.cashier ?: ""), "Sales person" to (probeSystem?.salesPerson ?: ""),
                ),
            )
            SectionLabel("BUYER · VALUE")
            EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                KvLine("Name", "Walk-in Customer", mono = false, divider = true)
                KvLine("ID type", "KID", mono = false, divider = true)
                KvLine("Tax", "150.00 ETB", mono = true, divider = true)
                KvLine("Total", "1,650.00 ETB", mono = true, divider = true)
                KvLine("Currency", "ETB", mono = true, divider = false)
            }
            SectionLabel("ITEMS")
            EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                val items = listOf(
                    listOf("1", "Item VAT15", "1 PCS · VAT15", "1,150.00"),
                    listOf("2", "Item VAT0", "1 PCS · VAT0", "500.00"),
                )
                items.forEachIndexed { i, (n, d, q, a) ->
                    Row(
                        Modifier.fillMaxWidth().then(if (i < items.lastIndex) Modifier.bottomHairline(c.line) else Modifier).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MonoText(n, size = 13.sp, color = c.muted)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(d, style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink))
                            Text(q, style = EirsType.sans(11.sp).copy(color = c.muted))
                        }
                        MonoText(a, size = 13.sp)
                    }
                }
            }
        }
    }
}
