@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInBounce
import androidx.compose.animation.core.EaseInElastic
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.EaseInOutElastic
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.EaseOutElastic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable
import com.adapty.internal.utils.InternalAdaptyApi

@Immutable
@InternalAdaptyApi
public data class Animation<T> internal constructor(
    val start: T,
    val end: T,
    val durationMillis: Int,
    val startDelayMillis: Int,
    val repeatDelayMillis: Int,
    val pingPongDelayMillis: Int,
    internal val interpolator: Interpolator,
    val repeatMode: RepeatMode?,
    val repeatMaxCount: Int,
    val role: Role,
) {
    public enum class RepeatMode {
        Normal, PingPong
    }

    public enum class Role {
        Offset, Scale, Rotation, Opacity, Background, Border, Box, Shadow
    }
}

internal sealed class Interpolator {
    class Named(val name: String): Interpolator()
    class CubicBezier(
        val a: Float,
        val b: Float,
        val c: Float,
        val d: Float,
    ): Interpolator()
}

internal val Animation<*>.easing: Easing
    get() = when (interpolator) {
        is Interpolator.Named -> when (interpolator.name) {
            "ease_in_out" -> EaseInOut
            "ease_in" -> EaseIn
            "ease_out" -> EaseOut
            "linear" -> LinearEasing
            "ease_in_elastic" -> EaseInElastic
            "ease_out_elastic" -> EaseOutElastic
            "ease_in_out_elastic" -> EaseInOutElastic
            "ease_in_bounce" -> EaseInBounce
            "ease_out_bounce" -> EaseOutBounce
            "ease_in_out_bounce" -> EaseInOutBounce
            else -> EaseInOut
        }
        is Interpolator.CubicBezier ->
            CubicBezierEasing(interpolator.a, interpolator.b, interpolator.c, interpolator.d)
    }
