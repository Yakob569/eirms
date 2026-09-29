package com.eirspay.domain.repository

import com.eirspay.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

interface AuthRepository {
    val session: StateFlow<Session?>
    suspend fun signIn(identifier: String, password: String): SignInResult
    suspend fun signUp(request: SignUpRequest): PendingAccount
    fun pendingAccount(): PendingAccount?
    /** Sends a one-time code and returns the masked destination it went to. */
    suspend fun sendVerificationCode(channel: VerificationChannel): String
    suspend fun verifyCode(code: String): Boolean
    suspend fun requestPasswordReset(email: String)
    /** Validates an emailed reset token; returns the account email it belongs to. */
    suspend fun validateResetToken(token: String): String
    suspend fun resetPassword(token: String, newPassword: String)
    suspend fun changeTemporaryPassword(current: String, newPassword: String)
    suspend fun signOut()
}

interface MerchantRepository {
    val branches: List<BranchInfo>
    /** Completed fiscal-readiness steps, 0..5. */
    val setupStepsDone: Flow<Int>
    fun seller(): Party
}

interface InvoiceRepository {
    fun observeInvoices(): Flow<List<Invoice>>
    suspend fun register(draft: InvoiceDraft): Invoice
    suspend fun cancel(invoiceId: String, reason: CancelReason): Invoice
}

interface ReceiptRepository {
    fun observeReceipts(): Flow<List<Receipt>>
    suspend fun register(draft: ReceiptDraft): Receipt
}

interface CatalogRepository {
    fun observeItems(): Flow<List<CatalogItem>>
    suspend fun save(item: CatalogItem, isNew: Boolean)
    suspend fun setActive(code: String, active: Boolean)
}

interface WithholdingRepository {
    fun observeRecords(): Flow<List<WithholdingRecord>>
    suspend fun markCertificateReceived(id: String)
}

interface AnalyticsRepository {
    suspend fun salesAnalytics(range: DashboardRange, branch: Branch?): SalesAnalytics
}

interface TaxpayerRepository {
    /** Looks a TIN up in the MoR taxpayer registry. */
    suspend fun lookup(tin: String): Party?
}

/** UI-wide selections that outlive a single screen (branch filter, onboarding flag). */
interface PreferencesRepository {
    val selectedBranch: StateFlow<Branch?>
    fun selectBranch(branch: Branch?)
    val onboardingCompleted: StateFlow<Boolean>
    fun completeOnboarding()
}

data class InvoiceDraft(
    val documentType: DocumentType,
    val transactionType: TransactionType,
    val branch: Branch,
    val buyer: Party,
    val paymentMode: InvoicePaymentMode,
    val paymentTerm: PaymentTerm,
    val items: List<LineItem>,
    val amendment: Amendment? = null,
)

data class ReceiptDraft(
    val type: ReceiptType,
    val method: PaymentMethod,
    val branch: Branch,
    val buyer: Party?,
    val items: List<LineItem>,
    val reference: String?,
    val provider: String?,
    val tendered: BigDecimal?,
    val linkedInvoiceId: String?,
    val amount: BigDecimal?,
)
