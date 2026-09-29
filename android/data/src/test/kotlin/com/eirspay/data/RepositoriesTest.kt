package com.eirspay.data

import com.eirspay.data.fake.*
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.InvoiceDraft
import com.eirspay.domain.usecase.ObserveReceiptViewsUseCase
import com.eirspay.domain.usecase.ObserveWithholdingUseCase
import com.eirspay.domain.usecase.PasswordPolicy
import com.eirspay.domain.util.AmountInWords
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class RepositoriesTest {
    @Before fun noLatency() { FakeLatency.millis = 0 }

    @Test fun invoiceTotalsMatchPrototype() = runTest {
        val inv = FakeInvoiceRepository().observeInvoices().first().first { it.number == "104" }
        assertEquals(BigDecimal("23213.00"), inv.totals.subtotal)
        assertEquals(BigDecimal("3481.95"), inv.totals.taxTotal)
        assertEquals(BigDecimal("26694.95"), inv.totals.grandTotal)
    }

    @Test fun cancelMovesInvoiceToCanceled() = runTest {
        val repo = FakeInvoiceRepository()
        val canceled = repo.cancel("INV-FP95G9D7JG", CancelReason.DUPLICATE)
        assertEquals(InvoiceStatus.CANCELED, canceled.status)
        assertEquals(InvoiceStatus.ISSUED, canceled.cancellation?.originalStatus)
    }

    @Test fun registerPrependsInvoice() = runTest {
        val repo = FakeInvoiceRepository()
        val new = repo.register(InvoiceDraft(DocumentType.INV, TransactionType.B2C, Branch.BOLE, Party.WalkIn,
            InvoicePaymentMode.CASH, PaymentTerm.IMMEDIATE, listOf(LineItem("X", "ITM-1", NatureOfSupply.GOODS, "PCS", BigDecimal.ONE, money(100.0), TaxCode.VAT15))))
        assertEquals(new.id, repo.observeInvoices().first().first().id)
        assertEquals(BigDecimal("115.00"), new.totals.grandTotal)
    }

    @Test fun paymentReceiptNetsOutWithholding() = runTest {
        val inv = FakeInvoiceRepository(); val wht = FakeWithholdingRepository()
        val views = ObserveReceiptViewsUseCase(FakeReceiptRepository(), inv, wht)().first()
        val payment = views.first { it.receipt.number == "2289" }
        // INV-100: subtotal 11300, grand 12650; 2% withheld on subtotal = 226
        assertEquals(BigDecimal("226.00"), payment.withheldAmount)
        assertEquals(BigDecimal("12424.00"), payment.amount)
        val w = ObserveWithholdingUseCase(wht, ObserveReceiptViewsUseCase(FakeReceiptRepository(), inv, wht), inv)().first()
        assertEquals(4, w.size)
        assertTrue(w.first { it.record.id == "WHT-2026-004310" }.paymentReceipt != null)
    }

    @Test fun passwordPolicy() {
        assertEquals(0, PasswordPolicy.evaluate("").score)
        assertTrue(PasswordPolicy.evaluate("Abcdefg1").acceptable)
        assertEquals(4, PasswordPolicy.evaluate("Abcdefg1!").score)
    }

    @Test fun amountInWords() {
        assertEquals("Twenty-six thousand six hundred ninety-four birr and ninety-five cents", AmountInWords.birr(BigDecimal("26694.95")))
    }

    @Test fun analyticsSplitByBranch() = runTest {
        val all = FakeAnalyticsRepository().salesAnalytics(DashboardRange.DAYS_7, null)
        val bole = FakeAnalyticsRepository().salesAnalytics(DashboardRange.DAYS_7, Branch.BOLE)
        assertEquals(7, all.points.size)
        assertTrue(bole.totalSales < all.totalSales)
    }
}
