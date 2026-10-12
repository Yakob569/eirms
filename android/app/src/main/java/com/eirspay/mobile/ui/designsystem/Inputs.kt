package com.eirspay.mobile.ui.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/**
 * The design's input: 52dp, radius 12, tinted background; on focus the border turns brand,
 * a 4dp tint ring appears and the fill goes white. Text is 16sp so the IME never zooms.
 */
@Composable
fun EirsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    height: Dp = 52.dp,
    mono: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRing: Boolean = true,
    restingBackground: Color = Eirs.colors.background,
    fontSize: TextUnit = 16.sp,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    horizontalPadding: Dp = 14.dp,
    readOnly: Boolean = false,
) {
    val c = Eirs.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) c.brand else c.line, label = "fieldBorder")
    val shape = RoundedCornerShape(12.dp)
    val style = (if (mono) EirsType.mono(fontSize) else EirsType.sans(fontSize)).copy(color = c.ink, textAlign = textAlign)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = readOnly,
        textStyle = style,
        cursorBrush = SolidColor(c.brand),
        interactionSource = source,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions.Default,
        decorationBox = { inner ->
            Box(
                Modifier
                    .then(if (focused && focusRing) Modifier.border(4.dp, c.tint1, RoundedCornerShape(16.dp)).padding(0.dp) else Modifier)
                    .height(height).fillMaxWidth().clip(shape)
                    .background(if (focused) Color.White else restingBackground)
                    .border(1.dp, border, shape),
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    leading?.invoke()
                    Box(Modifier.weight(1f).padding(horizontal = horizontalPadding), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, modifier = Modifier.fillMaxWidth(), style = style.copy(color = c.placeholder), maxLines = 1)
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            }
        },
    )
}

/** Field with its label stacked above (CSS `<label>` wrapping). */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 14.sp,
    gap: Dp = 8.dp,
    trailingLabel: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = EirsType.sans(labelSize, FontWeight.Medium))
            trailingLabel?.invoke()
        }
        content()
    }
}

/** Password field with the 44dp show/hide eye button inside the right edge. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "8+ characters",
    height: Dp = 52.dp,
    imeAction: ImeAction = ImeAction.Next,
) {
    val c = Eirs.colors
    EirsTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, placeholder = placeholder, height = height,
        keyboardType = KeyboardType.Password, imeAction = imeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            Box(
                Modifier.padding(end = 4.dp).size(44.dp)
                    .pressBackground(Color.Transparent, c.tint1, RoundedCornerShape(10.dp), onClick = onToggleVisible),
                contentAlignment = Alignment.Center,
            ) { LineIcon(EirsIcons.Eye, size = 19.dp, tint = if (visible) c.brand else c.muted) }
        },
    )
}

/** White search box with magnifier (lists). */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = Eirs.colors
    EirsTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, placeholder = placeholder, height = 46.dp,
        focusRing = false, restingBackground = Color.White, imeAction = ImeAction.Search, horizontalPadding = 10.dp,
        leading = { LineIcon(EirsIcons.Search, size = 16.dp, tint = c.muted, modifier = Modifier.padding(start = 12.dp)) },
    )
}

/** Compact form input used inside register forms (46dp, no focus ring). */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
    keyboardType: KeyboardType = KeyboardType.Text,
    restingBackground: Color = Eirs.colors.background,
    fontSize: TextUnit = 16.sp,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    height: Dp = 46.dp,
) = EirsTextField(
    value = value, onValueChange = onValueChange, modifier = modifier, placeholder = placeholder, height = height,
    mono = mono, textAlign = textAlign, keyboardType = keyboardType, focusRing = false,
    restingBackground = restingBackground, horizontalPadding = 12.dp, fontSize = fontSize,
    readOnly = readOnly, visualTransformation = visualTransformation,
)

/** Multi-line white text area (radius 14) for free text such as a credit note's reason. */
@Composable
fun TextAreaField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 3,
) {
    val c = Eirs.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val style = EirsType.sans(16.sp, lineHeight = 1.4).copy(color = c.ink)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        textStyle = style,
        cursorBrush = SolidColor(c.brand),
        interactionSource = source,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(shape).background(Color.White).border(1.dp, if (focused) c.brand else c.line, shape).padding(12.dp),
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = style.copy(color = c.placeholder))
                inner()
            }
        },
    )
}

/** Keeps only digits and one decimal point, for money inputs. */
fun String.decimalOnly(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot < 0) cleaned else cleaned.substring(0, dot + 1) + cleaned.substring(dot + 1).replace(".", "")
}
