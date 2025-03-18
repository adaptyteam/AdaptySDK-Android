package com.adapty.ui.internal.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.pow

internal class RectWithArcShape(private val arcHeight: Float, private val segments: Int = 50) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val bounds = Rect(0f, 0f, size.width, size.height)
        path.moveTo(bounds.left, bounds.bottom)
        when {
            arcHeight > 0f -> {
                val yOffset = bounds.top + arcHeight
                path.lineTo(bounds.left, yOffset)
                addParabola(path, bounds, yOffset, bounds.top, segments)
            }
            arcHeight < 0f -> {
                path.lineTo(bounds.left, bounds.top)
                addParabola(path, bounds, bounds.top, bounds.top - arcHeight, segments)
            }
            else -> {
                path.lineTo(bounds.left, bounds.top)
                path.lineTo(bounds.right, bounds.top)
            }
        }

        path.lineTo(bounds.right, bounds.bottom)
        path.close()

        return Outline.Generic(path)
    }

    private fun addParabola(path: Path, bounds: Rect, startY: Float, peakY: Float, segments: Int) {
        val a = (startY - peakY) * 4 / bounds.width.pow(2)
        for (i in 0..segments) {
            val x = bounds.left + bounds.width * i / segments
            val y = a * (x - bounds.center.x).pow(2) + peakY
            path.lineTo(x, y)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RectWithArcShape

        if (arcHeight != other.arcHeight) return false
        if (segments != other.segments) return false

        return true
    }

    override fun hashCode(): Int {
        var result = arcHeight.hashCode()
        result = 31 * result + segments
        return result
    }
}

internal object CircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radius = minOf(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        return Outline.Generic(
            Path().apply {
                addOval(
                    Rect(
                        center.x - radius,
                        center.y - radius,
                        center.x + radius,
                        center.y + radius
                    )
                )
            }
        )
    }
}