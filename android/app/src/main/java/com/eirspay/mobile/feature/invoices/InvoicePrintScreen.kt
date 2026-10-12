package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.model.TaxCode
import com.eirspay.domain.util.AmountInWords
import com.eirspay.mobile.ui.designsystem.BackButton
import com.eirspay.mobile.ui.designsystem.DashedRule
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.QrCode
import com.eirspay.mobile.ui.designsystem.bottomHairline
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.designsystem.verificationUrl
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/** 80mm thermal-style printable receipt of an invoice. */
@Composable
fun InvoicePrintScreen(id: String, onBack: () -> Unit) {
    val vm: InvoiceDetailViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.bind(id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val c = Eirs.colors
    val inv = state.invoice

    Column(Modifier.fillMaxSize().background(c.paperBackdrop)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).bottomHairline(c.line).statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackButton(onBack)
            Text("Receipt ${inv?.number.orEmpty()}", modifier = Modifier.weight(1f), style = EirsType.sans(16.sp, FontWeight.SemiBold).copy(color = c.ink), maxLines = 1)
            // Download / share the receipt as PDF — not wired yet.
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.brand).plainClickable { },
                contentAlignment = Alignment.Center,
            ) { LineIcon(EirsIcons.Download, size = 17.dp, tint = Color.White) }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 40.dp).navigationBarsPadding(),
        ) {
            if (inv != null) Paper(inv)
        }
    }
}

@Composable
private fun Paper(inv: Invoice) {
    val c = Eirs.colors
    val t = inv.totals
    val base = EirsType.sans(11.sp).copy(color = c.ink)
    val muted = base.copy(color = c.muted)
    val monoBold = EirsType.mono(11.sp, FontWeight.Medium).copy(color = c.ink)
    val consumer = inv.transactionType.isConsumer
    val title = when {
        inv.documentType == DocumentType.CRE -> "Credit note · VAT"
        consumer -> "Cash sales invoice · VAT"
        else -> "Tax invoice · VAT"
    }
    val sub = "የሽያጭ ደረሰኝ · " + inv.transactionType.name + if (t.taxes.any { it.code == TaxCode.EXC10 }) " · EXCISE" else ""
    val s = inv.seller

    Box(
        Modifier.fillMaxWidth()
            .shadow(16.dp, ambientColor = Color(0x800B2A33), spotColor = Color(0x800B2A33))
            .background(Color.White).clipToBounds(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Seller header
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(s.name, style = EirsType.sans(14.sp, FontWeight.Bold).copy(color = c.ink), textAlign = TextAlign.Center)
                    Text("TIN ${s.tin ?: "—"} · VAT ${s.vatNumber ?: "—"}", style = muted, textAlign = TextAlign.Center)
                    s.address?.let { Text(it, style = muted, textAlign = TextAlign.Center) }
                }
                DashedRule()
            }
            // Title
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = EirsType.serif(20.sp).copy(color = c.ink), textAlign = TextAlign.Center)
                Text(sub, style = EirsType.mono(10.sp).copy(color = c.muted, letterSpacing = 0.08.em), textAlign = TextAlign.Center)
            }
            // No / Date / Time / Buyer
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                GridRow("የሰነድ ቁጥር / No", inv.number, muted, monoBold)
                GridRow("ቀን / Date", Fmt.isoDate(inv.issuedAt), muted, monoBold)
                GridRow("ሰዓት / Time", Fmt.time(inv.issuedAt) + ":00", muted, monoBold)
                GridRow("ገዢ / Buyer", inv.buyer.name, muted, base.copy(fontWeight = FontWeight.Bold))
            }
            // Items between dashed rules
            Column(Modifier.fillMaxWidth()) {
                DashedRule()
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    t.lines.forEach { l ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(l.item.description, style = base)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "${qtyText(l.item.quantity)} × ${Fmt.f2(l.item.unitPrice)} · ${l.item.taxCode.name}",
                                    style = EirsType.mono(10.sp).copy(color = c.muted), modifier = Modifier.weight(1f),
                                )
                                Text(Fmt.f2(l.total), style = EirsType.mono(11.sp, FontWeight.Medium).copy(color = c.ink))
                            }
                        }
                    }
                }
                DashedRule()
            }
            // Totals
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taxable total", style = muted)
                    Text(Fmt.f2(t.subtotal), style = EirsType.mono(11.sp).copy(color = c.ink))
                }
                t.taxes.forEach { tx ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tx.code.label, style = muted)
                        Text(Fmt.f2(tx.tax), style = EirsType.mono(11.sp).copy(color = c.ink))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp)
                        .drawBehind { drawLine(c.ink, Offset(0f, 0f), Offset(size.width, 0f), 1.5.dp.toPx()) }
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ጠቅላላ / Total", style = EirsType.sans(14.sp, FontWeight.Bold).copy(color = c.ink))
                    Text(Fmt.etb(t.grandTotal), style = EirsType.mono(14.sp, FontWeight.Medium).copy(color = c.ink))
                }
                Text(
                    AmountInWords.birr(t.grandTotal),
                    style = EirsType.serif(13.sp, lineHeight = 1.35).copy(fontStyle = FontStyle.Italic, color = c.muted),
                )
            }
            // QR + IRN + payment
            Column(Modifier.fillMaxWidth()) {
                DashedRule()
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(6.dp)) {
                        QrCode(verificationUrl(inv.irn), Modifier.fillMaxSize())
                    }
                    Text(
                        "IRN ${inv.irn}", textAlign = TextAlign.Center,
                        style = EirsType.mono(9.sp).copy(color = c.ink, lineHeight = (9 * 1.5).sp),
                    )
                    Text(
                        buildAnnotatedString {
                            append("የክፍያ ሁኔታ / Payment · ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append(inv.paymentMode.code) }
                        },
                        style = muted, textAlign = TextAlign.Center,
                    )
                    Text("Verify at portal.mor.gov.et", style = muted, textAlign = TextAlign.Center)
                }
            }
        }
        if (inv.isCanceled) {
            Text(
                "CANCELED",
                modifier = Modifier.align(BiasAlignment(0f, -0.2f)).graphicsLayer { rotationZ = -20f; alpha = 0.14f },
                style = EirsType.serif(64.sp).copy(color = c.red), maxLines = 1, softWrap = false,
            )
        }
    }
}

@Composable
private fun GridRow(label: String, value: String, labelStyle: TextStyle, valueStyle: TextStyle) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = labelStyle)
        Text(value, style = valueStyle, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

