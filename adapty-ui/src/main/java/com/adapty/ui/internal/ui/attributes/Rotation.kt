package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.TransformOrigin

@Immutable
internal data class Rotation(
    val degrees: Float,
    val anchor: Point,
) {
    companion object {
        val Default = Rotation(0f, Point.NormalizedCenter)
    }
}

internal fun Point.asTransformOrigin() =
    if (this == Point.NormalizedCenter) TransformOrigin.Center else TransformOrigin(x, y)
