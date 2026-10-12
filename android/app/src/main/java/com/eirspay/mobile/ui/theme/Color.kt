package com.eirspay.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Brand tokens from the design (CSS vars --b, --bd, --ink, --t1, --t2, --bg, --ln, --mu). */
@Immutable
data class EirsColors(
    val brand: Color,
    val brandDark: Color,
    val ink: Color,
    val tint1: Color,
    val tint2: Color,
    val background: Color,
    val line: Color,
    val muted: Color,
    val surface: Color = Color.White,
    val placeholder: Color = Color(0xFF9AAEB3),
    val red: Color = Color(0xFFB42318),
    val redTint: Color = Color(0xFFFDECEA),
    val redBorder: Color = Color(0xFFF3C5BF),
    val redInk: Color = Color(0xFF7A2A20),
    val amberTint: Color = Color(0xFFFFF6E0),
    val amberBorder: Color = Color(0xFFF1DDA6),
    val amberInk: Color = Color(0xFF6B4700),
    val warnTint: Color = Color(0xFFFFF3DC),
    val warnInk: Color = Color(0xFF8A5A00),
    val paidTint: Color = Color(0xFFE4F2EA),
    val paidInk: Color = Color(0xFF12704F),
    val neutralChip: Color = Color(0xFFEEF1F2),
    val pending: Color = Color(0xFFC9A227),
    val strengthFair: Color = Color(0xFFC98A00),
    val paperBackdrop: Color = Color(0xFFE4EAEB),
    val scrim: Color = Color(0x660B2A33),
)

/** Teal is the chosen brand; green and blue were the alternates explored in the design. */
enum class BrandPalette(val colors: EirsColors) {
    Teal(EirsColors(Color(0xFF0E7C86), Color(0xFF0A636B), Color(0xFF0B2A33), Color(0xFFE6F3F4), Color(0xFFC9E5E7), Color(0xFFF5F9F9), Color(0xFFD7E5E6), Color(0xFF4A6570))),
    Green(EirsColors(Color(0xFF12704F), Color(0xFF0C5A3F), Color(0xFF0A2E24), Color(0xFFE7F2EC), Color(0xFFC8E3D4), Color(0xFFF5FAF7), Color(0xFFD4E4DA), Color(0xFF4B665A))),
    Blue(EirsColors(Color(0xFF2150D6), Color(0xFF1A40AE), Color(0xFF0C1B48), Color(0xFFEAEFFD), Color(0xFFCFDAF9), Color(0xFFF6F8FE), Color(0xFFD9E0F2), Color(0xFF4D5877))),
}

val LocalEirsColors = staticCompositionLocalOf { BrandPalette.Teal.colors }
