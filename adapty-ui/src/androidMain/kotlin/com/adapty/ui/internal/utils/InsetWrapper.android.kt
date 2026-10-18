@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.utils

import android.view.View
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.adapty.ui.AdaptyFlowInsets
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

// View-based bar/cutout insets (split from InsetWrapper.kt; used by the PlatformSafeInsets actual).

internal fun View.rootBarOrCutoutInsets(): Insets? =
    ViewCompat.getRootWindowInsets(this)?.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    )

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
