package com.eirspay.data

import com.eirspay.data.fake.*
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.InvoiceDraft
import com.eirspay.domain.usecase.AdjustmentLine
import com.eirspay.domain.usecase.AdjustmentRules
import com.eirspay.domain.usecase.AdjustmentValidation
import com.eirspay.domain.usecase.CreateApiKeyUseCase
import com.eirspay.domain.usecase.InvoiceRowAction
import com.eirspay.domain.usecase.ObserveReceiptViewsUseCase
import com.eirspay.domain.usecase.ReceiptRowAction
import com.eirspay.domain.usecase.RegisterAdjustmentNoteUseCase
import com.eirspay.domain.usecase.RegisterRetainerInvoiceUseCase
import com.eirspay.domain.usecase.RowActionPolicy
import com.eirspay.domain.usecase.SettingsRules
import com.eirspay.domain.usecase.ObserveWithholdingUseCase
import com.eirspay.domain.usecase.PasswordPolicy
import com.eirspay.domain.util.AmountInWords
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    // --- Row actions: credit / debit notes, interim / final invoices --------------------------

    @Test fun rowActionsFollowDocumentType() = runTest {
        val all = FakeInvoiceRepository().observeInvoices().first()
        fun actions(id: String) = RowActionPolicy.invoiceActions(all.first { it.id == id }, all)
        // INV-100 already has CRE-098, so only the debit note is offered.
        assertEquals(
            listOf(InvoiceRowAction.VIEW_DETAILS, InvoiceRowAction.MAKE_DEBIT_NOTE, InvoiceRowAction.CANCEL),
            actions("INV-B3N5M7Q9WE"),
        )
        assertEquals(listOf(InvoiceRowAction.VIEW_DETAILS), actions("CRE-Q1W2E3R4TY"))
        assertEquals(listOf(InvoiceRowAction.VIEW_DETAILS), actions("INV-K2L8M3N4PQ")) // canceled
        assertEquals(
            listOf(InvoiceRowAction.VIEW_DETAILS, InvoiceRowAction.MAKE_INTERIM, InvoiceRowAction.MAKE_FINAL),
            actions("RTN-H7G6F5D4SA"),
        )
    }

    @Test fun receiptLinkedInvoiceActionOnlyWhenLinked() = runTest {
        val inv = FakeInvoiceRepository(); val wht = FakeWithholdingRepository()
        val views = ObserveReceiptViewsUseCase(FakeReceiptRepository(), inv, wht)().first()
        assertEquals(listOf(ReceiptRowAction.VIEW_DETAILS, ReceiptRowAction.VIEW_LINKED_INVOICE),
            RowActionPolicy.receiptActions(views.first { it.receipt.number == "2289" }))
        assertEquals(listOf(ReceiptRowAction.VIEW_DETAILS), RowActionPolicy.receiptActions(views.first { it.receipt.number == "2291" }))
    }

    @Test fun debitNoteCopiesOriginalAndHidesItselfAfterwards() = runTest {
        val repo = FakeInvoiceRepository()
        val note = RegisterAdjustmentNoteUseCase(repo)(
            DocumentType.DEB, "INV-B3N5M7Q9WE", listOf(AdjustmentLine(1, BigDecimal.ONE, BigDecimal("500"))), "Extra delivery leg",
        )
        assertEquals(DocumentType.DEB, note.documentType)
        assertEquals("Dashen Trading PLC", note.buyer.name)
        assertEquals(Branch.BOLE, note.branch)
        assertEquals("INV-B3N5M7Q9WE", note.amendment?.previousInvoiceId)
        assertEquals(listOf("SRV-0009"), note.items.map { it.itemCode })
        assertEquals(BigDecimal("500.00"), note.totals.grandTotal) // VAT0 line
        val all = repo.observeInvoices().first()
        assertFalse(InvoiceRowAction.MAKE_DEBIT_NOTE in RowActionPolicy.invoiceActions(all.first { it.id == "INV-B3N5M7Q9WE" }, all))
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                RegisterAdjustmentNoteUseCase(repo)(DocumentType.DEB, "INV-B3N5M7Q9WE", listOf(AdjustmentLine(0, BigDecimal.ONE, BigDecimal.TEN)), "Again")
            }
        }
    }

    @Test fun creditNoteCannotExceedOriginalLine() = runTest {
        val original = FakeInvoiceRepository().observeInvoices().first().first { it.id == "INV-R4T6Y8U1IO" }
        // Line 1: 10 BOX × 1200
        assertEquals(AdjustmentValidation.ExceedsOriginal(setOf(1)),
            AdjustmentRules.validate(DocumentType.CRE, original, listOf(AdjustmentLine(1, BigDecimal(11), BigDecimal(1200))), "Returned"))
        assertEquals(AdjustmentValidation.Ok,
            AdjustmentRules.validate(DocumentType.DEB, original, listOf(AdjustmentLine(1, BigDecimal(11), BigDecimal(1200))), "Extra box"))
        assertEquals(AdjustmentValidation.MissingReason,
            AdjustmentRules.validate(DocumentType.CRE, original, listOf(AdjustmentLine(1, BigDecimal(2), BigDecimal(1200))), " "))
        assertEquals(AdjustmentValidation.NoLines, AdjustmentRules.validate(DocumentType.CRE, original, emptyList(), "Returned"))
    }

    @Test fun interimInvoiceRegistersAgainstRetainer() = runTest {
        val repo = FakeInvoiceRepository()
        val buyer = Party("Abyssinia Consulting PLC (HQ)", tin = "0072214598")
        val items = listOf(LineItem("Q4 support, October", "SRV-0200", NatureOfSupply.SERVICE, "JOB", BigDecimal.ONE, money(15000.0), TaxCode.VAT15))
        val int = RegisterRetainerInvoiceUseCase(repo)(DocumentType.INT, "RTN-H7G6F5D4SA", buyer, items, InvoicePaymentMode.BANK_TRANSFER, PaymentTerm.NET_15)
        assertEquals(DocumentType.INT, int.documentType)
        assertEquals(TransactionType.B2B, int.transactionType)
        assertEquals("Abyssinia Consulting PLC (HQ)", int.buyer.name) // buyer is editable, unlike credit/debit
        assertEquals("RTN-H7G6F5D4SA", int.amendment?.previousInvoiceId)
        assertEquals(BigDecimal("17250.00"), int.totals.grandTotal)
        assertEquals(int.id, repo.observeInvoices().first().first().id)
    }

    // --- Settings ------------------------------------------------------------------------------

    @Test fun exciseIsDerivedForEverySourceSystem() = runTest {
        val repo = FakeSettingsRepository()
        val s1 = repo.settings.value.sourceSystem("s1")!!
        assertFalse(SettingsCatalog.EXCISE in SettingsRules.taxTypes(s1, repo.settings.value.exciseEnabled))
        repo.setExciseEnabled(true)
        val s = repo.settings.value
        assertTrue(s.sourceSystems.all { SettingsCatalog.EXCISE in SettingsRules.taxTypes(it, s.exciseEnabled) })
        assertTrue(SettingsCatalog.EXCISE in SettingsRules.eligibleTaxTypes("b1", s))
        // Saving a source system never stores EXCISE on it.
        repo.saveSourceSystem(s1.copy(taxTypes = s1.taxTypes + SettingsCatalog.EXCISE))
        assertFalse(SettingsCatalog.EXCISE in repo.settings.value.sourceSystem("s1")!!.taxTypes)
    }

    @Test fun apiKeyIsClampedRevealedOnceAndRevokedNotRemoved() = runTest {
        val repo = FakeSettingsRepository()
        val draft = ApiKeyDraft("Till 3", "s3", operations = listOf("Single Register", "Bulk Register"),
            transactionTypes = listOf("B2C", "B2G"), taxTypes = listOf("VAT15", "VAT0"), users = emptyList())
        val created = CreateApiKeyUseCase(repo)(draft, repo.settings.value)
        // Piassa allows no bulk register or B2G; Piassa Till 2 only has VAT15.
        assertEquals(listOf("Single Register"), created.key.operations)
        assertEquals(listOf("B2C"), created.key.transactionTypes)
        assertEquals(listOf("VAT15"), created.key.taxTypes)
        assertTrue(created.secret.startsWith("eirs_live_"))
        assertEquals(created.secret.takeLast(4), created.key.tail)
        assertFalse(created.key.masked.contains(created.secret))
        repo.revokeApiKey(created.key.id)
        val revoked = repo.settings.value.apiKey(created.key.id)
        assertNotNull(revoked)
        assertFalse(revoked!!.active)
        // Inactive source systems can't take new keys.
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { CreateApiKeyUseCase(repo)(draft.copy(sourceSystemId = "s4"), repo.settings.value) }
        }
    }

    @Test fun deleteGuardsAndVerification() = runTest {
        val repo = FakeSettingsRepository()
        val s = repo.settings.value
        assertEquals("The main branch can\u2019t be deleted.", SettingsRules.branchDeleteBlocker(s.branch("b1")!!, s))
        assertEquals("Move or delete its 1 source system first.", SettingsRules.branchDeleteBlocker(s.branch("b2")!!, s))
        assertEquals("Remove its 2 API keys first.", SettingsRules.sourceSystemDeleteBlocker(s.sourceSystem("s2")!!, s))
        val added = repo.addBranch()
        assertNull(SettingsRules.branchDeleteBlocker(added, repo.settings.value))
        repo.deleteBranch(added.id)
        assertNull(repo.settings.value.branch(added.id))
        val verified = repo.verifySourceSystem("s3", "3004")
        assertEquals(VerificationStatus.VERIFIED, verified.verification)
        assertTrue(verified.verificationNote.contains("3004"))
        assertTrue(repo.verifySetup())
        assertTrue(repo.settings.value.setupVerified)
    }
}
