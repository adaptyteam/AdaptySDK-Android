package com.adapty.ui.internal.platform

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.LayoutDirection
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.AdaptyUIWeb

// Browser environment: one host context, no system bars, no back dispatcher yet.

internal actual class PlatformContextHandle internal constructor() {
    actual val context: Context get() = WebContext

    actual fun activityOrNull(): Activity? = null

    override fun equals(other: Any?): Boolean = other is PlatformContextHandle

    override fun hashCode(): Int = 0
}

internal actual fun PlatformContextHandle.isChangingConfigurations(): Boolean = false

@Composable
internal actual fun rememberPlatformContext(): PlatformContextHandle = remember { PlatformContextHandle() }

private fun windowInnerHeight(): Int = js("window.innerHeight")

private fun devicePixelRatio(): Double = js("window.devicePixelRatio || 1")

internal actual fun screenSmallestWidthDp(): Int = AdaptyUIWeb.environment.smallestScreenWidthDp

internal actual fun osVersion(): String {
    webStub("osVersion")
    return ""
}

internal actual fun systemDensity(): Float = devicePixelRatio().toFloat()

internal actual fun parseUri(value: String): Uri = WebUri(value)

@Composable
internal actual fun areAnimationsDisabled(): Boolean = false

@Composable
internal actual fun isLandscape(): Boolean = AdaptyUIWeb.environment.isLandscape

internal actual fun progressIndicatorColor(ctx: PlatformContextHandle): Int? = null

internal actual fun setSoftInputMode(ctx: PlatformContextHandle): (() -> Unit)? = null

internal actual class PlatformRootInsets(actual val isNone: Boolean)

/**
 * Web has no window insets, so [AdaptyUIWeb.environment] insets are delivered through the Android
 * "insets not delivered" fallback: non-empty insets report non-None root insets and come back from
 * [fallbackInsets]; empty insets skip the fallback.
 */
internal actual class PlatformSafeInsets {
    actual val screenHeightDp: Int get() = windowInnerHeight()

    actual val configurationKey: Any get() = AdaptyUIWeb.environment

    actual fun rootInsetsOrNull(): PlatformRootInsets? {
        val insets = AdaptyUIWeb.environment.safeInsets
        return if (insets == AdaptyFlowInsets.None || insets == AdaptyFlowInsets.Unspecified) null else PlatformRootInsets(false)
    }

    actual fun requestApplyInsets() {}

    actual fun fallbackInsets(layoutDirection: LayoutDirection): AdaptyFlowInsets = AdaptyUIWeb.environment.safeInsets
}

@Composable
internal actual fun rememberSafeInsets(): PlatformSafeInsets = remember { PlatformSafeInsets() }

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, key: Any?, onBack: PlatformBackScope.() -> Unit) {
    webStub("PlatformBackHandler")
}

@Composable
internal actual fun OnLifecycleResumePause(
    key: Any?,
    onResume: () -> Unit,
    onPause: () -> Unit,
) {
    val hasAppeared = rememberSaveable(key) { mutableStateOf(false) }
    DisposableEffect(key) {
        if (!hasAppeared.value) {
            hasAppeared.value = true
            onResume()
        }
        onDispose { onPause() }
    }
}

internal actual fun noOverscroll(): ProvidedValue<*> = LocalOverscrollFactory provides null

@Composable
internal actual fun isImeVisible(): Boolean = false
