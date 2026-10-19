package com.adapty.ui.internal.ui.element

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.adapty.ui.internal.ui.attributes.ComposeFill
import com.adapty.ui.internal.ui.drawImageFill
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PathUtils
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Path as SkiaPath

// Mirrors RadialProgressElement.android.kt on Skia: android.graphics.Path/Paint wrap the same SkPath::addArc/arcTo
// and skpathutils::FillPathWithPaint (Paint.getFillPath), so the outlines are identical.
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
        if (imageFill == null && brush == null) return
        val outline = strokedArcOutline(topLeft, diameter, strokeWidth, roundCaps, startAngle, sweepAngle)
        when {
            imageFill != null -> drawClippedImageFill(outline, imageFill)
            brush != null -> drawPath(outline, brush = brush, style = Fill)
        }
    } else if (imageFill != null) {
        drawClippedImageFill(pieOutline(topLeft, diameter, startAngle, sweepAngle), imageFill)
    }
}

/** Fill outline of an arc stroked with [strokeWidth] (Android `Paint.getFillPath` of `Path.addArc`). */
internal fun strokedArcOutline(
    topLeft: Offset,
    diameter: Float,
    strokeWidth: Float,
    roundCaps: Boolean,
    startAngle: Float,
    sweepAngle: Float,
): Path {
    val arc = SkiaPath().addArc(ovalRect(topLeft, diameter), startAngle, sweepAngle)
    val paint = Paint().apply {
        mode = PaintMode.STROKE
        this.strokeWidth = strokeWidth
        if (roundCaps) strokeCap = PaintStrokeCap.ROUND
    }
    try {
        return PathUtils.fillPathWithPaint(arc, paint).asComposePath()
    } finally {
        // Skia objects are native; release them per frame instead of waiting for GC
        paint.close()
        arc.close()
    }
}

/** Pie slice from the center (Android `moveTo(center)` + `arcTo(rect, start, sweep, false)` + `close()`). */
internal fun pieOutline(topLeft: Offset, diameter: Float, startAngle: Float, sweepAngle: Float): Path =
    SkiaPath()
        .moveTo(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
        .arcTo(ovalRect(topLeft, diameter), startAngle, sweepAngle, false)
        .closePath()
        .asComposePath()

private fun ovalRect(topLeft: Offset, diameter: Float): Rect =
    Rect.makeLTRB(topLeft.x, topLeft.y, topLeft.x + diameter, topLeft.y + diameter)

private fun DrawScope.drawClippedImageFill(clip: Path, imageFill: ComposeFill.Image) {
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(clip)
        drawImageFill(imageFill.image, imageFill.matrix, imageFill.alpha)
        canvas.restore()
    }
}
