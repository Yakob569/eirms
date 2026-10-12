package com.eirspay.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class LineItem(
    val description: String,
    val itemCode: String,
    val nature: NatureOfSupply,
    val unit: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val taxCode: TaxCode,
)

data class LineTotals(
    val number: Int,
    val item: LineItem,
    val preTax: BigDecimal,
    val tax: BigDecimal,
) {
    val total: BigDecimal get() = preTax + tax
}

data class TaxLine(val code: TaxCode, val taxable: BigDecimal, val tax: BigDecimal)

data class DocumentTotals(
    val lines: List<LineTotals>,
    val subtotal: BigDecimal,
    val taxTotal: BigDecimal,
    val taxes: List<TaxLine>,
) {
    val grandTotal: BigDecimal get() = subtotal + taxTotal

    companion object {
        val Empty = DocumentTotals(emptyList(), ZeroMoney, ZeroMoney, emptyList())
    }
}

data class Cancellation(
    val reason: CancelReason,
    val canceledAt: LocalDateTime,
    val originalStatus: InvoiceStatus,
)

/** Credit/debit notes amend an earlier registered invoice. */
data class Amendment(val reason: String, val previousInvoiceId: String)

data class SourceSystem(val type: String, val number: String)

/**
 * An invoice registered with the Ministry of Revenues. [irn] is the fiscal signature MoR returns;
 * it stays on record even when the invoice is later canceled.
 */
data class Invoice(
    val id: String,
    val number: String,
    val documentType: DocumentType,
    val transactionType: TransactionType,
    val status: InvoiceStatus,
    val issuedAt: LocalDateTime,
    val branch: Branch,
    val sourceSystem: SourceSystem,
    val seller: Party,
    val buyer: Party,
    val paymentMode: InvoicePaymentMode,
    val paymentTerm: PaymentTerm,
    val items: List<LineItem>,
    val irn: String,
    val amendment: Amendment? = null,
    val cancellation: Cancellation? = null,
) {
    val totals: DocumentTotals = TaxCalculator.calculate(items)
    val isCanceled: Boolean get() = status == InvoiceStatus.CANCELED
    /** Sequential counter MoR tracks per source system. */
    val counter: String get() = ((number.toIntOrNull() ?: 0) + 1000).toString()
}

enum class ReceiptType(val displayName: String, val description: String) {
    SALES("Sales", "Paid at the counter for goods or services"),
    PAYMENT("Payment", "Settles an invoice issued on credit"),
    ADVANCE("Advance", "Deposit before goods or services are delivered"),
    REFUND("Refund", "Money returned against an earlier invoice");

    /** Sales and advance receipts carry their own line items; payment and refund point at an invoice. */
    val carriesItems: Boolean get() = this == SALES || this == ADVANCE
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CHEQUE("Cheque"),
    BANK_TRANSFER("Bank transfer"),
    MOBILE_MONEY("Mobile money"),
    CARD("Card");

    val requiresReference: Boolean get() = this == CHEQUE || this == BANK_TRANSFER || this == MOBILE_MONEY
}

val MobileMoneyProviders = listOf("telebirr", "M-Pesa", "CBE Birr", "HelloCash")

data class Receipt(
    val id: String,
    val number: String,
    val type: ReceiptType,
    val method: PaymentMethod,
    val issuedAt: LocalDateTime,
    val branch: Branch,
    val sourceSystem: SourceSystem,
    val irn: String,
    val buyer: Party? = null,
    val items: List<LineItem> = emptyList(),
    val reference: String? = null,
    val provider: String? = null,
    val tendered: BigDecimal? = null,
    val linkedInvoiceId: String? = null,
    /** Explicit amount for receipts without items (payments, refunds). */
    val amount: BigDecimal? = null,
)

data class WithholdingRecord(
    val id: String,
    val invoiceId: String,
    val rate: BigDecimal,
    val withheldOn: java.time.LocalDate,
    val taxPeriod: String,
    val certificateReceived: Boolean,
)

data class CatalogItem(
    val code: String,
    val description: String,
    val nature: NatureOfSupply,
    val hsCode: String,
    val taxCode: TaxCode,
    val unit: String,
    val unitPrice: BigDecimal,
    val active: Boolean,
    val category: String,
) {
    val grossPrice: BigDecimal get() = (unitPrice * BigDecimal.valueOf(taxCode.rate) + unitPrice).cents()

    fun toLineItem(quantity: BigDecimal = BigDecimal.ONE) = LineItem(
        description = description,
        itemCode = code,
        nature = nature,
        unit = unit,
        quantity = quantity,
        unitPrice = unitPrice,
        taxCode = taxCode,
    )
}
