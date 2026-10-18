@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import androidx.compose.animation.core.Easing
import com.adapty.internal.utils.InternalAdaptyApi

@InternalAdaptyApi
public class Transition internal constructor(
    internal val durationMillis: Float,
    internal val startDelayMillis: Float,
    internal val interpolator: Interpolator,
)

internal val Transition.easing: Easing
    get() = interpolator.toEasing()
