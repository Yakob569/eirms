package com.eirspay.domain.usecase

import com.eirspay.domain.model.*
import com.eirspay.domain.repository.InvoiceDraft
import com.eirspay.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject

/** Options in an invoice card's ⋮ sheet. */
enum class InvoiceRowAction { VIEW_DETAILS, MAKE_CREDIT_NOTE, MAKE_DEBIT_NOTE, CANCEL, MAKE_INTERIM, MAKE_FINAL }

/** Options in a receipt card's ⋮ sheet. */
enum class ReceiptRowAction { VIEW_DETAILS, VIEW_LINKED_INVOICE }

object RowActionPolicy {
    /** True when [invoice] already has a live (not canceled) credit or debit note of [type]. */
    fun hasAmendment(invoice: Invoice, type: DocumentType, all: List<Invoice>): Boolean =
        all.any { it.documentType == type && !it.isCanceled && it.amendment?.previousInvoiceId == invoice.id }

    /**
     * INV: details, credit note and debit note (each hidden once one exists), cancel.
     * RTN: details, interim and final invoice. CRE/DEB/INT/FIN and canceled documents: details only.
     */
    fun invoiceActions(invoice: Invoice, all: List<Invoice>): List<InvoiceRowAction> = buildList {
        add(InvoiceRowAction.VIEW_DETAILS)
        if (invoice.isCanceled) return@buildList
        when (invoice.documentType) {
            DocumentType.INV -> {
                if (!hasAmendment(invoice, DocumentType.CRE, all)) add(InvoiceRowAction.MAKE_CREDIT_NOTE)
                if (!hasAmendment(invoice, DocumentType.DEB, all)) add(InvoiceRowAction.MAKE_DEBIT_NOTE)
                add(InvoiceRowAction.CANCEL)
            }
            DocumentType.RTN -> {
                add(InvoiceRowAction.MAKE_INTERIM)
                add(InvoiceRowAction.MAKE_FINAL)
            }
            else -> Unit
        }
    }

    /** "View linked invoice" only when the receipt carries a linked invoice. */
    fun receiptActions(view: ReceiptView): List<ReceiptRowAction> =
        listOfNotNull(ReceiptRowAction.VIEW_DETAILS, ReceiptRowAction.VIEW_LINKED_INVOICE.takeIf { view.linkedInvoice != null })
}

/** One ticked line of a credit/debit note: the original line [index] with its new quantity and price. */
data class AdjustmentLine(val index: Int, val quantity: BigDecimal, val unitPrice: BigDecimal)

sealed interface AdjustmentValidation {
    data object Ok : AdjustmentValidation
    data object NoLines : AdjustmentValidation
    data object MissingReason : AdjustmentValidation
    /** Credit-note lines above the original quantity or price, by original line index. */
    data class ExceedsOriginal(val indexes: Set<Int>) : AdjustmentValidation
}

object AdjustmentRules {
    /** A credit note can't exceed the original line; a debit note may. */
    fun exceedsOriginal(kind: DocumentType, original: LineItem, quantity: BigDecimal, unitPrice: BigDecimal): Boolean =
        kind == DocumentType.CRE && (quantity > original.quantity || unitPrice > original.unitPrice)

    fun validate(kind: DocumentType, original: Invoice, lines: List<AdjustmentLine>, reason: String): AdjustmentValidation {
        if (lines.isEmpty()) return AdjustmentValidation.NoLines
        if (reason.isBlank()) return AdjustmentValidation.MissingReason
        val bad = lines.filter { l ->
            val item = original.items.getOrNull(l.index) ?: return@filter true
            exceedsOriginal(kind, item, l.quantity, l.unitPrice)
        }.map { it.index }.toSet()
        return if (bad.isEmpty()) AdjustmentValidation.Ok else AdjustmentValidation.ExceedsOriginal(bad)
    }

    /** The note's items: ticked original lines with their new quantity and price, in original order. */
    fun items(original: Invoice, lines: List<AdjustmentLine>): List<LineItem> =
        lines.sortedBy { it.index }.mapNotNull { l ->
            original.items.getOrNull(l.index)?.copy(quantity = l.quantity, unitPrice = l.unitPrice.cents())
        }
}

/**
 * Registers a credit or debit note against an invoice. Seller, buyer, branch, transaction type and
 * payment are copied from the original (MoR requires the buyer to match exactly).
 */
class RegisterAdjustmentNoteUseCase @Inject constructor(private val invoices: InvoiceRepository) {
    suspend operator fun invoke(kind: DocumentType, originalId: String, lines: List<AdjustmentLine>, reason: String): Invoice {
        require(kind.isAmendment) { "Only credit and debit notes adjust an invoice." }
        val all = invoices.observeInvoices().first()
        val original = all.first { it.id == originalId }
        require(original.documentType == DocumentType.INV && !original.isCanceled) { "Only live invoices can be adjusted." }
        check(!RowActionPolicy.hasAmendment(original, kind, all)) { "This invoice already has a ${kind.displayName.lowercase()}." }
        val validation = AdjustmentRules.validate(kind, original, lines, reason)
        require(validation == AdjustmentValidation.Ok) { "Invalid adjustment: $validation" }
        return invoices.register(
            InvoiceDraft(
                documentType = kind, transactionType = original.transactionType, branch = original.branch,
                buyer = original.buyer, paymentMode = original.paymentMode, paymentTerm = original.paymentTerm,
                items = AdjustmentRules.items(original, lines), amendment = Amendment(reason.trim(), original.id),
            ),
        )
    }
}

/**
 * Registers an interim or final invoice against a retainer. Only the retainer reference and the
 * document type are fixed; buyer, items and payment are free (no match-the-original rule).
 */
class RegisterRetainerInvoiceUseCase @Inject constructor(private val invoices: InvoiceRepository) {
    suspend operator fun invoke(
        kind: DocumentType,
        retainerId: String,
        buyer: Party,
        items: List<LineItem>,
        paymentMode: InvoicePaymentMode,
        paymentTerm: PaymentTerm,
    ): Invoice {
        require(kind.isRetainerFollowUp) { "Only interim and final invoices settle a retainer." }
        val retainer = invoices.observeInvoices().first().first { it.id == retainerId }
        require(retainer.documentType == DocumentType.RTN && !retainer.isCanceled) { "Not a live retainer." }
        require(buyer.name.isNotBlank()) { "The buyer's legal name is required." }
        require(items.isNotEmpty()) { "Add at least one priced line item." }
        return invoices.register(
            InvoiceDraft(
                documentType = kind, transactionType = retainer.transactionType, branch = retainer.branch, buyer = buyer,
                paymentMode = paymentMode, paymentTerm = paymentTerm, items = items,
                amendment = Amendment("${kind.displayName} against retainer RTN-${retainer.number}", retainer.id),
            ),
        )
    }
}
