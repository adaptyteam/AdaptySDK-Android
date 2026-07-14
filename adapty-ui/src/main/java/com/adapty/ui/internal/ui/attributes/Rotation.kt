package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.LayoutDirection

@Immutable
internal data class Rotation(
    val degrees: Float,
    val anchor: Point,
) {
    companion object {
        val Default = Rotation(0f, Point.NormalizedCenter)
    }
}

internal fun Rotation.degreesIn(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Rtl) -degrees else degrees

internal fun Point.asTransformOrigin(layoutDirection: LayoutDirection = LayoutDirection.Ltr) =
    when {
        this == Point.NormalizedCenter -> TransformOrigin.Center
        layoutDirection == LayoutDirection.Rtl -> TransformOrigin(1f - x, y)
        else -> TransformOrigin(x, y)
    }
