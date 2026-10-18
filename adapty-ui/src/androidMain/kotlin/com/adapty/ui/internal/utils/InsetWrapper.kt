@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

internal sealed class InsetWrapper {
    abstract fun getTop(density: Density): Int
    abstract fun getBottom(density: Density): Int
    abstract fun getLeft(density: Density, layoutDirection: LayoutDirection): Int
    abstract fun getRight(density: Density, layoutDirection: LayoutDirection): Int

    val isCustom get() = this is Custom

    class System(internal val insets: WindowInsets): InsetWrapper() {
        override fun getBottom(density: Density): Int {
            return insets.getBottom(density)
        }

        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
            return insets.getLeft(density, layoutDirection)
        }

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
            return insets.getRight(density, layoutDirection)
        }

        override fun getTop(density: Density): Int {
            return insets.getTop(density)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as System

            return insets == other.insets
        }

        override fun hashCode(): Int {
            return insets.hashCode()
        }
    }

    class Custom(internal val insets: AdaptyFlowInsets): InsetWrapper() {
        override fun getBottom(density: Density): Int {
            return insets.bottom
        }

        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
            return if (layoutDirection == LayoutDirection.Rtl) insets.end else insets.start
        }

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
            return if (layoutDirection == LayoutDirection.Rtl) insets.start else insets.end
        }

        override fun getTop(density: Density): Int {
            return insets.top
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Custom

            return insets == other.insets
        }

        override fun hashCode(): Int {
            return insets.hashCode()
        }
    }
}

internal fun WindowInsets.wrap() = InsetWrapper.System(this)

internal fun AdaptyFlowInsets.wrap() = InsetWrapper.Custom(this)

internal val LocalCustomInsets = staticCompositionLocalOf<InsetWrapper.Custom> {
    AdaptyFlowInsets.Unspecified.wrap()
}

@Composable
internal fun getInsets() = LocalCustomInsets.current.takeIf { it.insets != AdaptyFlowInsets.Unspecified }
    ?: WindowInsets.safeContent.wrap()

internal fun View.rootBarOrCutoutInsets(): Insets? =
    ViewCompat.getRootWindowInsets(this)?.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    )

@Composable
internal fun systemInsetsDelivered(): Boolean {
    val insets = WindowInsets.safeContent
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return insets.getTop(density) > 0 ||
        insets.getBottom(density) > 0 ||
        insets.getLeft(density, layoutDirection) > 0 ||
        insets.getRight(density, layoutDirection) > 0
}

internal const val INSETS_DELIVERY_WAIT_FRAMES = 3

internal fun View.fallbackBarOrCutoutInsets(layoutDirection: LayoutDirection): AdaptyFlowInsets {
    val rootInsets = rootBarOrCutoutInsets() ?: return AdaptyFlowInsets.None
    if (rootInsets == Insets.NONE) return AdaptyFlowInsets.None
    val windowWidth = rootView.width
    val windowHeight = rootView.height
    val laidOut = width > 0 && height > 0 && windowWidth > 0 && windowHeight > 0
    val location = IntArray(2)
    if (laidOut) {
        getLocationInWindow(location)
    } else {
        log(VERBOSE) { "$LOG_PREFIX fallback insets before layout, assuming a window-sized view" }
    }
    val viewWidth = if (laidOut) width else windowWidth
    val viewHeight = if (laidOut) height else windowHeight
    val gapLeft = location[0].coerceAtLeast(0)
    val gapTop = location[1].coerceAtLeast(0)
    val gapRight = (windowWidth - (location[0] + viewWidth)).coerceAtLeast(0)
    val gapBottom = (windowHeight - (location[1] + viewHeight)).coerceAtLeast(0)
    val left = (rootInsets.left - gapLeft).coerceIn(0, rootInsets.left)
    val top = (rootInsets.top - gapTop).coerceIn(0, rootInsets.top)
    val right = (rootInsets.right - gapRight).coerceIn(0, rootInsets.right)
    val bottom = (rootInsets.bottom - gapBottom).coerceIn(0, rootInsets.bottom)
    return when (layoutDirection) {
        LayoutDirection.Rtl -> AdaptyFlowInsets.of(right, top, left, bottom)
        else -> AdaptyFlowInsets.of(left, top, right, bottom)
    }
}

@Stable
internal data class SafeAreaInsets(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
) {
    companion object {
        val Zero = SafeAreaInsets(0.dp, 0.dp, 0.dp, 0.dp)
    }
}

@Stable
internal data class ScreenDimensions(
    val widthDp: Float,
    val heightDp: Float,
) {
    companion object {
        val Zero = ScreenDimensions(0f, 0f)
    }
}

internal val LocalSafeAreaInsets = staticCompositionLocalOf { SafeAreaInsets.Zero }

internal val LocalScreenDimensions = staticCompositionLocalOf { ScreenDimensions.Zero }

@Composable
internal fun computeSafeAreaInsets(density: Density, layoutDirection: LayoutDirection): SafeAreaInsets {
    val insets = getInsets()
    return with(density) {
        SafeAreaInsets(
            start = insets.getLeft(density, layoutDirection).toDp(),
            top = insets.getTop(density).toDp(),
            end = insets.getRight(density, layoutDirection).toDp(),
            bottom = insets.getBottom(density).toDp(),
        )
    }
}
