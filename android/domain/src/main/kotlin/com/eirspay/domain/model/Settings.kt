package com.eirspay.domain.model

/**
 * Merchant settings: MoR credentials, seller profile and the Branch → Source system → API key
 * hierarchy. A source system belongs to one branch; an API key is bound to one source system, and
 * everything below a branch is limited to what the branch allows.
 *
 * Chip values (transaction types, operations, tax types…) are the API's string codes, listed in
 * [SettingsCatalog].
 */
object SettingsCatalog {
    val transactionTypes = listOf("B2C", "B2B", "B2G", "G2B")
    val documentTypes = listOf("INV", "CRE", "DEB", "RCT", "WHT")
    val natures = listOf("GOODS", "SERVICE")
    /** Tax types a source system or key can be granted directly. EXCISE is derived (see [EXCISE]). */
    val taxTypes = listOf("VAT0", "VAT15", "VATEX")
    /** Granted business-wide by the merchant's excise status, never per source system. */
    const val EXCISE = "EXCISE"
    val operations = listOf("Single Register", "Bulk Register", "Single Cancel", "Bulk Cancel", "Receipt Issue", "Withholding Issue")
    val permissions = listOf("Issue invoices", "Cancel invoices", "Issue receipts", "Issue withholding", "View reports", "Manage items")
    val systemTypes = listOf("POS", "ERP", "E-COMMERCE", "MANUAL")
}

enum class MorEnvironment { DEV, PROD }

data class MorCredentials(
    val clientId: String,
    val tin: String,
    val clientSecret: String,
    val apiKey: String,
    val environment: MorEnvironment,
)

/** The legal entity printed on every invoice and receipt. */
data class SellerProfile(
    val tin: String,
    val legalName: String,
    val vatNumber: String,
    val email: String,
    val phone: String,
    val region: String,
    val city: String,
    val wereda: String,
    val subCity: String,
    val zone: String,
    val kebele: String,
    val locality: String,
    val houseNumber: String,
    val country: String,
)

data class BranchAddress(
    val city: String = "",
    val subCity: String = "",
    val wereda: String = "",
    val kebele: String = "",
    val houseNumber: String = "",
    val locality: String = "",
    val zone: String = "",
    val region: String = "",
    val country: String = "Ethiopia",
)

/** A branch and its business rules: the ceiling for its source systems, API keys and users. */
data class MerchantBranch(
    val id: String,
    val name: String,
    val code: String,
    val isMain: Boolean,
    val active: Boolean,
    val manager: String?,
    val address: BranchAddress,
    val transactionTypes: List<String>,
    val documentTypes: List<String>,
    val natures: List<String>,
    val operations: List<String>,
    val permissions: List<String>,
)

enum class VerificationStatus(val label: String) { VERIFIED("Verified"), FAILED("Failed"), UNVERIFIED("Unverified") }

/** A till, ERP or storefront registered with MoR under one branch. */
data class SourceSystemProfile(
    val id: String,
    val label: String,
    val type: String,
    val branchId: String,
    val active: Boolean,
    val verification: VerificationStatus,
    val verificationNote: String,
    /** Directly granted tax types; EXCISE is merged in from the merchant (see [SettingsRules.taxTypes]). */
    val taxTypes: List<String>,
    val cashier: String,
    val salesPerson: String,
    val systemId: String,
    val lastDocument: String,
)

/** An API key bound to one source system. Only the last four characters are kept after creation. */
data class ApiKey(
    val id: String,
    val label: String,
    val sourceSystemId: String,
    val active: Boolean,
    val created: String,
    val operations: List<String>,
    val transactionTypes: List<String>,
    val taxTypes: List<String>,
    val tail: String,
    val users: List<String>,
) {
    val masked: String get() = "eirs_live_••••••••$tail"
}

data class ApiKeyDraft(
    val label: String,
    val sourceSystemId: String,
    val operations: List<String>,
    val transactionTypes: List<String>,
    val taxTypes: List<String>,
    val users: List<String>,
)

/** Returned once, on creation. [secret] is never stored; show it and let the user copy it. */
data class CreatedApiKey(val key: ApiKey, val secret: String)

data class SignedInDevice(
    val id: String,
    val device: String,
    val fingerprint: String,
    val location: String,
    val lastActive: String,
    val current: Boolean,
)

enum class SecurityEventKind { SIGN_IN, FAILED, SIGN_OUT, PASSWORD_RESET }

data class SecurityEvent(val kind: SecurityEventKind, val title: String, val detail: String, val whenText: String)

/** Everything the settings pages show. */
data class MerchantSettings(
    val credentials: MorCredentials,
    val seller: SellerProfile,
    val staff: List<String>,
    val branches: List<MerchantBranch>,
    val sourceSystems: List<SourceSystemProfile>,
    val apiKeys: List<ApiKey>,
    /** Merchant-wide excise status: when on, EXCISE joins every source system's tax types. */
    val exciseEnabled: Boolean,
    val devices: List<SignedInDevice>,
    val securityEvents: List<SecurityEvent>,
    val passwordNote: String,
    /** The signed MoR probe passed (Verify setup). */
    val setupVerified: Boolean,
) {
    fun branch(id: String): MerchantBranch? = branches.find { it.id == id }
    fun sourceSystem(id: String): SourceSystemProfile? = sourceSystems.find { it.id == id }
    fun apiKey(id: String): ApiKey? = apiKeys.find { it.id == id }
    fun sourceSystemsOf(branchId: String): List<SourceSystemProfile> = sourceSystems.filter { it.branchId == branchId }
    fun keysOf(sourceSystemId: String): List<ApiKey> = apiKeys.filter { it.sourceSystemId == sourceSystemId }
}
