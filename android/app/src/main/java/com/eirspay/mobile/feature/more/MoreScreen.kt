package com.eirspay.mobile.feature.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.SetupSteps
import com.eirspay.domain.model.UserRole
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/** More tab ("More tools."): terminal mode, tools, fiscal readiness and the settings index. */
@Composable
fun MoreTab(
    onStartTerminal: () -> Unit,
    onOpenItems: () -> Unit,
    onOpenWithholding: () -> Unit,
    onOpenCanceledInvoices: () -> Unit,
) {
    val vm: MoreViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    MoreContent(state, onStartTerminal, onOpenItems, onOpenWithholding, onOpenCanceledInvoices)
}

@Composable
private fun MoreContent(
    state: MoreUiState,
    onStartTerminal: () -> Unit,
    onOpenItems: () -> Unit,
    onOpenWithholding: () -> Unit,
    onOpenCanceled: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TerminalCard(onStartTerminal)
        ToolsCard(state, onOpenItems, onOpenWithholding, onOpenCanceled)
        SectionLabel("FISCAL READINESS")
        ReadinessCard(state)
        SetupStepper(state.setupDone)
        SectionLabel("ALL SETTINGS")
        SettingsCard(state)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Eyebrow(text, color = Eirs.colors.muted, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp))
}

@Composable
private fun TerminalCard(onStart: () -> Unit) {
    val c = Eirs.colors
    val ring = Color.White.copy(alpha = 0.12f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.ink)
            .drawBehind {
                // 160dp ring off the top-right corner (right:-50, top:-50).
                val r = 80.dp.toPx()
                drawCircle(ring, radius = r - 0.5.dp.toPx(), center = Offset(size.width + 50.dp.toPx() - r, -50.dp.toPx() + r), style = Stroke(1.dp.toPx()))
            }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.brand), contentAlignment = Alignment.Center) {
                LineIcon(EirsIcons.Monitor, size = 22.dp, strokeWidth = 1.8f, tint = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Terminal mode", style = EirsType.serif(26.sp).copy(color = Color.White))
                Text("For counter tablets and sales staff", style = EirsType.sans(12.sp).copy(color = c.tint2))
            }
        }
        Text(
            "Locks the app to a full-screen sales screen. Tabs, reports and settings stay hidden until you hold to exit.",
            style = EirsType.sans(13.sp, lineHeight = 1.5).copy(color = Color.White.copy(alpha = 0.8f)),
        )
        PrimaryButton("Start terminal mode", onStart, height = 48.dp, radius = 12.dp, fontSize = 15.sp, horizontalPadding = 16.dp)
    }
}

@Composable
private fun ChevronRow(height: androidx.compose.ui.unit.Dp, divider: Boolean, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val c = Eirs.colors
    Row(
        Modifier.fillMaxWidth().height(height).pressBackground(Color.White, c.background, RoundedCornerShape(0.dp), onClick = onClick)
            .then(if (divider) Modifier.bottomHairline(c.line) else Modifier).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
        LineIcon(EirsIcons.ChevronRight, size = 15.dp, tint = c.muted)
    }
}

@Composable
private fun ToolsCard(state: MoreUiState, onItems: () -> Unit, onWithholding: () -> Unit, onCanceled: () -> Unit) {
    val c = Eirs.colors
    val rows = listOf(
        Triple(EirsIcons.Package, "Items" to "${state.activeItems} active", onItems),
        Triple(
            EirsIcons.Percent,
            "Withholding" to (if (state.awaitingCertificates > 0) "${state.awaitingCertificates} awaiting certificate" else "All certificates in"),
            onWithholding,
        ),
        Triple(EirsIcons.Ban, "Canceled invoices" to "", onCanceled),
    )
    EirsCard(padding = PaddingValues(0.dp)) {
        rows.forEachIndexed { i, (icon, text, go) ->
            ChevronRow(60.dp, divider = i < rows.lastIndex, onClick = go) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.tint1), contentAlignment = Alignment.Center) {
                    LineIcon(icon, size = 17.dp, strokeWidth = 1.9f, tint = c.brand)
                }
                Text(text.first, modifier = Modifier.weight(1f), style = EirsType.sans(15.sp, FontWeight.Medium), maxLines = 1)
                if (text.second.isNotEmpty()) Text(text.second, style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1)
            }
        }
    }
}

@Composable
private fun ReadinessCard(state: MoreUiState) {
    val c = Eirs.colors
    val done = state.setupDone
    val left = 5 - done
    val role = (state.session?.user?.role ?: UserRole.OWNER).name
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.tint1).border(1.dp, c.tint2, RoundedCornerShape(18.dp)).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val k = size.minDimension / 36f
                val stroke = 3f * k
                val r = 15.5f * k
                val topLeft = Offset(size.width / 2 - r, size.height / 2 - r)
                val arc = androidx.compose.ui.geometry.Size(2 * r, 2 * r)
                drawCircle(c.tint2, radius = r, style = Stroke(stroke))
                if (done > 0) drawArc(c.brand, -90f, 360f * done / 5f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text("$done/5", style = EirsType.serif(24.sp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow("${done * 20}% COMPLETE · $role", size = 10.sp)
            Text(
                if (done == 5) "You're ready. Live invoicing is unlocked." else "$left step${if (left == 1) "" else "s"} left before live invoicing.",
                style = EirsType.serif(24.sp, lineHeight = 1.1),
            )
        }
    }
}

@Composable
private fun SetupStepper(done: Int) {
    val c = Eirs.colors
    EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
        SetupSteps.forEachIndexed { i, step ->
            val isDone = i < done
            val cur = i == done
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.fillMaxHeight().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(if (isDone) c.brand else Color.White)
                            .border(1.5.dp, if (isDone || cur) c.brand else c.line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) LineIcon(EirsIcons.Check, size = 14.dp, strokeWidth = 2.8f, tint = Color.White)
                        else MonoText((i + 1).toString(), color = if (cur) c.brand else c.muted)
                    }
                    Box(
                        Modifier.width(1.5.dp).weight(1f).alpha(if (i == SetupSteps.lastIndex) 0f else 1f)
                            .background(if (isDone) c.brand else c.line),
                    )
                }
                Column(Modifier.weight(1f).padding(top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            step.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = if (isDone || cur) c.ink else c.muted),
                        )
                        val (bg, fg, label) = when {
                            isDone -> Triple(c.tint1, c.brandDark, "Complete")
                            cur -> Triple(c.ink, Color.White, "Next")
                            else -> Triple(c.neutralChip, c.muted, "Locked")
                        }
                        Badge(label, bg, fg, radius = 999.dp, fontSize = 11.sp, tracking = 0.0, horizontal = 8.dp, vertical = 3.dp)
                    }
                    Text(step.description, style = EirsType.sans(13.sp, lineHeight = 1.45).copy(color = c.muted))
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(state: MoreUiState) {
    val c = Eirs.colors
    val uid = state.session?.user?.id?.let { if (it.length > 8) it.take(8) + "…" else it } ?: ""
    val env = state.session?.merchant?.environment ?: "DEV"
    val rows = listOf(
        "Account" to uid, "MoR credentials" to env, "Seller profile" to "", "Branches" to state.branchCount.toString(),
        "Departments" to "", "Source systems" to "4 active", "API keys" to "", "Team" to "2", "Roles" to "",
    )
    EirsCard(padding = PaddingValues(0.dp)) {
        rows.forEachIndexed { i, (label, meta) ->
            ChevronRow(52.dp, divider = i < rows.lastIndex, onClick = { }) {
                Text(label, modifier = Modifier.weight(1f), style = EirsType.sans(15.sp), maxLines = 1)
                if (meta.isNotEmpty()) Text(meta, style = EirsType.sans(13.sp).copy(color = c.muted, letterSpacing = 0.em), maxLines = 1)
            }
        }
    }
}
