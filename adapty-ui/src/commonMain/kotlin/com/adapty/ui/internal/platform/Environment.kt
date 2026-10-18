@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.unit.LayoutDirection
import com.adapty.ui.AdaptyFlowInsets

// Environment seams: host context, screen, insets, back handling, lifecycle.

/** Opaque platform context; Android wraps [Context]. Equality follows the wrapped context. */
internal expect class PlatformContextHandle {
    val context: Context

    fun activityOrNull(): Activity?
}

/** True when the host activity is being recreated for a configuration change. */
internal expect fun PlatformContextHandle.isChangingConfigurations(): Boolean

@Composable
internal expect fun rememberPlatformContext(): PlatformContextHandle

internal expect fun screenSmallestWidthDp(): Int

internal expect fun osVersion(): String

internal expect fun systemDensity(): Float

internal expect fun parseUri(value: String): Uri

@Composable
internal expect fun areAnimationsDisabled(): Boolean

@Composable
internal expect fun isLandscape(): Boolean

internal expect fun progressIndicatorColor(ctx: PlatformContextHandle): Int?

/** Forces adjust-resize soft input mode on the host window; returns a restore callback. */
internal expect fun setSoftInputMode(ctx: PlatformContextHandle): (() -> Unit)?

/** Root window bar/cutout insets as reported by the platform view hierarchy. */
internal expect class PlatformRootInsets {
    val isNone: Boolean
}

/** Safe-area probe used before Compose window insets are delivered. */
internal expect class PlatformSafeInsets {
    val screenHeightDp: Int

    /** Changes whenever the platform configuration changes (effect key). */
    val configurationKey: Any

    fun rootInsetsOrNull(): PlatformRootInsets?

    fun requestApplyInsets()

    fun fallbackInsets(layoutDirection: LayoutDirection): AdaptyFlowInsets
}

@Composable
internal expect fun rememberSafeInsets(): PlatformSafeInsets

internal interface PlatformBackScope {
    /** Runs [block] with this back handler disabled. */
    fun withHandlerDisabled(block: () -> Unit)

    /** Lets the next platform back handler process the event. */
    fun forwardToSystem()
}

@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, key: Any?, onBack: PlatformBackScope.() -> Unit)

/**
 * [onResume]: first ON_RESUME for [key]; [onPause]: leaving composition, except for a configuration change.
 */
@Composable
internal expect fun OnLifecycleResumePause(
    key: Any?,
    onResume: () -> Unit,
    onPause: () -> Unit,
)

/** Disables overscroll effects for the provided subtree. */
internal expect fun noOverscroll(): ProvidedValue<*>

/** Whether the soft keyboard is currently shown. */
@Composable
internal expect fun isImeVisible(): Boolean
