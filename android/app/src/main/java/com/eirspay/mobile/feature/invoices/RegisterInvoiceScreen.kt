package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.BuyerIdType
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.InvoicePaymentMode
import com.eirspay.domain.model.PaymentTerm
import com.eirspay.domain.model.TaxCode
import com.eirspay.domain.model.TransactionType
import com.eirspay.mobile.ui.designsystem.ChipRail
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsChip
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.EirsSwitch
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.FormField
import com.eirspay.mobile.ui.designsystem.LabeledField
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.MonoText
import com.eirspay.mobile.ui.designsystem.PrimaryButton
import com.eirspay.mobile.ui.designsystem.SegmentOption
import com.eirspay.mobile.ui.designsystem.SegmentedControl
import com.eirspay.mobile.ui.designsystem.StepHeading
import com.eirspay.mobile.ui.designsystem.StickyBottomBar
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.ViewTitle
import com.eirspay.mobile.ui.designsystem.dashedBorder
import com.eirspay.mobile.ui.designsystem.plainClickable
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

private val DocOptions = listOf(
    SegmentOption(DocumentType.INV, "Invoice"),
    SegmentOption(DocumentType.CRE, "Credit note"),
    SegmentOption(DocumentType.DEB, "Debit note"),
)

private val TtOrder = listOf(TransactionType.B2C, TransactionType.B2B, TransactionType.B2G, TransactionType.G2B, TransactionType.G2C)

private fun docWord(t: DocumentType) = when (t) {
    DocumentType.INV -> "invoice."
    DocumentType.CRE -> "credit note."
    DocumentType.DEB -> "debit note."
    DocumentType.RTN -> "retainer."
    DocumentType.INT -> "interim invoice."
    DocumentType.FIN -> "final invoice."
}

@Composable
fun RegisterInvoiceScreen(onBack: () -> Unit, onRegistered: (String) -> Unit) {
    val vm: RegisterInvoiceViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.registered.collect { onRegistered(it) } }
    val c = Eirs.colors

    LoadingHost(s.loading) {
        Column(Modifier.fillMaxSize().background(c.background)) {
            ViewHeader(
                eyebrow = "NEW · " + s.branch.displayName.uppercase(),
                onBack = onBack,
                title = { ViewTitle("Register ", docWord(s.documentType)) },
            )
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DocumentCard(s, vm)
                BuyerCard(s, vm)
                LinesCard(s, vm)
                PaymentCard(s, vm)
            }
            StickyBottomBar {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Bill to ${s.billTo}", style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(if (s.valid) c.brand else c.pending))
                            Text("${s.documentType.name} · ${s.transactionType.name}", style = EirsType.sans(11.sp).copy(color = c.muted))
                        }
                    }
                    Text(Fmt.etb(s.grandTotal), style = EirsType.serif(28.sp).copy(color = c.ink), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
                PrimaryButton(
                    "Register with MoR", onClick = vm::submit, enabled = s.valid,
                    height = 50.dp, fontSize = 15.sp, horizontalPadding = 18.dp,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    LabeledField(label, modifier = modifier, labelSize = 13.sp, gap = 6.dp, content = content)

@Composable
private fun DocumentCard(s: RegisterInvoiceUiState, vm: RegisterInvoiceViewModel) {
    val c = Eirs.colors
    EirsCard(spacing = 14.dp) {
        StepHeading(1, "Document")
        SegmentedControl(DocOptions, s.documentType, vm::setDocumentType)
        ChipRail(spacing = 8.dp) {
            TtOrder.forEach { tt ->
                val on = tt == s.transactionType
                val shape = RoundedCornerShape(12.dp)
                Column(
                    Modifier.width(112.dp).clip(shape).background(if (on) c.tint1 else Color.White)
                        .border(1.5.dp, if (on) c.brand else c.line, shape).plainClickable { vm.setTransactionType(tt) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    MonoText(tt.name, size = 15.sp, weight = FontWeight.Medium)
                    Text(tt.displayName, style = EirsType.sans(11.sp, lineHeight = 1.3).copy(color = c.muted), minLines = 2)
                }
            }
        }
        Text(
            if (s.consumer) "Consumer sale: buyer ID is optional for walk-in customers."
            else "Business or government sale: buyer TIN and legal name are required by MoR.",
            style = EirsType.sans(12.sp, lineHeight = 1.45).copy(color = c.muted),
        )
        if (s.documentType.isAmendment) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.tint1).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FieldLabel("Previous IRN") {
                    FormField(s.previousIrn, vm::setPreviousIrn, placeholder = "IRN being amended", restingBackground = Color.White)
                }
                FieldLabel("Reason") {
                    FormField(s.amendReason, vm::setAmendReason, placeholder = "e.g. Returned goods", restingBackground = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuyerCard(s: RegisterInvoiceUiState, vm: RegisterInvoiceViewModel) {
    val c = Eirs.colors
    EirsCard(spacing = 14.dp) {
        StepHeading(2, "Buyer") {
            Text(
                if (s.consumer) "Consumer · light ID" else "Business · TIN required",
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(c.tint1).padding(horizontal = 8.dp, vertical = 3.dp),
                style = EirsType.sans(11.sp, FontWeight.SemiBold).copy(color = c.brandDark), maxLines = 1,
            )
        }
        if (s.consumer) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.line, RoundedCornerShape(12.dp))
                    .plainClickable(onClick = vm::toggleWalkIn).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EirsSwitch(s.walkIn, onToggle = vm::toggleWalkIn, small = true)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Walk-in customer", style = EirsType.sans(14.sp, FontWeight.Medium).copy(color = c.ink))
                    Text("No buyer ID needed", style = EirsType.sans(12.sp).copy(color = c.muted))
                }
            }
            if (!s.walkIn) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BuyerIdType.entries.forEach { t ->
                        EirsChip(
                            t.name, selected = s.idType == t, onClick = { vm.setIdType(t) },
                            height = 34.dp, radius = 9.dp, mono = true, fontSize = 12.sp, horizontalPadding = 11.dp,
                        )
                    }
                }
                FieldLabel("Full name") { FormField(s.name, vm::setName, placeholder = "Selam Bekele") }
                FieldLabel("${s.idType.name} number") { FormField(s.idNumber, vm::setIdNumber, placeholder = "ID number") }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                FieldLabel("Buyer TIN *", modifier = Modifier.weight(1f)) {
                    FormField(s.tin, vm::setTin, placeholder = "10-digit TIN", keyboardType = KeyboardType.Number)
                }
                Box(
                    Modifier.height(46.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, c.brand, RoundedCornerShape(12.dp))
                        .alpha(if (s.lookingUp) 0.6f else 1f).plainClickable(onClick = vm::lookup).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Look up", style = EirsType.sans(14.sp, FontWeight.SemiBold).copy(color = c.brand), maxLines = 1) }
            }
            FieldLabel("Legal name *") { FormField(s.name, vm::setName, placeholder = "Dashen Trading PLC") }
            FieldLabel("VAT number") { FormField(s.vat, vm::setVat, placeholder = "Optional") }
        }
    }
}

@Composable
private fun LinesCard(s: RegisterInvoiceUiState, vm: RegisterInvoiceViewModel) {
    val c = Eirs.colors
    EirsCard(spacing = 12.dp) {
        StepHeading(3, "Line items")
        if (s.quickAdd.isNotEmpty()) {
            ChipRail {
                s.quickAdd.forEach { item ->
                    Box(
                        Modifier.height(34.dp).clip(RoundedCornerShape(999.dp)).background(c.tint1).dashedBorder(c.tint2, 17.dp)
                            .plainClickable { vm.quickAdd(item) }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("+ ${item.description}", style = EirsType.sans(12.sp, FontWeight.Medium).copy(color = c.brandDark), maxLines = 1) }
                }
            }
        }
        s.lines.forEach { line ->
            LineDraftEditor(line, onChange = vm::replaceLine, onRemove = { vm.removeLine(line.key) })
        }
        AddLineButton(onClick = vm::addLine)
    }
}

@Composable
private fun PaymentCard(s: RegisterInvoiceUiState, vm: RegisterInvoiceViewModel) {
    EirsCard(spacing = 12.dp) {
        StepHeading(4, "Payment")
        PaymentPicker(s.paymentMode, s.paymentTerm, vm::setPaymentMode, vm::setPaymentTerm)
    }
}
