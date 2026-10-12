package com.eirspay.mobile.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.R

/** Instrument Serif for display, Geist for UI, Geist Mono for codes, IRNs and amounts. */
object EirsFonts {
    val Serif = FontFamily(
        Font(R.font.instrument_serif_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
    )
    val Sans = FontFamily(
        Font(R.font.geist_regular, FontWeight.Normal),
        Font(R.font.geist_medium, FontWeight.Medium),
        Font(R.font.geist_semibold, FontWeight.SemiBold),
        Font(R.font.geist_bold, FontWeight.Bold),
    )
    val Mono = FontFamily(
        Font(R.font.geist_mono_regular, FontWeight.Normal),
        Font(R.font.geist_mono_medium, FontWeight.Medium),
    )
}

object EirsType {
    fun serif(size: TextUnit, lineHeight: Double = 1.0) =
        TextStyle(fontFamily = EirsFonts.Serif, fontSize = size, lineHeight = (size.value * lineHeight).sp)

    fun sans(size: TextUnit, weight: FontWeight = FontWeight.Normal, lineHeight: Double? = null) = TextStyle(
        fontFamily = EirsFonts.Sans, fontSize = size, fontWeight = weight,
        lineHeight = lineHeight?.let { (size.value * it).sp } ?: TextUnit.Unspecified,
    )

    fun mono(size: TextUnit, weight: FontWeight = FontWeight.Normal) =
        TextStyle(fontFamily = EirsFonts.Mono, fontSize = size, fontWeight = weight)

    /** Uppercase mono label with wide tracking ("01 · EIRS-REGISTERED", "PROOF OF REGISTRATION"). */
    fun eyebrow(size: TextUnit = 11.sp, tracking: Double = 0.14) =
        TextStyle(fontFamily = EirsFonts.Mono, fontSize = size, letterSpacing = tracking.em)
}
