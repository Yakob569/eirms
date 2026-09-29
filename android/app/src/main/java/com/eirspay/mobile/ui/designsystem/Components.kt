package com.eirspay.mobile.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

// ---------------------------------------------------------------------------------------------
// Interaction helpers
// ---------------------------------------------------------------------------------------------

/** Click without ripple; the design signals presses with colour changes instead. */
@Composable
fun Modifier.plainClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Clickable that swaps the background while pressed (CSS `style-active`). */
@Composable
fun Modifier.pressBackground(
    normal: Color,
    pressed: Color,
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    return clip(shape)
        .background(if (isPressed && enabled) pressed else normal, shape)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Dashed rounded border (CSS `border: 1px dashed`). */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))),
    )
}

// ---------------------------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------------------------

/** Serif headline with an italic, brand-coloured accent: "Welcome *back*". */
@Composable
fun serifAccent(plain: String, accent: String, trailing: String = "", accentColor: Color = Eirs.colors.brand): AnnotatedString =
    buildAnnotatedString {
        append(plain)
        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = accentColor)) { append(accent) }
        append(trailing)
    }

@Composable
fun SerifTitle(
    plain: String,
    accent: String = "",
    size: TextUnit = 40.sp,
    modifier: Modifier = Modifier,
    lineHeight: Double = 1.0,
    color: Color = Eirs.colors.ink,
    accentColor: Color = Eirs.colors.brand,
) {
    Text(
        serifAccent(plain, accent, accentColor = accentColor),
        modifier = modifier,
        style = EirsType.serif(size, lineHeight).copy(color = color),
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Eirs.colors.brandDark, size: TextUnit = 11.sp, tracking: Double = 0.14) {
    Text(text, modifier = modifier, style = EirsType.eyebrow(size, tracking).copy(color = color), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 12.sp,
    color: Color = Eirs.colors.ink,
    weight: FontWeight = FontWeight.Normal,
    strike: Boolean = false,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = textAlign,
        style = EirsType.mono(size, weight).copy(color = color, textDecoration = if (strike) TextDecoration.LineThrough else null),
    )
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier, size: TextUnit = 13.sp, lineHeight: Double? = null, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier = modifier, style = EirsType.sans(size, lineHeight = lineHeight).copy(color = Eirs.colors.muted), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

// ---------------------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------------------

/** 54dp brand button with the label on the left and an arrow on the right. Disabled = 45% opacity. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 54.dp,
    radius: Dp = 14.dp,
    fontSize: TextUnit = 16.sp,
    showArrow: Boolean = true,
    containerColor: Color = Eirs.colors.brand,
    pressedColor: Color = Eirs.colors.brandDark,
    contentColor: Color = Color.White,
    horizontalPadding: Dp = 20.dp,
) {
    val shape = RoundedCornerShape(radius)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .pressBackground(containerColor, pressedColor, shape, enabled, onClick)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showArrow) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        Text(text, style = EirsType.sans(fontSize, FontWeight.SemiBold).copy(color = contentColor))
        if (showArrow) LineIcon(EirsIcons.ArrowRight, size = 18.dp, tint = contentColor)
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    radius: Dp = 14.dp,
    borderColor: Color = Eirs.colors.line,
    contentColor: Color = Eirs.colors.ink,
    fontWeight: FontWeight = FontWeight.Medium,
    fontSize: TextUnit = 15.sp,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.height(height).clip(shape).border(1.5.dp, borderColor, shape).background(Color.White).plainClickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = EirsType.sans(fontSize, fontWeight).copy(color = contentColor)) }
}

/** Square outlined icon button used for back navigation in headers. */
@Composable
fun IconSquareButton(
    icon: LineIconSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    filled: Boolean = false,
    iconSize: Dp = 18.dp,
    strokeWidth: Float = 2f,
) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.size(size).clip(shape)
            .then(if (filled) Modifier.background(c.brand) else Modifier.border(1.dp, c.line, shape).background(Color.White))
            .plainClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { LineIcon(icon, size = iconSize, strokeWidth = strokeWidth, tint = if (filled) Color.White else c.ink) }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 40.dp) =
    IconSquareButton(EirsIcons.ArrowLeft, onClick, modifier, size)

/** Inline brand-coloured text action ("Sign in", "Resend code"). */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: TextUnit = 14.sp, color: Color = Eirs.colors.brand, weight: FontWeight = FontWeight.SemiBold) {
    Text(text, modifier = modifier.plainClickable(onClick = onClick), style = EirsType.sans(size, weight).copy(color = color), maxLines = 1)
}

/** "Already have an account? Sign in" footer line. */
@Composable
fun PromptLink(prompt: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text("$prompt ", style = EirsType.sans(14.sp).copy(color = Eirs.colors.muted))
        TextLink(action, onClick)
    }
}

// ---------------------------------------------------------------------------------------------
// Chips, badges, segmented controls, switches
// ---------------------------------------------------------------------------------------------

enum class ChipStyle { Solid, Tint }

/**
 * Filter chip. [ChipStyle.Solid]: selected is ink-filled (range, status chips).
 * [ChipStyle.Tint]: selected is tinted with a brand border (transaction-type, method chips).
 */
@Composable
fun EirsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Solid,
    height: Dp = 36.dp,
    radius: Dp = 999.dp,
    mono: Boolean = false,
    fontSize: TextUnit = if (mono) 12.sp else 13.sp,
    horizontalPadding: Dp = 14.dp,
    borderWidth: Dp = 1.dp,
) {
    val c = Eirs.colors
    val (bg, fg, bd) = when {
        !selected -> Triple(Color.White, c.ink, c.line)
        style == ChipStyle.Solid -> Triple(c.ink, Color.White, c.ink)
        else -> Triple(c.tint1, c.brandDark, c.brand)
    }
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.height(height).clip(shape).background(bg).border(borderWidth, bd, shape).plainClickable(onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, maxLines = 1,
            style = (if (mono) EirsType.mono(fontSize) else EirsType.sans(fontSize, FontWeight.Medium)).copy(color = fg),
        )
    }
}

/** Horizontally scrolling chip row that bleeds to the screen edges like the design's chip rails. */
@Composable
fun ChipRail(modifier: Modifier = Modifier, edge: Dp = 16.dp, spacing: Dp = 6.dp, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().layout { measurable, constraints ->
            // Extend the rail into the parent's horizontal padding so chips scroll edge to edge.
            val extra = (edge * 2).roundToPx()
            val p = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.maxWidth + extra))
            layout(constraints.maxWidth, p.height) { p.place(-edge.roundToPx(), 0) }
        }.horizontalScroll(rememberScrollState()).padding(horizontal = edge),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun Badge(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp,
    fontSize: TextUnit = 10.sp,
    weight: FontWeight = FontWeight.SemiBold,
    tracking: Double = 0.04,
    horizontal: Dp = 7.dp,
    vertical: Dp = 3.dp,
) {
    Text(
        text, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(radius)).background(background).padding(horizontal = horizontal, vertical = vertical),
        style = EirsType.sans(fontSize, weight).copy(color = content, letterSpacing = tracking.em),
    )
}

/** Small mono tag such as a tax code ("VAT15") on a tinted background. */
@Composable
fun CodeTag(text: String, modifier: Modifier = Modifier, background: Color = Eirs.colors.tint1, content: Color = Eirs.colors.brandDark, fontSize: TextUnit = 10.sp) {
    Text(
        text, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(5.dp)).background(background).padding(horizontal = 6.dp, vertical = 2.dp),
        style = EirsType.mono(fontSize).copy(color = content),
    )
}

data class SegmentOption<T>(val value: T, val label: String)

/** Grey track with a raised white thumb for the selected option (document type, buyer mode). */
@Composable
fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
    fontSize: TextUnit = 13.sp,
    solidSelection: Boolean = false,
) {
    val c = Eirs.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.background).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(3.dp),
    ) {
        options.forEach { o ->
            val on = o.value == selected
            val bg = when { !on -> Color.Transparent; solidSelection -> c.ink; else -> Color.White }
            val fg = when { !on -> if (solidSelection) c.ink else c.muted; solidSelection -> Color.White; else -> c.ink }
            Box(
                Modifier.weight(1f).height(height)
                    .then(if (on && !solidSelection) Modifier.shadowLite() else Modifier)
                    .clip(RoundedCornerShape(9.dp)).background(bg).plainClickable { onSelect(o.value) },
                contentAlignment = Alignment.Center,
            ) { Text(o.label, style = EirsType.sans(fontSize, FontWeight.Medium).copy(color = fg), maxLines = 1) }
        }
    }
}

private fun Modifier.shadowLite(): Modifier = this.then(Modifier.shadow(1.5.dp, RoundedCornerShape(9.dp), ambientColor = Color(0x260B2A33), spotColor = Color(0x260B2A33)))

/** 42×26 pill switch with a sliding knob. */
@Composable
fun EirsSwitch(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false) {
    val c = Eirs.colors
    val w = if (small) 40.dp else 42.dp
    val h = if (small) 24.dp else 26.dp
    val knob = if (small) 16.dp else 18.dp
    val offset by animateDpAsState(if (checked) (if (small) 19.dp else 19.dp) else 3.dp, label = "knob")
    val bg by animateColorAsState(if (checked) c.brand else c.line, label = "track")
    Box(modifier.size(w, h).clip(CircleShape).background(bg).plainClickable(onClick = onToggle)) {
        Box(Modifier.padding(start = offset, top = (h - knob) / 2).size(knob).clip(CircleShape).background(Color.White))
    }
}

/** Rounded checkbox square used for "I agree to the Terms". */
@Composable
fun EirsCheckbox(checked: Boolean, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    Box(
        modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(if (checked) c.brand else Color.White)
            .border(1.5.dp, if (checked) c.brand else c.line, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) { if (checked) LineIcon(EirsIcons.Check, size = 12.dp, strokeWidth = 3f, tint = Color.White) }
}

// ---------------------------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------------------------

/** White card with a hairline border (radius 16) — the building block of every app screen. */
@Composable
fun EirsCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    radius: Dp = 16.dp,
    background: Color = Color.White,
    borderColor: Color = Eirs.colors.line,
    spacing: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier.fillMaxWidth().clip(shape).background(background).border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.plainClickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/**
 * Tappable list card: brand border + soft shadow while pressed (the design's hover/active state),
 * tinted background on press.
 */
@Composable
fun ListCard(onClick: () -> Unit, modifier: Modifier = Modifier, alpha: Float = 1f, content: @Composable ColumnScope.() -> Unit) {
    val c = Eirs.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val border by animateColorAsState(if (pressed) c.brand else c.line, label = "cardBorder")
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().alpha(alpha)
            .then(if (pressed) Modifier.shadow(10.dp, shape, ambientColor = Color(0x660B2A33), spotColor = Color(0x660B2A33)) else Modifier)
            .clip(shape).background(if (pressed) c.tint1 else Color.White).border(1.dp, border, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content,
    )
}

/** Label / value row with a hairline on top, used in party, document and payment cards. */
@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, mono: Boolean = true, topBorder: Boolean = true) {
    val c = Eirs.colors
    Row(
        modifier.fillMaxWidth().then(if (topBorder) Modifier.topHairline(c.line) else Modifier).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(key, style = EirsType.sans(13.sp).copy(color = c.muted), maxLines = 1)
        Text(
            value, modifier = Modifier.weight(1f), textAlign = TextAlign.End,
            style = (if (mono) EirsType.mono(12.sp) else EirsType.sans(13.sp)).copy(color = c.ink),
        )
    }
}

/** A titled card of key/value rows ("DOCUMENT", "SOURCE SYSTEM", "PAYMENT"). */
@Composable
fun KeyValueCard(title: String, rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    EirsCard(modifier, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Eyebrow(title, color = Eirs.colors.muted, size = 10.sp, tracking = 0.12, modifier = Modifier.padding(bottom = 6.dp))
        rows.forEach { (k, v) -> KeyValueRow(k, v) }
    }
}

fun Modifier.topHairline(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), width.toPx())
}

fun Modifier.bottomHairline(color: Color, width: Dp = 1.dp): Modifier = drawBehind {
    val y = size.height - width.toPx() / 2
    drawLine(color, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), width.toPx())
}

/** Dashed empty-state box: "No documents match these filters." */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).dashedBorder(c.line, 14.dp)
            .padding(horizontal = 20.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = EirsType.sans(14.sp).copy(color = c.muted), textAlign = TextAlign.Center) }
}

/** Numbered step heading inside a form card: ● 1  Document. */
@Composable
fun StepHeading(number: Int, title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    val c = Eirs.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = EirsType.mono(11.sp).copy(color = Color.White))
        }
        Text(title, modifier = Modifier.weight(1f), style = EirsType.sans(15.sp, FontWeight.SemiBold))
        trailing?.invoke()
    }
}

/** Soft rounded info banner (teal: confirmation, amber: warning, red: canceled). */
@Composable
fun Banner(
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    icon: LineIconSpec? = null,
    textStyle: TextStyle = EirsType.sans(14.sp, FontWeight.Medium, lineHeight = 1.45),
    body: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(background).then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) LineIcon(icon, size = 18.dp, strokeWidth = 2.2f, tint = content, modifier = Modifier.padding(top = 1.dp))
        ProvideTextStyleColor(textStyle.copy(color = content)) { body() }
    }
}

@Composable
fun ProvideTextStyleColor(style: TextStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides style, content = content)
}

/** Thin 1dp divider in the line colour. */
@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Eirs.colors.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** Row of dashed separator used on printed receipts. */
@Composable
fun DashedRule(modifier: Modifier = Modifier, color: Color = Eirs.colors.ink) {
    Box(modifier.fillMaxWidth().height(1.5.dp).drawBehind {
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
    })
}

val BorderHairline @Composable get() = BorderStroke(1.dp, Eirs.colors.line)
