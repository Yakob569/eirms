package com.eirspay.domain.model

enum class UserRole { OWNER, ADMIN, CASHIER }

data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val role: UserRole,
) {
    val fullName: String get() = "$firstName $lastName"
    val initials: String get() = "${firstName.take(1)}${lastName.take(1)}".uppercase()
}

data class Merchant(val id: String, val displayName: String, val seller: Party, val environment: String)

data class Session(val user: User, val merchant: Merchant)

enum class VerificationChannel { SMS, EMAIL }

data class PendingAccount(val fullName: String, val maskedPhone: String, val maskedEmail: String)

data class SignUpRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
)

sealed interface SignInResult {
    data class Success(val session: Session) : SignInResult
    /** Account was created with a temporary password; the user must set their own first. */
    data class PasswordChangeRequired(val session: Session) : SignInResult
    data object InvalidCredentials : SignInResult
}

data class BranchInfo(val branch: Branch, val sourceSystems: Int, val isDefault: Boolean)

data class SetupStep(val title: String, val description: String)

val SetupSteps = listOf(
    SetupStep("MoR credentials", "Save your EIRMS client ID, secret, API key, TIN and environment."),
    SetupStep("Seller profile", "Legal name, TIN, VAT number, wereda and region."),
    SetupStep("Source system", "Register at least one POS or storefront system."),
    SetupStep("Verify setup", "Run a signed probe against the MoR endpoint."),
    SetupStep("API keys unlocked", "Issue live keys once every step above is green."),
)
