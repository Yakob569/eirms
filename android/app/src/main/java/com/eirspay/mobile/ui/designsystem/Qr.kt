package com.eirspay.mobile.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.eirspay.mobile.ui.theme.Eirs
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** The public MoR verification link encoded in every document QR. */
fun verificationUrl(irn: String) = "https://portal.mor.gov.et/verify?irn=$irn"

/**
 * Real, scannable QR code. Today it encodes the verification URL for the IRN; once the backend
 * returns MoR's `signed_qr` payload, pass that instead.
 */
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, color: Color = Eirs.colors.ink) {
    val matrix = remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    }
    Canvas(modifier) {
        val n = matrix.width
        val cell = size.minDimension / n
        for (y in 0 until n) for (x in 0 until n) {
            if (matrix.get(x, y)) drawRect(color, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

/** Decorative 13×13 QR-like pattern for the onboarding illustration (same generator as the prototype). */
@Composable
fun DecorativeQr(modifier: Modifier = Modifier, color: Color = Eirs.colors.ink) {
    val cells = remember {
        var seed = 7L
        List(169) { i ->
            val r = i / 13; val c = i % 13
            fun f(rr: Int, cc: Int) = rr in 0..3 && cc in 0..3
            if (f(r, c) || f(r, c - 9) || f(r - 9, c)) {
                val rr = if (r >= 9) r - 9 else r; val cc = if (c >= 9) c - 9 else c
                rr == 0 || rr == 3 || cc == 0 || cc == 3 || (rr in 1..2 && cc in 1..2)
            } else { seed = (seed * 9301 + 49297) % 233280; seed / 233280.0 > 0.52 }
        }
    }
    Canvas(modifier) {
        val cell = size.minDimension / 13
        cells.forEachIndexed { i, on -> if (on) drawRect(color, Offset((i % 13) * cell, (i / 13) * cell), Size(cell + 0.5f, cell + 0.5f)) }
    }
}
