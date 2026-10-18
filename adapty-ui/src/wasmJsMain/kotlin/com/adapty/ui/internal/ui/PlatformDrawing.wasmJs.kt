package com.adapty.ui.internal.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.PathDirection
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import kotlin.math.ceil
import kotlin.math.roundToInt

// Drawing seams on Skia; each mirrors the Android actual in Modifier.android.kt.

/**
 * Radius-to-sigma conversion Android applies to both blurs: BlurMaskFilter (SkBlurMask::ConvertRadiusToSigma) and
 * RenderEffect.createBlurEffect (hwui jni/RenderEffect.cpp passes Blur::convertRadiusToSigma(radius) to
 * SkImageFilters::Blur). CMP's skiko BlurEffect applies the same conversion.
 */
internal fun radiusToSigma(radius: Float): Float = if (radius > 0f) 0.57735f * radius + 0.5f else 0f

/** Sigma of the [platformBlur] image filter for a blur radius in px (Android RenderEffect semantics). */
internal fun platformBlurSigma(blurPx: Float): Float = radiusToSigma(blurPx)

/** Outer drop shadow of [outline]; [spread] is not supported (Android ignores it too). */
internal actual fun DrawScope.drawPlatformShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
    spread: Float,
) {
    val paint = Paint()
    paint.asFrameworkPaint().apply {
        this.color = color.toArgb()
        if (blurRadiusPx > 0f) maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, radiusToSigma(blurRadiusPx))
    }

    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(offset.x, offset.y)

        when (outline) {
            is Outline.Rectangle -> canvas.drawRect(outline.rect, paint)
            // Android draws drawRoundRect with the top-left radius only
            is Outline.Rounded -> {
                val rr = outline.roundRect
                canvas.nativeCanvas.drawRRect(
                    RRect.makeLTRB(rr.left, rr.top, rr.right, rr.bottom, rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y),
                    paint.asFrameworkPaint(),
                )
            }
            is Outline.Generic -> canvas.drawPath(outline.path, paint)
        }

        canvas.restore()
    }
}

/** Inner shadow of [outline] over already drawn content (saveLayer + DST_OUT cutout + DST_IN clip, as on Android). */
internal actual fun DrawScope.drawPlatformInnerShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    val shadowPaint = org.jetbrains.skia.Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
    }
    val cutoutPaint = org.jetbrains.skia.Paint().apply {
        isAntiAlias = true
        blendMode = BlendMode.DST_OUT
        if (blurRadiusPx > 0f) maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, radiusToSigma(blurRadiusPx))
    }
    val clipPaint = org.jetbrains.skia.Paint().apply {
        isAntiAlias = true
        blendMode = BlendMode.DST_IN
    }

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val path = outline.toSkiaPath()
        nc.saveLayer(null, null)

        nc.drawPaint(shadowPaint)

        nc.save()
        nc.translate(offset.x, offset.y)
        nc.drawPath(path, cutoutPaint)
        nc.restore()

        nc.drawPath(path, clipPaint)

        nc.restore()
    }
}

private fun Outline.toSkiaPath(): org.jetbrains.skia.Path =
    when (this) {
        is Outline.Rectangle -> org.jetbrains.skia.Path().addRect(
            Rect.makeLTRB(rect.left, rect.top, rect.right, rect.bottom),
            PathDirection.CLOCKWISE,
        )
        is Outline.Rounded -> {
            val rr = roundRect
            org.jetbrains.skia.Path().addRRect(
                RRect.makeComplexLTRB(
                    rr.left, rr.top, rr.right, rr.bottom,
                    floatArrayOf(
                        rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y,
                        rr.topRightCornerRadius.x, rr.topRightCornerRadius.y,
                        rr.bottomRightCornerRadius.x, rr.bottomRightCornerRadius.y,
                        rr.bottomLeftCornerRadius.x, rr.bottomLeftCornerRadius.y,
                    ),
                ),
                PathDirection.CLOCKWISE,
            )
        }
        is Outline.Generic -> path.asSkiaPath()
    }

/** Row-major 3x3 (android.graphics.Matrix values) to a Compose 4x4 matrix. */
internal fun composeMatrixFromRowMajor(matrix: FloatArray): Matrix {
    val m = Matrix()
    m[0, 0] = matrix[0]
    m[1, 0] = matrix[1]
    m[3, 0] = matrix[2]
    m[0, 1] = matrix[3]
    m[1, 1] = matrix[4]
    m[3, 1] = matrix[5]
    m[0, 3] = matrix[6]
    m[1, 3] = matrix[7]
    m[3, 3] = matrix[8]
    return m
}

/** Draws [image] transformed by a row-major 3x3 [matrix]. */
internal actual fun DrawScope.drawImageFill(image: ImageBitmap, matrix: FloatArray, alpha: Float) {
    // Android quantizes paint alpha to 0..255
    val quantized = if (alpha < 1f) (alpha * 255f).roundToInt().coerceIn(0, 255) / 255f else 1f
    withTransform({ transform(composeMatrixFromRowMajor(matrix)) }) {
        drawImage(image, alpha = quantized)
    }
}

/**
 * Blurs the drawn content like the Android RenderNode path: content is captured within the bounds expanded by
 * blur radius + [captureExpand], blurred with DECAL edges and clipped to that same expanded rect.
 */
internal actual fun Modifier.platformBlur(radius: Dp, captureExpand: Dp): Modifier =
    this.then(BlurElement(radius, captureExpand))

private data class BlurElement(
    private val blurRadius: Dp,
    private val captureExpand: Dp,
) : ModifierNodeElement<BlurNode>() {
    override fun create() = BlurNode(blurRadius, captureExpand)
    override fun update(node: BlurNode) {
        node.blurRadius = blurRadius
        node.captureExpand = captureExpand
    }
}

private class BlurNode(
    var blurRadius: Dp,
    var captureExpand: Dp,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    override fun ContentDrawScope.draw() {
        val density = currentValueOf(LocalDensity)
        val blurPx = with(density) { blurRadius.toPx() }
        val w = size.width.toInt()
        val h = size.height.toInt()
        if (blurPx <= 0f || w <= 0 || h <= 0) {
            drawContent()
            return
        }

        val expand = ceil(blurPx + captureExpand.toPx()).toInt().toFloat()
        val bounds = Rect.makeLTRB(-expand, -expand, w + expand, h + expand)
        val sigma = platformBlurSigma(blurPx)
        val layerPaint = org.jetbrains.skia.Paint().apply {
            imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.DECAL, null, null)
        }

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.clipRect(bounds)
            nc.saveLayer(bounds, layerPaint)
            drawContent()
            nc.restore()
            nc.restore()
        }
    }
}
