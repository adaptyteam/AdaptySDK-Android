@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Brush
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.attributes.ComposeFill

@ColorInt
internal fun String.parseColorInt(): Int {
    return try {
        Color.parseColor(
            when (length) {
                9 -> rgbaToArgbStr(this)
                else -> this
            }
        )
    } catch (e: Exception) {
        throw adaptyError(
            message = "color value should be a valid #RRGGBB or #RRGGBBAA",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            originalError = e
        )
    }
}

private fun rgbaToArgbStr(rgbaColorString: String): String {
    return rgbaColorString.toCharArray().let { chars ->
        val a1 = chars[7]
        val a2 = chars[8]
        for (i in 8 downTo 3) {
            chars[i] = chars[i - 2]
        }
        chars[1] = a1
        chars[2] = a2
        String(chars)
    }
}

internal fun Brush.Companion.color(color: androidx.compose.ui.graphics.Color): Brush =
    Brush.linearGradient(colors = listOf(color, color))

internal val ComposeFill.Color.shader get() = Brush.color(color)
