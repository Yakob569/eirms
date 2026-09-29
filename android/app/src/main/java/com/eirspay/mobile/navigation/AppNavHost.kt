package com.eirspay.mobile.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.eirspay.domain.model.Branch
import com.eirspay.mobile.feature.auth.*
import com.eirspay.mobile.feature.invoices.InvoiceDetailScreen
import com.eirspay.mobile.feature.invoices.InvoicePrintScreen
import com.eirspay.mobile.feature.invoices.InvoicesTabContent
import com.eirspay.mobile.feature.invoices.RegisterInvoiceScreen
import com.eirspay.mobile.feature.items.ItemFormScreen
import com.eirspay.mobile.feature.items.ItemsScreen
import com.eirspay.mobile.feature.main.MainNavigator
import com.eirspay.mobile.feature.main.MainScreen
import com.eirspay.mobile.feature.onboarding.OnboardingScreen
import com.eirspay.mobile.feature.receipts.ReceiptDetailScreen
import com.eirspay.mobile.feature.receipts.ReceiptsTabContent
import com.eirspay.mobile.feature.receipts.RegisterReceiptScreen
import com.eirspay.mobile.feature.splash.SplashScreen
import com.eirspay.mobile.feature.terminal.TerminalScreen
import com.eirspay.mobile.feature.withholding.WithholdingDetailScreen
import com.eirspay.mobile.feature.withholding.WithholdingScreen
import com.eirspay.mobile.ui.designsystem.KeepScreenOn

/** Navigates to [route] and clears the whole back stack (sign in / sign out boundaries). */
private fun NavHostController.navigateClearing(route: Any) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/**
 * The app's navigation graph. Feature screens only know callbacks; this is the one place that maps
 * them to destinations.
 *
 * @param resetToken token from an `eirspay://reset?token=…` link; opens the reset-password screen.
 * @param showPrototypeShortcuts debug-only shortcuts (e.g. "Open reset link") from the prototype.
 */
@Composable
fun AppNavHost(resetToken: String?, showPrototypeShortcuts: Boolean) {
    val nav = rememberNavController()
    val app: AppViewModel = hiltViewModel()

    LaunchedEffect(resetToken) {
        if (resetToken != null) nav.navigate(ResetPasswordRoute(resetToken)) { launchSingleTop = true }
    }

    val mainNavigator = object : MainNavigator {
        override fun openInvoice(id: String) = nav.navigate(InvoiceDetailRoute(id))
        override fun openReceipt(id: String) = nav.navigate(ReceiptDetailRoute(id))
        override fun registerInvoice() = nav.navigate(RegisterInvoiceRoute)
        override fun registerReceipt() = nav.navigate(RegisterReceiptRoute)
        override fun openItems() = nav.navigate(ItemsRoute)
        override fun openWithholding() = nav.navigate(WithholdingRoute)
        override fun startTerminal(branch: Branch) = nav.navigate(TerminalRoute(branch.name))
        override fun signedOut() = nav.navigateClearing(SignInRoute)
    }
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(
        navController = nav,
        startDestination = SplashRoute,
        enterTransition = { fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 60 } },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        composable<SplashRoute> {
            SplashScreen(onFinished = { nav.navigateClearing(app.destinationAfterSplash()) })
        }
        composable<OnboardingRoute> {
            OnboardingScreen(onCreateAccount = { nav.navigate(SignUpRoute) }, onSignIn = { nav.navigate(SignInRoute) })
        }

        // --- Auth -----------------------------------------------------------------------------
        composable<SignInRoute> {
            SignInScreen(
                onBack = back,
                onForgotPassword = { nav.navigate(ForgotPasswordRoute) },
                onCreateAccount = { nav.navigate(SignUpRoute) },
                onSignedIn = { mustChange -> if (mustChange) nav.navigate(ChangePasswordRoute) else nav.navigateClearing(MainRoute) },
            )
        }
        composable<SignUpRoute> {
            SignUpScreen(onBack = back, onSignIn = { nav.navigate(SignInRoute) }, onAccountCreated = { nav.navigate(VerifyMethodRoute) })
        }
        composable<VerifyMethodRoute> {
            VerifyMethodScreen(onBack = back, onCodeSent = { channel, sentTo -> nav.navigate(VerifyCodeRoute(channel, sentTo)) })
        }
        composable<VerifyCodeRoute> { entry ->
            val r = entry.toRoute<VerifyCodeRoute>()
            VerifyCodeScreen(
                channel = r.channel, sentTo = r.sentTo, onBack = back, onUseDifferentMethod = back,
                onVerified = { nav.navigate(VerifySuccessRoute(r.sentTo)) },
            )
        }
        composable<VerifySuccessRoute> { entry ->
            VerifySuccessScreen(sentTo = entry.toRoute<VerifySuccessRoute>().sentTo, onBack = back, onContinue = { nav.navigateClearing(MainRoute) })
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(onBack = back, onSignIn = back, onLinkSent = { nav.navigate(ResetLinkSentRoute(it)) })
        }
        composable<ResetLinkSentRoute> { entry ->
            ResetLinkSentScreen(
                email = entry.toRoute<ResetLinkSentRoute>().email, onBack = back, onUseAnotherEmail = back,
                onOpenResetLink = if (showPrototypeShortcuts) ({ nav.navigate(ResetPasswordRoute("debug-reset-token")) }) else null,
            )
        }
        composable<ResetPasswordRoute> { entry ->
            ResetPasswordScreen(token = entry.toRoute<ResetPasswordRoute>().token, onBack = back, onPasswordReset = { nav.navigate(ResetDoneRoute) })
        }
        composable<ResetDoneRoute> {
            ResetDoneScreen(onBack = { nav.navigateClearing(SignInRoute) }, onContinueToSignIn = { nav.navigateClearing(SignInRoute) })
        }
        composable<ChangePasswordRoute> {
            ChangePasswordScreen(onBack = back, onSignOut = { nav.navigateClearing(SignInRoute) }, onPasswordChanged = { nav.navigate(ChangeDoneRoute) })
        }
        composable<ChangeDoneRoute> {
            ChangeDoneScreen(onBack = back, onContinue = { nav.navigateClearing(MainRoute) })
        }

        // --- Signed-in app --------------------------------------------------------------------
        composable<MainRoute> {
            MainScreen(
                navigator = mainNavigator,
                invoicesTab = { request -> InvoicesTabContent(showCanceledRequest = request, onOpenInvoice = mainNavigator::openInvoice) },
                receiptsTab = {
                    ReceiptsTabContent(
                        onOpenReceipt = mainNavigator::openReceipt,
                        onOpenInvoice = mainNavigator::openInvoice,
                        onNewReceipt = mainNavigator::registerReceipt,
                    )
                },
            )
        }
        composable<InvoiceDetailRoute> { entry ->
            InvoiceDetailScreen(id = entry.toRoute<InvoiceDetailRoute>().id, onBack = back, onViewReceipt = { nav.navigate(InvoicePrintRoute(it)) })
        }
        composable<InvoicePrintRoute> { entry ->
            InvoicePrintScreen(id = entry.toRoute<InvoicePrintRoute>().id, onBack = back)
        }
        composable<RegisterInvoiceRoute> {
            RegisterInvoiceScreen(onBack = back, onRegistered = { id ->
                nav.navigate(InvoiceDetailRoute(id)) { popUpTo<RegisterInvoiceRoute> { inclusive = true } }
            })
        }
        composable<ReceiptDetailRoute> { entry ->
            ReceiptDetailScreen(
                id = entry.toRoute<ReceiptDetailRoute>().id, onBack = back,
                onOpenInvoice = { nav.navigate(InvoiceDetailRoute(it)) },
                onOpenWithholding = { nav.navigate(WithholdingDetailRoute(it)) },
            )
        }
        composable<RegisterReceiptRoute> {
            RegisterReceiptScreen(onBack = back, onRegistered = { id ->
                nav.navigate(ReceiptDetailRoute(id)) { popUpTo<RegisterReceiptRoute> { inclusive = true } }
            })
        }
        composable<ItemsRoute> {
            ItemsScreen(onBack = back, onNewItem = { nav.navigate(ItemFormRoute(null)) }, onEditItem = { nav.navigate(ItemFormRoute(it)) })
        }
        composable<ItemFormRoute> { entry ->
            ItemFormScreen(code = entry.toRoute<ItemFormRoute>().code, onBack = back)
        }
        composable<WithholdingRoute> {
            WithholdingScreen(
                onBack = back,
                onOpenRecord = { nav.navigate(WithholdingDetailRoute(it)) },
                onOpenInvoice = { nav.navigate(InvoiceDetailRoute(it)) },
            )
        }
        composable<WithholdingDetailRoute> { entry ->
            WithholdingDetailScreen(
                id = entry.toRoute<WithholdingDetailRoute>().id, onBack = back,
                onOpenInvoice = { nav.navigate(InvoiceDetailRoute(it)) },
                onOpenReceipt = { nav.navigate(ReceiptDetailRoute(it)) },
            )
        }
        composable<TerminalRoute> { entry ->
            KeepScreenOn()
            TerminalScreen(branch = Branch.valueOf(entry.toRoute<TerminalRoute>().branch), onExit = back)
        }
    }
}
