package com.eirspay.mobile.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.sp

object Eirs {
    val colors: EirsColors @Composable @ReadOnlyComposable get() = LocalEirsColors.current
}

@Composable
fun EirsPayTheme(palette: BrandPalette = BrandPalette.Teal, content: @Composable () -> Unit) {
    val c = palette.colors
    val scheme = lightColorScheme(
        primary = c.brand, onPrimary = c.surface, secondary = c.ink, background = c.background,
        surface = c.surface, onSurface = c.ink, onBackground = c.ink, outline = c.line, error = c.red,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalEirsColors provides c,
            LocalTextStyle provides EirsType.sans(15.sp).copy(color = c.ink),
            LocalTextSelectionColors provides TextSelectionColors(handleColor = c.brand, backgroundColor = c.tint2),
            content = content,
        )
    }
}
