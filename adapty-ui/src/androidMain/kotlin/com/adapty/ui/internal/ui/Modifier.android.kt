@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.ui

import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import kotlin.math.roundToInt

// Drawing seams; Android bodies moved from Modifier.kt.

/** Outer drop shadow of [outline]; [spread] is not supported on Android (callers pass 0). */
internal actual fun DrawScope.drawPlatformShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
    @Suppress("UNUSED_PARAMETER") spread: Float,
) {
    val paint = Paint()
    val maskFilter = if (blurRadiusPx > 0f) BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL) else null

    paint.asFrameworkPaint().apply {
        this.color = color.toArgb()
        this.maskFilter = maskFilter
    }

    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(offset.x, offset.y)

        when (outline) {
            is Outline.Rectangle -> canvas.drawRect(outline.rect, paint)
            is Outline.Rounded -> {
                val roundRect = outline.roundRect
                canvas.nativeCanvas.drawRoundRect(
                    roundRect.left,
                    roundRect.top,
                    roundRect.right,
                    roundRect.bottom,
                    roundRect.topLeftCornerRadius.x,
                    roundRect.topLeftCornerRadius.y,
                    paint.asFrameworkPaint()
                )
            }
            is Outline.Generic -> canvas.drawPath(outline.path, paint)
        }

        canvas.restore()
    }
}

/** Inner shadow of [outline], drawn over already drawn content. */
internal actual fun DrawScope.drawPlatformInnerShadow(
    outline: Outline,
    color: Color,
    blurRadiusPx: Float,
    offset: Offset,
) {
    val shadowPaint = android.graphics.Paint()
    val cutoutPaint = android.graphics.Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    val clipPaint = android.graphics.Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    val outlinePath = android.graphics.Path()
    val maskFilter = if (blurRadiusPx > 0f) BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL) else null

    shadowPaint.color = color.toArgb()
    cutoutPaint.maskFilter = maskFilter

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.saveLayer(null, null)

        nc.drawPaint(shadowPaint)

        nc.save()
        nc.translate(offset.x, offset.y)
        nc.drawPath(outline.toNativePath(outlinePath), cutoutPaint)
        nc.restore()

        nc.drawPath(outline.toNativePath(outlinePath), clipPaint)

        nc.restore()
    }
}

private fun Outline.toNativePath(dst: android.graphics.Path): android.graphics.Path {
    dst.rewind()
    when (this) {
        is Outline.Rectangle -> {
            dst.addRect(rect.left, rect.top, rect.right, rect.bottom, android.graphics.Path.Direction.CW)
        }
        is Outline.Rounded -> {
            val rr = roundRect
            dst.addRoundRect(
                rr.left, rr.top, rr.right, rr.bottom,
                floatArrayOf(
                    rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y,
                    rr.topRightCornerRadius.x, rr.topRightCornerRadius.y,
                    rr.bottomRightCornerRadius.x, rr.bottomRightCornerRadius.y,
                    rr.bottomLeftCornerRadius.x, rr.bottomLeftCornerRadius.y,
                ),
                android.graphics.Path.Direction.CW,
            )
        }
        is Outline.Generic -> dst.set(path.asAndroidPath())
    }
    return dst
}

/** Draws [image] transformed by a row-major 3x3 [matrix]. */
internal actual fun DrawScope.drawImageFill(image: ImageBitmap, matrix: FloatArray, alpha: Float) {
    val paint = android.graphics.Paint()
    if (alpha < 1f) paint.alpha = (alpha * 255f).roundToInt().coerceIn(0, 255)
    val nativeMatrix = android.graphics.Matrix().apply { setValues(matrix) }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawBitmap(image.asAndroidBitmap(), nativeMatrix, paint)
    }
}

/** Blurs the drawn content (RenderEffect on API 31+, StackBlur below). */
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

    private var renderNode: android.graphics.RenderNode? = null
    private var cachedHwBlurPx: Float = Float.NaN

    private var cachedBitmap: com.adapty.ui.internal.platform.Bitmap? = null
    private var cachedSwCanvas: android.graphics.Canvas? = null
    private var cachedComposeCanvas: androidx.compose.ui.graphics.Canvas? = null
    private var blurWorker: com.adapty.ui.internal.utils.StackBlurWorker? = null

    override fun ContentDrawScope.draw() {
        val density = currentValueOf(LocalDensity)
        val blurPx = with(density) { blurRadius.toPx() }

        if (blurPx <= 0f) {
            drawContent()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            drawBlurHardware(blurPx)
        } else {
            drawBlurSoftware(blurPx)
        }
    }

    private fun ContentDrawScope.captureExpandPx(blurPx: Float): Int =
        kotlin.math.ceil(blurPx + captureExpand.toPx()).toInt()

    @android.annotation.SuppressLint("NewApi")
    private fun ContentDrawScope.drawBlurHardware(blurPx: Float) {
        val w = size.width.toInt()
        val h = size.height.toInt()
        if (w <= 0 || h <= 0) { drawContent(); return }

        val expand = captureExpandPx(blurPx)
        val nodeW = w + expand * 2
        val nodeH = h + expand * 2

        val node = renderNode
            ?: android.graphics.RenderNode("blur").also {
                renderNode = it
                cachedHwBlurPx = Float.NaN
            }

        if (node.width != nodeW || node.height != nodeH) {
            node.setPosition(0, 0, nodeW, nodeH)
        }
        if (cachedHwBlurPx != blurPx) {
            cachedHwBlurPx = blurPx
            node.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.DECAL)
            )
        }

        val expandF = expand.toFloat()
        val recordingCanvas = node.beginRecording()
        recordingCanvas.translate(expandF, expandF)
        val originalCanvas = drawContext.canvas
        drawContext.canvas = androidx.compose.ui.graphics.Canvas(recordingCanvas)
        drawContent()
        drawContext.canvas = originalCanvas
        node.endRecording()

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.translate(-expandF, -expandF)
            nc.drawRenderNode(node)
            nc.restore()
        }
    }

    private fun ContentDrawScope.drawBlurSoftware(blurPx: Float) {
        val w = size.width.toInt()
        val h = size.height.toInt()
        if (w <= 0 || h <= 0) { drawContent(); return }

        val expand = captureExpandPx(blurPx)
        val bmpW = w + expand * 2
        val bmpH = h + expand * 2

        val bitmap = cachedBitmap
            ?.takeIf { it.width == bmpW && it.height == bmpH && !it.isRecycled }
            ?: android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                .also {
                    cachedBitmap = it
                    val swCanvas = android.graphics.Canvas(it)
                    cachedSwCanvas = swCanvas
                    cachedComposeCanvas = androidx.compose.ui.graphics.Canvas(swCanvas)
                }

        bitmap.eraseColor(0)
        cachedSwCanvas!!.setBitmap(bitmap)

        val bitmapCanvas = cachedComposeCanvas!!
        val originalCanvas = drawContext.canvas

        drawContext.canvas = bitmapCanvas
        bitmapCanvas.save()
        bitmapCanvas.translate(expand.toFloat(), expand.toFloat())
        drawContent()
        bitmapCanvas.restore()
        drawContext.canvas = originalCanvas

        val worker = blurWorker
            ?.takeIf { it.fits(bmpW, bmpH) }
            ?: com.adapty.ui.internal.utils.StackBlurWorker(bmpW, bmpH).also { blurWorker = it }
        worker.blur(bitmap, blurPx.toInt().coerceAtLeast(1))

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmap(bitmap, -expand.toFloat(), -expand.toFloat(), null)
        }
    }

    override fun onDetach() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedSwCanvas = null
        cachedComposeCanvas = null
        blurWorker = null
        renderNode = null
    }
}
