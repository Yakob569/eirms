package com.eirspay.mobile.feature.invoices

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eirspay.domain.model.DocumentType
import com.eirspay.domain.model.Invoice
import com.eirspay.domain.usecase.InvoiceRowAction
import com.eirspay.mobile.ui.designsystem.ActionSheetContent
import com.eirspay.mobile.ui.designsystem.EirsBottomSheet
import com.eirspay.mobile.ui.designsystem.EirsIcons
import com.eirspay.mobile.ui.designsystem.Fmt
import com.eirspay.mobile.ui.designsystem.LoadingHost
import com.eirspay.mobile.ui.designsystem.SheetAction

/** "INV-100", "RTN-095": document type + number, as the row-action sheets and page headers show it. */
internal val Invoice.docNo: String get() = "${documentType.name}-$number"

/**
 * The invoice list's ⋮ sheet and the cancel sheet it opens. Drawn by the signed-in shell above the
 * tabs; shares [InvoicesViewModel] with the Invoices tab (same back-stack entry).
 */
@Composable
fun InvoiceRowActionsOverlay(
    onViewDetails: (String) -> Unit,
    onMakeAdjustment: (invoiceId: String, kind: DocumentType) -> Unit,
    onMakeRetainerInvoice: (retainerId: String, kind: DocumentType) -> Unit,
) {
    val vm: InvoicesViewModel = hiltViewModel()
    val s by vm.sheets.collectAsStateWithLifecycle()

    LoadingHost(s.loading) {
        Box(Modifier.fillMaxSize()) {
            EirsBottomSheet(visible = s.actionsOpen, onDismiss = vm::closeActions) {
                val inv = s.actionInvoice
                if (inv != null) {
                    val actions = s.actions.map { a ->
                        when (a) {
                            InvoiceRowAction.VIEW_DETAILS ->
                                SheetAction("View details", EirsIcons.Eye, { vm.closeActions(); onViewDetails(inv.id) })
                            InvoiceRowAction.MAKE_CREDIT_NOTE ->
                                SheetAction("Make credit note", EirsIcons.FileMinus, { vm.closeActions(); onMakeAdjustment(inv.id, DocumentType.CRE) })
                            InvoiceRowAction.MAKE_DEBIT_NOTE ->
                                SheetAction("Make debit note", EirsIcons.FilePlusSmall, { vm.closeActions(); onMakeAdjustment(inv.id, DocumentType.DEB) })
                            InvoiceRowAction.CANCEL ->
                                SheetAction("Cancel invoice", EirsIcons.XCircle, { vm.startCancel(inv.id) }, danger = true, separated = true)
                            InvoiceRowAction.MAKE_INTERIM ->
                                SheetAction("Make interim invoice", EirsIcons.FileLines, { vm.closeActions(); onMakeRetainerInvoice(inv.id, DocumentType.INT) })
                            InvoiceRowAction.MAKE_FINAL ->
                                SheetAction("Make final invoice", EirsIcons.FileLines, { vm.closeActions(); onMakeRetainerInvoice(inv.id, DocumentType.FIN) })
                        }
                    }
                    ActionSheetContent(
                        eyebrow = "${inv.documentType.name} · ${inv.transactionType.name} · ${inv.status.name}",
                        title = inv.docNo,
                        subtitle = inv.buyer.name + " · " + Fmt.etb(inv.totals.grandTotal),
                        actions = actions,
                    )
                }
            }
            EirsBottomSheet(visible = s.cancelOpen, onDismiss = vm::closeCancel) {
                CancelSheetContent(s.cancelInvoice?.number.orEmpty(), s.cancelReason, vm::pickCancelReason, vm::confirmCancel)
            }
        }
    }
}
