package com.eirspay.mobile.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/** Lets a screen ask for light (white) status-bar icons, e.g. over the teal splash. Provided by MainActivity. */
fun interface SystemBarController { fun setLightIcons(light: Boolean) }

val LocalSystemBarController = staticCompositionLocalOf { SystemBarController { } }

/** While this is in composition the status bar uses light icons; dark icons are restored after. */
@Composable
fun LightStatusBarIcons() {
    val controller = LocalSystemBarController.current
    DisposableEffect(controller) {
        controller.setLightIcons(true)
        onDispose { controller.setLightIcons(false) }
    }
}

/** Keeps the display awake (terminal mode on a counter tablet). Provided by MainActivity. */
fun interface ScreenAwakeController { fun setKeepScreenOn(on: Boolean) }

val LocalScreenAwakeController = staticCompositionLocalOf { ScreenAwakeController { } }

@Composable
fun KeepScreenOn() {
    val controller = LocalScreenAwakeController.current
    DisposableEffect(controller) {
        controller.setKeepScreenOn(true)
        onDispose { controller.setKeepScreenOn(false) }
    }
}
