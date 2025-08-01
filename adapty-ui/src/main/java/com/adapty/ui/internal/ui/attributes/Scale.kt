package com.adapty.ui.internal.ui.attributes

import androidx.compose.runtime.Immutable

@Immutable
internal data class Scale(
    private val point: Point,
    val anchor: Point,
) {
    val y: Float get() = point.y
    val x: Float get() = point.x

    companion object {
        val Default = Scale(Point.One, Point.NormalizedCenter)
    }
}
