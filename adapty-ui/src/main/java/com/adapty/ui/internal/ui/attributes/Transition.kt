@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import com.adapty.internal.utils.InternalAdaptyApi

@InternalAdaptyApi
public sealed class Transition(
    internal val durationMillis: Int,
    internal val startDelayMillis: Int,
    internal val interpolatorName: String,
) {
    public class Fade(
        durationMillis: Int,
        startDelayMillis: Int,
        interpolatorName: String,
    ): Transition(durationMillis, startDelayMillis, interpolatorName)

    public class Slide(
        durationMillis: Int,
        startDelayMillis: Int,
        interpolatorName: String,
    ): Transition(durationMillis, startDelayMillis, interpolatorName)
}

internal val Transition.easing: Easing
    get() = when (interpolatorName) {
        "ease_in_out" -> EaseInOut
        "ease_in" -> EaseIn
        "ease_out" -> EaseOut
        "linear" -> LinearEasing
        else -> EaseInOut
    }
