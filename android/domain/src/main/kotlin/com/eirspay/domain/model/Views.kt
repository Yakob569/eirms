package com.eirspay.domain.model

import java.math.BigDecimal

/** A receipt with everything resolved that the UI needs: buyer, linked invoice, withholding and amount. */
data class ReceiptView(
    val receipt: Receipt,
    val buyer: Party,
    val totals: DocumentTotals,
    val amount: BigDecimal,
    val linkedInvoice: Invoice?,
    val withholding: WithholdingRecord?,
    val withheldAmount: BigDecimal,
) {
    val change: BigDecimal
        get() = receipt.tendered?.let { (it - amount).max(ZeroMoney) } ?: ZeroMoney
}

data class WithholdingView(
    val record: WithholdingRecord,
    val invoice: Invoice,
    val paymentReceipt: ReceiptView?,
) {
    val base: BigDecimal get() = invoice.totals.subtotal
    val amount: BigDecimal get() = (base * record.rate).cents()
    val netPaid: BigDecimal get() = invoice.totals.grandTotal - amount
}
