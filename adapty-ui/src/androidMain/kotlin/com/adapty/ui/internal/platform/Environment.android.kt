@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.platform

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.internal.utils.areAnimationsDisabled
import com.adapty.ui.internal.utils.fallbackBarOrCutoutInsets
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.getProgressCustomColorOrNull
import com.adapty.ui.internal.utils.rootBarOrCutoutInsets

// Environment seams: Android actuals.

/** Opaque platform context; Android wraps [Context]. */
internal actual class PlatformContextHandle(actual val context: Context) {
    actual fun activityOrNull(): Activity? = context.getActivityOrNull()

    override fun equals(other: Any?): Boolean =
        other is PlatformContextHandle && other.context == context

    override fun hashCode(): Int = context.hashCode()
}

/** True when the host activity is being recreated for a configuration change. */
internal actual fun PlatformContextHandle.isChangingConfigurations(): Boolean =
    activityOrNull()?.isChangingConfigurations == true

@Composable
internal actual fun rememberPlatformContext(): PlatformContextHandle {
    val context = LocalContext.current
    return remember(context) { PlatformContextHandle(context) }
}

internal actual fun screenSmallestWidthDp(): Int = Resources.getSystem().configuration.smallestScreenWidthDp

internal actual fun osVersion(): String = Build.VERSION.RELEASE

internal actual fun systemDensity(): Float = Resources.getSystem().displayMetrics.density

internal actual fun parseUri(value: String): Uri = value.toUri()

@Composable
internal actual fun areAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.areAnimationsDisabled()
    }
}

@Composable
internal actual fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

internal actual fun progressIndicatorColor(ctx: PlatformContextHandle): Int? =
    ctx.context.getProgressCustomColorOrNull()

/** Forces adjust-resize soft input mode on the host window; returns a restore callback (null when nothing changed). */
internal actual fun setSoftInputMode(ctx: PlatformContextHandle): (() -> Unit)? {
    val window = ctx.context.getActivityOrNull()?.window
    val savedSoftInputMode = window?.attributes?.softInputMode
    if (window != null && savedSoftInputMode != null) {
        val forced = (savedSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST.inv()) or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        window.setSoftInputMode(forced)
    }
    return {
        if (window != null && savedSoftInputMode != null) {
            window.setSoftInputMode(savedSoftInputMode)
        }
    }
}

/** Root window bar/cutout insets as reported by the platform view hierarchy. */
internal actual class PlatformRootInsets(private val insets: Insets) {
    actual val isNone: Boolean get() = insets == Insets.NONE

    override fun toString(): String = insets.toString()
}

/** Safe-area probe used before Compose window insets are delivered; Android wraps the host View + Configuration. */
internal actual class PlatformSafeInsets(private val view: View, private val configuration: Configuration) {
    actual val screenHeightDp: Int get() = configuration.screenHeightDp

    /** Changes whenever the platform configuration changes (effect key). */
    actual val configurationKey: Any get() = configuration

    actual fun rootInsetsOrNull(): PlatformRootInsets? = view.rootBarOrCutoutInsets()?.let { PlatformRootInsets(it) }

    actual fun requestApplyInsets() {
        view.requestApplyInsets()
    }

    actual fun fallbackInsets(layoutDirection: LayoutDirection): AdaptyFlowInsets =
        view.fallbackBarOrCutoutInsets(layoutDirection)
}

@Composable
internal actual fun rememberSafeInsets(): PlatformSafeInsets {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    return remember(view, configuration) { PlatformSafeInsets(view, configuration) }
}

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, key: Any?, onBack: PlatformBackScope.() -> Unit) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val currentEnabled = rememberUpdatedState(enabled)
    val activeCallback = remember { arrayOfNulls<OnBackPressedCallback>(1) }
    SideEffect { activeCallback[0]?.isEnabled = enabled }
    DisposableEffect(backDispatcher, key) {
        val callback = object : OnBackPressedCallback(currentEnabled.value), PlatformBackScope {
            override fun handleOnBackPressed() {
                onBack()
            }

            override fun withHandlerDisabled(block: () -> Unit) {
                isEnabled = false
                block()
                isEnabled = currentEnabled.value
            }

            override fun forwardToSystem() {
                isEnabled = false
                backDispatcher?.onBackPressed()
                isEnabled = currentEnabled.value
            }
        }
        backDispatcher?.addCallback(callback)
        activeCallback[0] = callback
        onDispose {
            callback.remove()
            if (activeCallback[0] === callback) activeCallback[0] = null
        }
    }
}

/**
 * [onResume]: first ON_RESUME for [key]; [onPause]: leaving composition, except for a configuration change.
 */
@Composable
internal actual fun OnLifecycleResumePause(
    key: Any?,
    onResume: () -> Unit,
    onPause: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val hasAppeared = rememberSaveable(key) { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!hasAppeared.value) {
                        hasAppeared.value = true
                        onResume()
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            val isChangingConfig = context.getActivityOrNull()?.isChangingConfigurations ?: false
            if (!isChangingConfig) {
                onPause()
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun isImeVisible(): Boolean = WindowInsets.isImeVisible

@OptIn(ExperimentalFoundationApi::class)
internal actual fun noOverscroll(): ProvidedValue<*> = LocalOverscrollConfiguration provides null
