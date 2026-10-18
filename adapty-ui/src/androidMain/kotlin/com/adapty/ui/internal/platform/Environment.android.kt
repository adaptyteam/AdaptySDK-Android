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

// Environment seams (Task 6c turns them into expect/actual).

/** Opaque platform context; Android wraps [Context]. */
internal class PlatformContextHandle(val context: Context) {
    fun activityOrNull(): Activity? = context.getActivityOrNull()

    override fun equals(other: Any?): Boolean =
        other is PlatformContextHandle && other.context == context

    override fun hashCode(): Int = context.hashCode()
}

/** True when the host activity is being recreated for a configuration change. */
internal fun PlatformContextHandle.isChangingConfigurations(): Boolean =
    activityOrNull()?.isChangingConfigurations == true

@Composable
internal fun rememberPlatformContext(): PlatformContextHandle {
    val context = LocalContext.current
    return remember(context) { PlatformContextHandle(context) }
}

internal fun screenSmallestWidthDp(): Int = Resources.getSystem().configuration.smallestScreenWidthDp

internal fun osVersion(): String = Build.VERSION.RELEASE

internal fun systemDensity(): Float = Resources.getSystem().displayMetrics.density

internal fun parseUri(value: String): Uri = value.toUri()

@Composable
internal fun areAnimationsDisabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.areAnimationsDisabled()
    }
}

@Composable
internal fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

internal fun progressIndicatorColor(ctx: PlatformContextHandle): Int? =
    ctx.context.getProgressCustomColorOrNull()

/** Forces adjust-resize soft input mode on the host window; returns a restore callback (null when nothing changed). */
internal fun setSoftInputMode(ctx: PlatformContextHandle): (() -> Unit)? {
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
internal class PlatformRootInsets(private val insets: Insets) {
    val isNone: Boolean get() = insets == Insets.NONE

    override fun toString(): String = insets.toString()
}

/** Safe-area probe used before Compose window insets are delivered; Android wraps the host View + Configuration. */
internal class PlatformSafeInsets(private val view: View, private val configuration: Configuration) {
    val screenHeightDp: Int get() = configuration.screenHeightDp

    /** Changes whenever the platform configuration changes (effect key). */
    val configurationKey: Any get() = configuration

    fun rootInsetsOrNull(): PlatformRootInsets? = view.rootBarOrCutoutInsets()?.let { PlatformRootInsets(it) }

    fun requestApplyInsets() {
        view.requestApplyInsets()
    }

    fun fallbackInsets(layoutDirection: LayoutDirection): AdaptyFlowInsets =
        view.fallbackBarOrCutoutInsets(layoutDirection)
}

@Composable
internal fun rememberSafeInsets(): PlatformSafeInsets {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    return remember(view, configuration) { PlatformSafeInsets(view, configuration) }
}

internal interface PlatformBackScope {
    /** Runs [block] with this back handler disabled. */
    fun withHandlerDisabled(block: () -> Unit)

    /** Lets the next platform back handler process the event. */
    fun forwardToSystem()
}

@Composable
internal fun PlatformBackHandler(enabled: Boolean, key: Any?, onBack: PlatformBackScope.() -> Unit) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backDispatcher, key) {
        val callback = object : OnBackPressedCallback(enabled), PlatformBackScope {
            override fun handleOnBackPressed() {
                onBack()
            }

            override fun withHandlerDisabled(block: () -> Unit) {
                isEnabled = false
                block()
                isEnabled = true
            }

            override fun forwardToSystem() {
                isEnabled = false
                backDispatcher?.onBackPressed()
                isEnabled = true
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }
}

/**
 * [onResume]: first ON_RESUME for [key]; [onPause]: leaving composition, except for a configuration change.
 */
@Composable
internal fun OnLifecycleResumePause(
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
