package com.adapty.internal.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.DeviceInfo
import com.adapty.internal.data.models.DeviceKind
import kotlin.math.min
import kotlin.math.roundToInt

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DeviceInfoResolver(
    private val appContext: Context,
) {

    fun current(): DeviceInfo {
        val (widthPx, heightPx) = physicalDisplaySizePx()
        val density = appContext.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val widthDp = (widthPx / density).roundToInt()
        val heightDp = (heightPx / density).roundToInt()

        val kind = if (min(widthDp, heightDp) >= TABLET_MIN_SMALLEST_WIDTH_DP) DeviceKind.TAB else DeviceKind.PHONE

        return DeviceInfo(kind = kind, horizontal = widthDp, vertical = heightDp)
    }

    private fun physicalDisplaySizePx(): Pair<Int, Int> {
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels to metrics.heightPixels
        }
    }

    private companion object {
        const val TABLET_MIN_SMALLEST_WIDTH_DP = 600
    }
}
