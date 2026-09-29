package com.eirspay.domain.usecase

import com.eirspay.domain.model.*
import com.eirspay.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import javax.inject.Inject

class ObserveInvoicesUseCase @Inject constructor(private val invoices: InvoiceRepository) {
    operator fun invoke(): Flow<List<Invoice>> = invoices.observeInvoices()
}

class RegisterInvoiceUseCase @Inject constructor(private val invoices: InvoiceRepository) {
    suspend operator fun invoke(draft: InvoiceDraft): Invoice = invoices.register(draft)
}

class CancelInvoiceUseCase @Inject constructor(private val invoices: InvoiceRepository) {
    suspend operator fun invoke(invoiceId: String, reason: CancelReason): Invoice = invoices.cancel(invoiceId, reason)
}

/** Resolves receipts against invoices and withholding so every screen shows the same amounts. */
class ObserveReceiptViewsUseCase @Inject constructor(
    private val receipts: ReceiptRepository,
    private val invoices: InvoiceRepository,
    private val withholding: WithholdingRepository,
) {
    operator fun invoke(): Flow<List<ReceiptView>> =
        combine(receipts.observeReceipts(), invoices.observeInvoices(), withholding.observeRecords()) { r, i, w ->
            r.map { resolve(it, i, w) }
        }

    companion object {
        fun resolve(r: Receipt, invoices: List<Invoice>, records: List<WithholdingRecord>): ReceiptView {
            val linked = r.linkedInvoiceId?.let { id -> invoices.find { it.id == id } }
            val buyer = r.buyer ?: linked?.buyer ?: Party(name = "—")
            if (r.items.isNotEmpty()) {
                val totals = TaxCalculator.calculate(r.items)
                return ReceiptView(r, buyer, totals, totals.grandTotal, linked, null, ZeroMoney)
            }
            val record = records.find { it.invoiceId == r.linkedInvoiceId }
            val withheld = if (record != null && linked != null && r.type == ReceiptType.PAYMENT)
                (linked.totals.subtotal * record.rate).cents() else ZeroMoney
            val amount = r.amount ?: linked?.let { it.totals.grandTotal - withheld } ?: ZeroMoney
            return ReceiptView(
                receipt = r, buyer = buyer,
                totals = DocumentTotals(emptyList(), amount, ZeroMoney, emptyList()),
                amount = amount, linkedInvoice = linked,
                withholding = record.takeIf { withheld > BigDecimal.ZERO }, withheldAmount = withheld,
            )
        }
    }
}

class RegisterReceiptUseCase @Inject constructor(private val receipts: ReceiptRepository) {
    suspend operator fun invoke(draft: ReceiptDraft): Receipt = receipts.register(draft)
}

class ObserveWithholdingUseCase @Inject constructor(
    private val withholding: WithholdingRepository,
    private val receiptViews: ObserveReceiptViewsUseCase,
    private val invoices: InvoiceRepository,
) {
    operator fun invoke(): Flow<List<WithholdingView>> =
        combine(withholding.observeRecords(), invoices.observeInvoices(), receiptViews()) { w, inv, rcp ->
            w.mapNotNull { rec ->
                val invoice = inv.find { it.id == rec.invoiceId } ?: return@mapNotNull null
                val payment = rcp.find { it.receipt.linkedInvoiceId == rec.invoiceId && it.receipt.type == ReceiptType.PAYMENT }
                WithholdingView(rec, invoice, payment)
            }
        }
}

class ObserveCatalogUseCase @Inject constructor(private val catalog: CatalogRepository) {
    operator fun invoke(): Flow<List<CatalogItem>> = catalog.observeItems()
}

sealed interface CatalogValidation {
    data object Ok : CatalogValidation
    data object DuplicateCode : CatalogValidation
    data class Missing(val fields: List<String>) : CatalogValidation
}

class ValidateCatalogItemUseCase @Inject constructor() {
    operator fun invoke(item: CatalogItem, isNew: Boolean, existing: List<CatalogItem>): CatalogValidation {
        if (isNew && existing.any { it.code.equals(item.code.trim(), ignoreCase = true) }) return CatalogValidation.DuplicateCode
        val missing = listOfNotNull(
            "code".takeIf { item.code.isBlank() },
            "description".takeIf { item.description.isBlank() },
            "HS code".takeIf { item.nature == NatureOfSupply.GOODS && item.hsCode.isBlank() },
            "price".takeIf { item.unitPrice <= BigDecimal.ZERO },
        )
        return if (missing.isEmpty()) CatalogValidation.Ok else CatalogValidation.Missing(missing)
    }
}

class SaveCatalogItemUseCase @Inject constructor(private val catalog: CatalogRepository) {
    suspend operator fun invoke(item: CatalogItem, isNew: Boolean) = catalog.save(item, isNew)
}

class SetCatalogItemActiveUseCase @Inject constructor(private val catalog: CatalogRepository) {
    suspend operator fun invoke(code: String, active: Boolean) = catalog.setActive(code, active)
}

class MarkCertificateReceivedUseCase @Inject constructor(private val withholding: WithholdingRepository) {
    suspend operator fun invoke(id: String) = withholding.markCertificateReceived(id)
}

class LookupTaxpayerUseCase @Inject constructor(private val taxpayers: TaxpayerRepository) {
    suspend operator fun invoke(tin: String): Party? = if (tin.length == 10 && tin.all { it.isDigit() }) taxpayers.lookup(tin) else null
}
