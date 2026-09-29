package com.eirspay.mobile.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation destinations. Arguments are primitives/strings; screens receive them as
// plain parameters so feature code stays independent of navigation.

@Serializable data object SplashRoute
@Serializable data object OnboardingRoute

@Serializable data object SignInRoute
@Serializable data object SignUpRoute
@Serializable data object VerifyMethodRoute
@Serializable data class VerifyCodeRoute(val channel: String, val sentTo: String)
@Serializable data class VerifySuccessRoute(val sentTo: String)
@Serializable data object ForgotPasswordRoute
@Serializable data class ResetLinkSentRoute(val email: String)
@Serializable data class ResetPasswordRoute(val token: String)
@Serializable data object ResetDoneRoute
@Serializable data object ChangePasswordRoute
@Serializable data object ChangeDoneRoute

/** Signed-in shell with the bottom tabs. */
@Serializable data object MainRoute

@Serializable data class InvoiceDetailRoute(val id: String)
@Serializable data class InvoicePrintRoute(val id: String)
@Serializable data object RegisterInvoiceRoute
@Serializable data class ReceiptDetailRoute(val id: String)
@Serializable data object RegisterReceiptRoute
@Serializable data object ItemsRoute
@Serializable data class ItemFormRoute(val code: String? = null)
@Serializable data object WithholdingRoute
@Serializable data class WithholdingDetailRoute(val id: String)
/** [branch] is a [com.eirspay.domain.model.Branch] name. */
@Serializable data class TerminalRoute(val branch: String)
