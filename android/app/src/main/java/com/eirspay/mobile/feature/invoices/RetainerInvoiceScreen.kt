package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.DocumentType
import com.eirspay.mobile.ui.designsystem.EirsCard
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.FormField
import com.eirspay.mobile.ui.designsystem.LabeledField
import com.eirspay.mobile.ui.designsystem.LineIcon
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.StepHeading
import com.eirspay.mobile.ui.designsystem.ViewHeader
import com.eirspay.mobile.ui.designsystem.ViewScaffold
import com.eirspay.mobile.ui.designsystem.ViewTitle
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsFonts
import com.eirspay.mobile.ui.theme.EirsType

private data class BuyerFieldSpec(val label: String, val mono: Boolean, val get: (BuyerForm) -> String, val set: (BuyerForm, String) -> BuyerForm)

private val BuyerFields = listOf(
    BuyerFieldSpec("TIN", true, { it.tin }, { b, v -> b.copy(tin = v) }),
    BuyerFieldSpec("VAT number", true, { it.vat }, { b, v -> b.copy(vat = v) }),
    BuyerFieldSpec("Phone", true, { it.phone }, { b, v -> b.copy(phone = v) }),
    BuyerFieldSpec("Email", false, { it.email }, { b, v -> b.copy(email = v) }),
    BuyerFieldSpec("Region", false, { it.region }, { b, v -> b.copy(region = v) }),
    BuyerFieldSpec("City", false, { it.city }, { b, v -> b.copy(city = v) }),
    BuyerFieldSpec("Wereda", true, { it.wereda }, { b, v -> b.copy(wereda = v) }),
)

/**
 * Interim or final invoice against a retainer. Reads as a full form: only the retainer reference
 * and the document type are locked (chips at the top); buyer, items and payment are editable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RetainerInvoiceScreen(retainerId: String, kind: DocumentType, onClose: () -> Unit, onRegistered: (String) -> Unit) {
    val vm: RetainerInvoiceViewModel = hiltViewModel()
    LaunchedEffect(retainerId, kind) { vm.bind(retainerId, kind) }
    val s by vm.state.collectAsStateWithLifecycle()
    val registered by rememberUpdatedState(onRegistered)
    LaunchedEffect(vm) { vm.registered.collect { registered(it) } }
    val c = Eirs.colors
    val srcNo = s.retainer?.docNo.orEmpty()

    LoadingHost(s.loading) {
        ViewScaffold(
            header = {
                ViewHeader(
                    eyebrow = (if (kind == DocumentType.INT) "INTERIM INVOICE" else "FINAL INVOICE") + " · AGAINST " + srcNo,
                    onBack = onClose, backIcon = EirsIcons.Close,
                    title = { ViewTitle(kind.displayName) },
                )
            },
            bottomBar = {
                DocumentSubmitFooter(
                    label = "Bill to " + s.buyer.name.trim().ifBlank { "—" },
                    ready = s.ready, hint = s.hint, total = Fmt.etb(s.grandTotal),
                    submitLabel = "Submit " + kind.displayName.lowercase(), onSubmit = vm::submit,
                )
            },
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LockedChip("Retainer", srcNo, locked = true)
                LockedChip("Document type", kind.name, locked = true)
                LockedChip("Transaction", s.retainer?.transactionType?.name.orEmpty(), locked = false)
            }

            EirsCard(spacing = 12.dp) {
                StepHeading(1, "Buyer") { Text("From the retainer", style = EirsType.sans(11.sp).copy(color = c.muted)) }
                LabeledField("Legal name *", labelSize = 13.sp, gap = 6.dp) {
                    FormField(s.buyer.name, { v -> vm.editBuyer { it.copy(name = v) } })
                }
                BuyerFields.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { spec ->
                            LabeledField(spec.label, modifier = Modifier.weight(1f), labelSize = 13.sp, gap = 6.dp) {
                                FormField(spec.get(s.buyer), { v -> vm.editBuyer { spec.set(it, v) } }, mono = spec.mono, fontSize = 15.sp)
                            }
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            EirsCard(spacing = 12.dp) {
                StepHeading(2, "Items")
                s.lines.forEachIndexed { i, line ->
                    LineDraftEditor(
                        line, onChange = vm::replaceLine, onRemove = if (s.lines.size > 1) ({ vm.removeLine(line.key) }) else null,
                        detailed = true, number = i + 1,
                    )
                }
                AddLineButton(onClick = vm::addLine)
            }

            EirsCard(spacing = 12.dp) {
                StepHeading(3, "Payment")
                PaymentPicker(s.paymentMode, s.paymentTerm, vm::setPaymentMode, vm::setPaymentTerm, labelled = true)
            }

            TotalsCard(listOf("Taxable" to Fmt.f2(s.subtotal)) + s.taxes.map { (code, tax) -> code.label to Fmt.f2(tax) })
        }
    }
}

/** Pill showing a value fixed for this document ("🔒 Retainer RTN-095"). */
@Composable
private fun LockedChip(label: String, value: String, locked: Boolean) {
    val c = Eirs.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier.height(34.dp).clip(shape).background(Color.White).border(1.dp, c.line, shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (locked) LineIcon(EirsIcons.Lock, size = 12.dp, tint = c.muted)
        Text(
            buildAnnotatedString {
                append("$label ")
                withStyle(SpanStyle(fontFamily = EirsFonts.Mono, fontWeight = FontWeight.Medium, color = c.ink)) { append(value) }
            },
            style = EirsType.sans(12.sp).copy(color = c.muted), maxLines = 1,
        )
    }
}
