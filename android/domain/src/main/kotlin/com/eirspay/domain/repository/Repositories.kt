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

/**
 * Merchant settings: credentials, seller profile, branches, source systems, API keys and account
 * security. Changes are applied immediately and pushed through [settings].
 */
interface SettingsRepository {
    val settings: StateFlow<MerchantSettings>
    suspend fun saveCredentials(credentials: MorCredentials)
    suspend fun saveSeller(profile: SellerProfile)

    /** Adds a branch with default rules and returns it, ready to edit. */
    suspend fun addBranch(): MerchantBranch
    suspend fun saveBranch(branch: MerchantBranch)
    suspend fun deleteBranch(id: String)

    /** Adds an unverified source system under [branchId] and returns it, ready to edit. */
    suspend fun addSourceSystem(branchId: String): SourceSystemProfile
    suspend fun saveSourceSystem(system: SourceSystemProfile)
    suspend fun deleteSourceSystem(id: String)
    /** Sends a probe document for the source system using [hsn] as the test item's HSN code. */
    suspend fun verifySourceSystem(id: String, hsn: String): SourceSystemProfile
    suspend fun setExciseEnabled(enabled: Boolean)

    /** Issues a key. The full secret is only ever returned here. */
    suspend fun createApiKey(draft: ApiKeyDraft): CreatedApiKey
    suspend fun saveApiKey(key: ApiKey)
    /** Deactivates the key; it stays on record. */
    suspend fun revokeApiKey(id: String)

    suspend fun changePassword(current: String, newPassword: String)
    suspend fun signOutDevice(id: String)
    suspend fun signOutOtherDevices()

    /** Sends the signed probe invoice to MoR; true when it passes. */
    suspend fun verifySetup(): Boolean
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
