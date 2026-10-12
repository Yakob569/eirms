package com.eirspay.mobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.EirsSwitch
import com.eirspay.mobile.ui.designsystem.Eyebrow
import com.eirspay.mobile.ui.designsystem.FormField
import com.eirspay.mobile.ui.designsystem.IconSquareButton
import com.eirspay.mobile.ui.designsystem.LabeledField
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.PrimaryButton
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.ViewTitle
import com.eirspay.mobile.ui.designsystem.bottomHairline
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType

/** Settings pages reachable from the More tab's settings list (and the setup steps). */
enum class SettingsPage { ACCOUNT, MOR, SELLER, BRANCHES, SOURCE_SYSTEMS, API_KEYS, VERIFY }

internal fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"

/** Full-screen settings header: back, mono eyebrow, serif title with an italic accent, optional action. */
@Composable
internal fun SettingsHeader(
    eyebrow: String,
    plain: String,
    accent: String,
    onBack: () -> Unit,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) = ViewHeader(eyebrow = eyebrow, onBack = onBack, title = { ViewTitle(plain, accent) }, trailing = trailing)

/** Brand "+" square in list headers (add branch / source system / API key). */
@Composable
internal fun HeaderPlusButton(onClick: () -> Unit) = IconSquareButton(EirsIcons.Plus, onClick, filled = true, strokeWidth = 2.4f)

/** Edit ↔ Save toggle for pages that stay read-only until Edit. */
@Composable
internal fun HeaderEditButton(editing: Boolean, onClick: () -> Unit) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.height(40.dp).clip(shape).background(if (editing) c.brand else Color.White)
            .border(1.dp, if (editing) c.brand else c.line, shape).plainClickable(onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(if (editing) "Save" else "Edit", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = if (editing) Color.White else c.ink)) }
}

/** Mono small-caps section label between cards ("SECURITY", "ADDRESS"). */
@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier, top: Boolean = true) {
    Eyebrow(text, color = Eirs.colors.muted, modifier = modifier.padding(start = 4.dp, end = 4.dp, top = if (top) 8.dp else 0.dp))
}

/** Muted one-line note under or between cards. */
@Composable
internal fun Note(text: String, modifier: Modifier = Modifier, color: Color = Eirs.colors.muted) {
    Text(text, modifier = modifier.padding(horizontal = 4.dp), style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = color))
}

/**
 * Labelled 46dp field. Read-only fields keep the tinted background; while editing a read-only-until-Edit
 * page they turn white ([editingStyle]).
 */
@Composable
internal fun SettingsField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    readOnly: Boolean = false,
    secret: Boolean = false,
    editingStyle: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    fontSize: Int = 16,
) {
    LabeledField(label, modifier = modifier, labelSize = 13.sp, gap = 6.dp) {
        FormField(
            value, onChange, placeholder = placeholder, mono = mono, readOnly = readOnly, keyboardType = keyboardType,
            restingBackground = if (editingStyle) Color.White else Eirs.colors.background, fontSize = fontSize.sp,
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        )
    }
}

/** Two fields per row (address grids). */
@Composable
internal fun <T> FieldGrid(items: List<T>, field: @Composable (item: T, modifier: Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { item -> field(item, Modifier.weight(1f)) }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

/** Bordered row with a switch, a title and a note (branch status, source system status). */
@Composable
internal fun SwitchRow(
    title: String,
    note: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.dp, c.line, shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EirsSwitch(checked, onToggle = { if (enabled) onToggle() }, modifier = Modifier.alpha(if (enabled) 1f else 0.5f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
            Text(note, style = EirsType.sans(12.sp).copy(color = c.muted))
        }
        trailing?.invoke(this)
    }
}

/** White card of key / value rows separated by hairlines. */
@Composable
internal fun KvCard(rows: List<Pair<String, String>>, monoValues: Boolean = false) {
    EirsCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        rows.forEachIndexed { i, (k, v) -> KvLine(k, v.ifBlank { "—" }, monoValues, divider = i < rows.lastIndex) }
    }
}

@Composable
internal fun KvLine(key: String, value: String, mono: Boolean, divider: Boolean) {
    val c = Eirs.colors
    Row(
        Modifier.fillMaxWidth().then(if (divider) Modifier.bottomHairline(c.line) else Modifier).padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(key, style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1)
        Text(
            value, modifier = Modifier.weight(1f), textAlign = TextAlign.End,
            style = EirsType.sans(13.sp, FontWeight.Medium).copy(color = c.ink, fontFamily = if (mono) EirsFonts.Mono else EirsFonts.Sans),
        )
    }
}

/** Tappable white list card (branch / source system / key rows). */
@Composable
internal fun SettingsListCard(onClick: () -> Unit, modifier: Modifier = Modifier, dim: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    com.eirspay.mobile.ui.designsystem.ListCard(onClick = onClick, modifier = modifier, alpha = if (dim) 0.6f else 1f, content = content)
}

/** Bold title that ellipsizes (list card headers). */
@Composable
internal fun CardTitle(text: String, modifier: Modifier = Modifier, size: Int = 15) {
    Text(text, modifier = modifier, style = EirsType.sans(size.sp, FontWeight.SemiBold).copy(color = Eirs.colors.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Sticky footer for branch / source system details: optional delete blocker, Delete, Save changes. */
@Composable
internal fun ColumnScope.DeleteSaveFooter(deleteBlocker: String?, onDelete: () -> Unit, onSave: () -> Unit) {
    val c = Eirs.colors
    if (deleteBlocker != null) Text(deleteBlocker, style = EirsType.sans(12.sp).copy(color = c.muted))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val shape = RoundedCornerShape(14.dp)
        Row(
            Modifier.height(52.dp).alpha(if (deleteBlocker == null) 1f else 0.45f).clip(shape).border(1.dp, c.line, shape)
                .background(Color.White).plainClickable(enabled = deleteBlocker == null, onClick = onDelete).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LineIcon(EirsIcons.Trash, size = 16.dp, tint = c.red)
            Text("Delete", style = EirsType.sans(15.sp, FontWeight.SemiBold).copy(color = c.red))
        }
        PrimaryButton("Save changes", onSave, modifier = Modifier.weight(1f), height = 52.dp, fontSize = 15.sp, horizontalPadding = 18.dp)
    }
}
