package com.eirspay.mobile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.eirspay.mobile.navigation.AppNavHost
import com.eirspay.mobile.ui.designsystem.LocalScreenAwakeController
import com.eirspay.mobile.ui.designsystem.LocalSystemBarController
import com.eirspay.mobile.ui.designsystem.ScreenAwakeController
import com.eirspay.mobile.ui.designsystem.SystemBarController
import com.eirspay.mobile.ui.theme.EirsPayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val resetToken = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        resetToken.value = intent.resetToken()

        val insets = WindowCompat.getInsetsController(window, window.decorView)
        val systemBars = SystemBarController { light -> insets.isAppearanceLightStatusBars = !light }
        val screenAwake = ScreenAwakeController { on ->
            if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setContent {
            EirsPayTheme {
                CompositionLocalProvider(
                    LocalSystemBarController provides systemBars,
                    LocalScreenAwakeController provides screenAwake,
                ) {
                    AppNavHost(resetToken = resetToken.value, showPrototypeShortcuts = BuildConfig.DEBUG)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.resetToken()?.let { resetToken.value = it }
    }

    private fun Intent?.resetToken(): String? =
        this?.data?.takeIf { it.scheme == "eirspay" && it.host == "reset" }?.getQueryParameter("token")
}
