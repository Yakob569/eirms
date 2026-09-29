package com.eirspay.mobile.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.domain.usecase.PasswordCheck
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/**
 * Shared auth chrome: white screen, top row with the 44dp back square, the optional 3-step progress
 * ("STEP n OF 3") and the "EN" language pill, then a scrollable body padded 28/24/40.
 *
 * The body is at least as tall as the viewport, so a `Spacer(Modifier.weight(1f))` inside [content]
 * pushes the primary button to the bottom when the form is short (CSS `min-height:100%` + `flex:1`).
 */
@Composable
internal fun AuthScaffold(
    onBack: () -> Unit,
    step: Int? = null,
    spacing: Dp = 22.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Eirs.colors
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconSquareButton(EirsIcons.ArrowLeft, onBack, size = 44.dp)
            if (step != null) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.width(140.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { i ->
                            val bar by animateColorAsState(if (i < step) c.brand else c.tint2, label = "stepBar")
                            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(4.dp)).background(bar))
                        }
                    }
                    Eyebrow("STEP $step OF 3", color = c.muted, size = 10.sp)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            Row(
                Modifier.height(36.dp).clip(CircleShape).border(1.dp, c.line, CircleShape).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                LineIcon(EirsIcons.Globe, size = 13.dp, tint = c.muted)
                Text("EN", style = EirsType.sans(12.sp).copy(color = c.muted))
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().imePadding()) {
            val viewport = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = viewport).padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 40.dp)
                        .riseIn(),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    content = content,
                )
            }
        }
    }
}

/** Serif headline + muted lead paragraph. */
@Composable
internal fun AuthHeading(
    plain: String,
    accent: String = "",
    lead: String? = null,
    leadContent: AnnotatedString? = null,
    size: TextUnit = 40.sp,
    lineHeight: Double = 1.0,
    leadLineHeight: Double? = null,
    gap: Dp = 8.dp,
    trailing: String = "",
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Text(serifAccent(plain, accent, trailing), style = EirsType.serif(size, lineHeight).copy(color = Eirs.colors.ink))
        val style = EirsType.sans(15.sp, lineHeight = leadLineHeight).copy(color = Eirs.colors.muted)
        if (leadContent != null) Text(leadContent, style = style) else if (lead != null) Text(lead, style = style)
    }
}

/** Small red validation message (13sp). */
@Composable
internal fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = EirsType.sans(13.sp).copy(color = Eirs.colors.red))
}

/** Teal confirmation banner with a check-circle ("Account created for …"). */
@Composable
internal fun ConfirmBanner(text: String) {
    val c = Eirs.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.tint1).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LineIcon(EirsIcons.CheckCircle, size = 18.dp, strokeWidth = 2.2f, tint = c.brandDark)
        Text(text, style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.brandDark))
    }
}

/** Bordered info card of label/value rows ("Account · Yishak Girma"). */
@Composable
internal fun InfoRows(rows: List<Pair<String, String>>, monoLast: Boolean = false) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, c.line, shape)) {
        rows.forEachIndexed { i, (k, v) ->
            Row(
                Modifier.fillMaxWidth().then(if (i < rows.lastIndex) Modifier.bottomHairline(c.line) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(k, style = EirsType.sans(14.sp).copy(color = c.muted))
                Spacer(Modifier.width(12.dp))
                val mono = monoLast && i == rows.lastIndex
                Text(
                    v, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false),
                    style = (if (mono) EirsType.mono(13.sp) else EirsType.sans(14.sp)).copy(color = c.ink),
                )
            }
        }
    }
}

/**
 * Success / done layout: the mark, headline and body are vertically centred in the free space and
 * the actions sit at the bottom (CSS `min-height:100%`, inner `flex:1; justify-content:center`).
 */
@Composable
internal fun ColumnScope.SuccessLayout(
    icon: LineIconSpec,
    plain: String,
    accent: String,
    body: @Composable () -> Unit,
    titleSize: TextUnit = 44.sp,
    iconSize: Dp = 32.dp,
    card: (@Composable () -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Spacer(Modifier.weight(1f))
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SuccessMark(icon, iconSize = iconSize)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(serifAccent(plain, accent), style = EirsType.serif(titleSize).copy(color = Eirs.colors.ink))
            ProvideTextStyleColor(EirsType.sans(15.sp, lineHeight = 1.55).copy(color = Eirs.colors.muted)) { body() }
        }
        card?.invoke()
    }
    Spacer(Modifier.weight(1f))
    Spacer(Modifier.height(24.dp))
    actions()
}

/** Labelled password input with the shared show/hide eye. */
@Composable
internal fun AuthPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit,
    placeholder: String,
    imeAction: ImeAction = ImeAction.Next,
) {
    LabeledField(label) {
        PasswordField(value, onValueChange, visible, onToggle, placeholder = placeholder, imeAction = imeAction)
    }
}

/** Strength meter (4 bars) + the 2-column rule checklist, exactly as the prototype's `pwVals()`. */
@Composable
internal fun PasswordStrength(check: PasswordCheck, password: String) {
    val c = Eirs.colors
    val scoreColors = listOf(c.line, c.red, c.strengthFair, c.brand, c.brandDark)
    val scoreColor = scoreColors[check.score]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) { i ->
                    val bar by animateColorAsState(if (i < check.score) scoreColor else c.line, label = "pwBar")
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(4.dp)).background(bar))
                }
            }
            Text(
                if (password.isNotEmpty()) check.strengthLabel else "",
                modifier = Modifier.widthIn(min = 48.dp), textAlign = TextAlign.End,
                style = EirsType.sans(12.sp, FontWeight.SemiBold).copy(color = scoreColor),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            check.rules.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { rule ->
                        val fg by animateColorAsState(if (rule.satisfied) c.ink else c.muted, label = "ruleFg")
                        val bg by animateColorAsState(if (rule.satisfied) c.brand else Color.Transparent, label = "ruleBg")
                        val bd by animateColorAsState(if (rule.satisfied) c.brand else c.line, label = "ruleBd")
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Box(
                                Modifier.size(16.dp).clip(CircleShape).background(bg).border(1.5.dp, bd, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { LineIcon(EirsIcons.Check, size = 9.dp, strokeWidth = 4f, tint = Color.White) }
                            Text(rule.label, style = EirsType.sans(13.sp).copy(color = fg), maxLines = 1)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Centered muted tappable line ("Use a different method"). */
@Composable
internal fun MutedAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text, textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().plainClickable(onClick = onClick),
        style = EirsType.sans(14.sp).copy(color = Eirs.colors.muted),
    )
}
