package com.eirspay.mobile.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.model.Branch
import com.eirspay.domain.model.BranchInfo
import com.eirspay.domain.model.Session
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

@Composable
private fun SheetTitle(plain: String, accent: String = "", size: Int = 26, bottom: Int = 8) {
    SerifTitle(plain, accent, size = size.sp, modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = bottom.dp))
}

/** "View data for": All branches + each merchant branch. */
@Composable
internal fun BranchSheet(
    visible: Boolean,
    selected: Branch?,
    branches: List<BranchInfo>,
    onPick: (Branch?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Eirs.colors
    EirsBottomSheet(visible, onDismiss) {
        SheetTitle("View data for")
        val options = listOf(Triple<Branch?, String, String>(null, "All branches", "Combined view · ${branches.size} branches")) +
            branches.map { info ->
                val systems = Fmt.plural(info.sourceSystems, "source system")
                Triple<Branch?, String, String>(info.branch, info.branch.displayName, if (info.isDefault) "Default · $systems" else systems)
            }
        options.forEach { (branch, label, sub) ->
            val on = branch == selected
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(if (on) c.tint1 else Color.White)
                    .border(1.dp, if (on) c.brand else c.line, shape).plainClickable { onPick(branch) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(label, style = EirsType.sans(15.sp, if (on) FontWeight.SemiBold else FontWeight.Normal))
                    Text(sub, style = EirsType.sans(12.sp).copy(color = c.muted))
                }
                if (on) LineIcon(EirsIcons.Check, size = 18.dp, strokeWidth = 2.6f, tint = c.brand)
            }
        }
    }
}

/** Profile sheet opened from the avatar. */
@Composable
internal fun UserSheet(
    visible: Boolean,
    session: Session?,
    onAccountSettings: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Eirs.colors
    EirsBottomSheet(visible, onDismiss) {
        val user = session?.user
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
                Text(user?.initials ?: "", style = EirsType.sans(17.sp, FontWeight.SemiBold).copy(color = Color.White))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(user?.fullName ?: "", style = EirsType.sans(17.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user?.email ?: "", style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.background).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            @Composable
            fun InfoRow(key: String, value: @Composable () -> Unit) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(key, style = EirsType.sans(13.sp).copy(color = c.muted))
                    value()
                }
            }
            InfoRow("Merchant") { Text(session?.merchant?.displayName ?: "", style = EirsType.sans(13.sp, FontWeight.Medium)) }
            InfoRow("Merchant ID") { MonoText(session?.merchant?.id ?: "", size = 13.sp) }
            InfoRow("User ID") { MonoText(user?.id ?: "", size = 13.sp) }
            InfoRow("Role") {
                Badge(user?.role?.name ?: "", c.ink, Color.White, radius = 999.dp, fontSize = 11.sp, tracking = 0.06, horizontal = 8.dp, vertical = 2.dp)
            }
        }
        SheetRow(EirsIcons.User, "Account settings", divider = true, onClick = onAccountSettings)
        SheetRow(EirsIcons.Globe, "Language", meta = "English", divider = true, onClick = { })
        SheetRow(EirsIcons.LogOut, "Sign out", color = c.red, weight = FontWeight.Medium, onClick = onSignOut)
    }
}

@Composable
private fun SheetRow(
    icon: LineIconSpec,
    label: String,
    onClick: () -> Unit,
    meta: String = "",
    divider: Boolean = false,
    color: Color = Eirs.colors.ink,
    weight: FontWeight = FontWeight.Normal,
) {
    val c = Eirs.colors
    Row(
        Modifier.fillMaxWidth().height(52.dp).then(if (divider) Modifier.bottomHairline(c.line) else Modifier)
            .plainClickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LineIcon(icon, size = 18.dp, strokeWidth = 1.8f, tint = color)
        Text(label, modifier = Modifier.weight(1f), style = EirsType.sans(15.sp, weight).copy(color = color))
        if (meta.isNotEmpty()) Text(meta, style = EirsType.sans(13.sp).copy(color = c.muted))
    }
}

/** "Start terminal mode": what happens, which branch to sell for, and the lock button. */
@Composable
internal fun TerminalSheet(
    visible: Boolean,
    branch: Branch,
    onPick: (Branch) -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Eirs.colors
    EirsBottomSheet(visible, onDismiss) {
        SheetTitle("Start terminal ", "mode", size = 28, bottom = 0)
        Column(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(
                "The app locks to a full-screen sales screen for this device.",
                "Tabs, reports and settings are hidden while it runs.",
                "Hold the Exit button to leave. The cart is cleared on exit.",
            ).forEachIndexed { i, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("0${i + 1}", style = EirsType.mono(14.sp, FontWeight.Medium).copy(color = c.brand))
                    Text(line, style = EirsType.sans(14.sp, lineHeight = 1.45))
                }
            }
        }
        Text("Sell for branch", modifier = Modifier.padding(horizontal = 6.dp), style = EirsType.sans(13.sp, FontWeight.Medium))
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Branch.entries.forEach { b ->
                val on = b == branch
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier.weight(1f).height(44.dp).clip(shape).background(if (on) c.ink else Color.White)
                        .border(1.5.dp, if (on) c.ink else c.line, shape).plainClickable { onPick(b) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(b.displayName, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = if (on) Color.White else c.ink), maxLines = 1)
                }
            }
        }
        PrimaryButton("Lock into terminal mode", onStart, height = 52.dp, radius = 14.dp, fontSize = 15.sp, horizontalPadding = 18.dp)
    }
}

/** "Register a document": invoice or receipt. */
@Composable
internal fun RegisterSheet(visible: Boolean, onInvoice: () -> Unit, onReceipt: () -> Unit, onDismiss: () -> Unit) {
    val c = Eirs.colors
    EirsBottomSheet(visible, onDismiss) {
        SheetTitle("Register a document")
        RegisterOption(
            icon = EirsIcons.FilePlus, title = "Register invoice", sub = "B2B, B2G and credit / debit notes", primary = true, onClick = onInvoice,
        )
        RegisterOption(icon = EirsIcons.Receipt, title = "Register receipt", sub = "Walk-in and point-of-sale", primary = false, onClick = onReceipt)
    }
}

@Composable
private fun RegisterOption(icon: LineIconSpec, title: String, sub: String, primary: Boolean, onClick: () -> Unit) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .then(if (primary) Modifier.background(c.brand) else Modifier.background(Color.White).border(1.dp, c.line, shape))
            .plainClickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (primary) Color.White.copy(alpha = 0.16f) else c.tint1),
            contentAlignment = Alignment.Center,
        ) { LineIcon(icon, size = 20.dp, strokeWidth = 1.8f, tint = if (primary) Color.White else c.brand) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = EirsType.sans(16.sp, FontWeight.SemiBold).copy(color = if (primary) Color.White else c.ink))
            Text(sub, style = EirsType.sans(12.sp).copy(color = if (primary) Color.White.copy(alpha = 0.85f) else c.muted))
        }
        LineIcon(EirsIcons.ArrowRight, size = 18.dp, tint = if (primary) Color.White else c.muted)
    }
}
