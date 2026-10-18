@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui.element

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.attributes.ComposeFill
import com.adapty.ui.internal.ui.drawImageFill

/**
 * Radial progress paths the shared renderer cannot draw with plain DrawScope arcs:
 * a stroked arc ([strokeWidth] != null) filled with [imageFill] or [brush], or a pie ([strokeWidth] == null) filled with [imageFill].
 */
internal actual fun DrawScope.drawRadialProgress(
    topLeft: Offset,
    diameter: Float,
    startAngle: Float,
    sweepAngle: Float,
    strokeWidth: Float?,
    roundCaps: Boolean,
    brush: Brush?,
    imageFill: ComposeFill.Image?,
) {
    if (strokeWidth != null) {
        if (imageFill != null) {
            drawArcWithImageFill(imageFill, topLeft, diameter, strokeWidth, roundCaps, startAngle, sweepAngle)
        } else if (brush != null) {
            drawArcWithBrushFill(brush, topLeft, diameter, strokeWidth, roundCaps, startAngle, sweepAngle)
        }
    } else if (imageFill != null) {
        drawDiscWithImageFill(imageFill, topLeft, diameter, startAngle, sweepAngle)
    }
}

private fun DrawScope.drawArcWithBrushFill(
    brush: Brush,
    topLeft: Offset,
    arcSize: Float,
    strokeWidth: Float,
    roundCaps: Boolean,
    startAngle: Float,
    directedSweep: Float,
) {
    val rect = RectF(topLeft.x, topLeft.y, topLeft.x + arcSize, topLeft.y + arcSize)

    val strokePaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        if (roundCaps) strokeCap = android.graphics.Paint.Cap.ROUND
    }

    val arcPath = android.graphics.Path().apply {
        addArc(rect, startAngle, directedSweep)
    }

    val fillPath = android.graphics.Path()
    strokePaint.getFillPath(arcPath, fillPath)

    val composePath = Path().also { it.asAndroidPath().set(fillPath) }
    drawPath(composePath, brush = brush, style = Fill)
}

private fun DrawScope.drawArcWithImageFill(
    imageFill: ComposeFill.Image,
    topLeft: Offset,
    arcSize: Float,
    strokeWidth: Float,
    roundCaps: Boolean,
    startAngle: Float,
    directedSweep: Float,
) {
    val rect = RectF(topLeft.x, topLeft.y, topLeft.x + arcSize, topLeft.y + arcSize)

    val strokePaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        if (roundCaps) strokeCap = android.graphics.Paint.Cap.ROUND
    }

    val arcPath = android.graphics.Path()
    arcPath.addArc(rect, startAngle, directedSweep)

    val fillPath = android.graphics.Path()
    strokePaint.getFillPath(arcPath, fillPath)

    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(Path().also { it.asAndroidPath().set(fillPath) })
        drawImageFill(imageFill.image, imageFill.matrix, imageFill.alpha)
        canvas.restore()
    }
}

private fun DrawScope.drawDiscWithImageFill(
    imageFill: ComposeFill.Image,
    topLeft: Offset,
    diameter: Float,
    startAngle: Float,
    directedSweep: Float,
) {
    val rect = RectF(topLeft.x, topLeft.y, topLeft.x + diameter, topLeft.y + diameter)
    val clipPath = Path().apply {
        asAndroidPath().apply {
            moveTo(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
            arcTo(rect, startAngle, directedSweep, false)
            close()
        }
    }
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(clipPath)
        drawImageFill(imageFill.image, imageFill.matrix, imageFill.alpha)
        canvas.restore()
    }
}
