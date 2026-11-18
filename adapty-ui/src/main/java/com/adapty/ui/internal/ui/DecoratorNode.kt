@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image.ScaleType
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.BlendMode as LayerBlend
import com.adapty.ui.internal.ui.attributes.Decorator
import com.adapty.ui.internal.ui.attributes.DpOffset
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.attributes.asDpOffset
import com.adapty.ui.internal.ui.attributes.degreesIn
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toComposeShape
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.SoftBlur
import com.adapty.ui.internal.utils.SoftBlurMath
import com.adapty.ui.internal.utils.getBitmap
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.floor

@Composable
internal fun Modifier.decorator(
    decorator: Decorator,
    underlayAlpha: State<Float>? = null,
    animations: DecoratorAnimations? = null,
    clipContent: Boolean = decorator.clip == Decorator.Clip.CONTENT,
    alpha: Float = 1f,
    rotation: Rotation = Rotation.Default,
    scale: Scale = Scale.Default,
    overflow: OverflowExtent? = null,
): Modifier = decorator(decorator.resolve(animations), underlayAlpha, clipContent, alpha, rotation, scale, overflow)

@Composable
internal fun Modifier.decorator(
    resolved: ResolvedDecorator,
    underlayAlpha: State<Float>? = null,
    clipContent: Boolean = resolved.decorator.clip == Decorator.Clip.CONTENT,
    alpha: Float = 1f,
    rotation: Rotation = Rotation.Default,
    scale: Scale = Scale.Default,
    overflow: OverflowExtent? = null,
): Modifier {
    val shape = resolved.shape
    val canvasClip = clipContent && shape.isGeneric()
    return this
        .then(
            DecoratorElement(
                shape = shape,
                underlays = resolved.underlays,
                overlays = resolved.overlays,
                leadingShadows = resolved.leadingShadows,
                contentBlend = resolved.decorator.blend,
                clipContent = canvasClip,
                contentOverflows = !clipContent,
                overflow = overflow.takeIf { !clipContent },
                underlayAlpha = underlayAlpha,
                alpha = alpha,
                rotation = rotation,
                scale = scale,
                context = LocalContext.current,
            )
        )
        .let { if (clipContent && !canvasClip) it.clip(shape) else it }
}

internal class ResolvedDecorator(
    val decorator: Decorator,
    val shape: Shape,
    val underlays: List<ResolvedLayer>,
    val overlays: List<ResolvedLayer>,
    val leadingShadows: List<ResolvedShadow>,
) {
    val shadowCaptureExpand: Dp =
        (leadingShadows.asSequence() + (underlays + overlays).asSequence().flatMap { it.dropShadows.asSequence() })
            .maxOfOrNull { it.blurRadius + maxOf(kotlin.math.abs(it.offset.x), kotlin.math.abs(it.offset.y)) }
            ?.dp ?: 0.dp

    val blends: Boolean =
        (decorator.blend != LayerBlend.NORMAL && decorator.blend != LayerBlend.DESTINATION_IN) ||
            (underlays + overlays).any { it.layer.kind !is Decorator.Layer.Kind.Effects && it.layer.blend != LayerBlend.NORMAL }
}

@Composable
internal fun Decorator.resolve(animations: DecoratorAnimations?): ResolvedDecorator =
    ResolvedDecorator(
        decorator = this,
        shape = type.toComposeShape(),
        underlays = underlays.map { it.resolve(animations) },
        overlays = overlays.map { it.resolve(animations) },
        leadingShadows = animations.leadingShadows(),
    )

internal fun DecoratorAnimations?.leadingShadows(): List<ResolvedShadow> =
    listOfNotNull(this?.leadingShadow?.takeIf { legacyBackground == null })

internal fun DecoratorAnimations?.dropShadowsOf(layer: Decorator.Layer, own: List<ResolvedShadow>): List<ResolvedShadow> =
    if (this != null && legacyBackground === layer) listOfNotNull(leadingShadow) + own else own

private fun Shape.isGeneric(): Boolean =
    createOutline(Size(100f, 100f), LayoutDirection.Ltr, Density(1f)) is Outline.Generic

internal class DecoratorAnimations(
    val layers: Map<Decorator.Layer, LayerOverride>,
    val shadows: Map<Decorator.Effect, ResolvedShadow?>,
    val leadingShadow: ResolvedShadow?,
    val legacyBackground: Decorator.Layer?,
    val addedInnerShadow: ResolvedShadow?,
)

internal class LayerOverride(val paint: LayerPaint?, val thickness: Float?)

internal class ResolvedLayer(
    val layer: Decorator.Layer,
    val paint: LayerPaint?,
    val thickness: Float,
    val dropShadows: List<ResolvedShadow>,
    val innerShadows: List<ResolvedShadow>,
)

internal class ResolvedShadow(val color: Color, val blurRadius: Float, val offset: DpOffset)

internal sealed class LayerPaint {
    class Solid(val color: Color) : LayerPaint()
    class Shader(val brush: Brush, val opaque: Boolean = false) : LayerPaint()
    class Image(val asset: Asset.Composite<Asset.Image>) : LayerPaint()

    class CrossFade(val start: Brush, val end: Brush, val progress: Float) : LayerPaint()
}

@Composable
private fun Decorator.Layer.resolve(animations: DecoratorAnimations?): ResolvedLayer {
    val override = animations?.layers?.get(this)
    val thickness = override?.thickness ?: (kind as? Decorator.Layer.Kind.Stroke)?.thickness ?: 0f
    val dropShadows = animations.dropShadowsOf(this, effects.mapNotNull { effect ->
        if (effect.kind == Decorator.Effect.Kind.DROP) effect.resolveShadow(animations) else null
    })
    val innerShadows = if (kind is Decorator.Layer.Kind.Effects) {
        if (effects.any { it.kind == Decorator.Effect.Kind.INNER }) reportEffectsInnerShadowSkipped()
        emptyList()
    } else {
        val own = effects.mapNotNull { effect ->
            if (effect.kind == Decorator.Effect.Kind.INNER) effect.resolveShadow(animations) else null
        }
        if (animations?.legacyBackground === this) listOfNotNull(animations.addedInnerShadow) + own else own
    }
    override?.paint?.let { return ResolvedLayer(this, it, thickness, dropShadows, innerShadows) }
    val asset = when (val kind = kind) {
        is Decorator.Layer.Kind.Effects -> null
        is Decorator.Layer.Kind.Fill -> kind.asset
        is Decorator.Layer.Kind.Stroke -> kind.asset
        is Decorator.Layer.Kind.Image -> kind.asset
    }
    val paint = asset?.resolveAsset<Asset.Filling.Local>()?.let { resolved ->
        when (resolved.main) {
            is Asset.Color -> LayerPaint.Solid(resolved.cast<Asset.Color>().toComposeFill().color)
            is Asset.Gradient -> resolved.cast<Asset.Gradient>().let { gradient ->
                LayerPaint.Shader(
                    gradient.toComposeFill().shader,
                    opaque = gradient.main.isOpaque(),
                )
            }
            is Asset.Image -> LayerPaint.Image(resolved.cast<Asset.Image>())
        }
    }
    return ResolvedLayer(this, paint, thickness, dropShadows, innerShadows)
}

internal fun Asset.Gradient.isOpaque(): Boolean = values.all { (it.color.value ushr 24) == 0xFF }

internal fun LayerPaint.uniformAlpha(kind: Decorator.Layer.Kind, imageHasAlpha: Boolean?): Float? = when (this) {
    is LayerPaint.Solid -> color.alpha
    is LayerPaint.Shader -> if (opaque) 1f else null
    is LayerPaint.CrossFade -> null
    is LayerPaint.Image -> {
        val aspect = (kind as? Decorator.Layer.Kind.Image)?.aspect ?: AspectRatio.FILL
        when {
            imageHasAlpha == null -> 0f
            !imageHasAlpha && aspect != AspectRatio.FIT -> 1f
            else -> null
        }
    }
}

internal fun LayerPaint?.shadesByAlpha(): Boolean = when (this) {
    is LayerPaint.Solid -> color.alpha < 1f
    is LayerPaint.Shader -> !opaque
    is LayerPaint.CrossFade -> true
    is LayerPaint.Image, null -> false
}

private val effectsInnerShadowReported = AtomicBoolean(false)

private fun reportEffectsInnerShadowSkipped() {
    if (!effectsInnerShadowReported.compareAndSet(false, true)) return
    log(WARN) { "$LOG_PREFIX inner shadows of an effects decorator layer are not supported and are skipped" }
}

@Composable
private fun Decorator.Effect.resolveShadow(animations: DecoratorAnimations?): ResolvedShadow? {
    val shadows = animations?.shadows
    if (shadows != null && this in shadows) return shadows[this]
    return resolveShadow()
}

@Composable
private fun Decorator.Effect.resolveShadow(): ResolvedShadow? {
    val color = color.resolveAsset<Asset.Filling.Local>()?.castOrNull<Asset.Color>()?.toComposeFill()?.color
    if (color == null || color.alpha == 0f) return null
    return ResolvedShadow(color, blurRadius, offset.asDpOffset())
}

private data class DecoratorElement(
    private val shape: Shape,
    private val underlays: List<ResolvedLayer>,
    private val overlays: List<ResolvedLayer>,
    private val leadingShadows: List<ResolvedShadow>,
    private val contentBlend: LayerBlend,
    private val clipContent: Boolean,
    private val contentOverflows: Boolean,
    private val overflow: OverflowExtent?,
    private val underlayAlpha: State<Float>?,
    private val alpha: Float,
    private val rotation: Rotation,
    private val scale: Scale,
    private val context: Context,
) : ModifierNodeElement<DecoratorNode>() {
    override fun create() = DecoratorNode(
        shape, underlays, overlays, leadingShadows, contentBlend, clipContent, contentOverflows, overflow, underlayAlpha,
        alpha, rotation, scale, context,
    )

    override fun update(node: DecoratorNode) {
        node.update(
            shape, underlays, overlays, leadingShadows, contentBlend, clipContent, contentOverflows, overflow, underlayAlpha,
            alpha, rotation, scale, context,
        )
    }
}

internal class DecoratorNode(
    private var shape: Shape,
    private var underlays: List<ResolvedLayer>,
    private var overlays: List<ResolvedLayer>,
    private var leadingShadows: List<ResolvedShadow>,
    private var contentBlend: LayerBlend,
    private var clipContent: Boolean,
    private var contentOverflows: Boolean,
    private var overflow: OverflowExtent?,
    private var underlayAlpha: State<Float>?,
    private var alpha: Float,
    private var rotation: Rotation,
    private var scale: Scale,
    private var context: Context,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val geometry = ShapeGeometry()
    private val shadowSpace = ShadowSpace()
    private val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val blurFilters = HashMap<Float, BlurMaskFilter>()
    private val innerFillPaint = android.graphics.Paint()
    private val innerCutoutPaint = android.graphics.Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private val innerLayerPaint = android.graphics.Paint()
    private val innerMaskPaint = android.graphics.Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val bitmapPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    private val bitmapMatrix = Matrix()
    private val crossFadeLayerPaint = androidx.compose.ui.graphics.Paint()
    private val groupPaint = androidx.compose.ui.graphics.Paint()
    private val blendPaints = HashMap<LayerBlend, android.graphics.Paint?>()
    private var reach = 0f
    private val contentShadows = ContentShadows()
    private val softMasks = SoftShadowMasks()
    private val snapshotShadows = SnapshotShadows()
    private val snapshotCanvas = WrappedCanvas()
    private val softTintPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    private val softCutoutPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        color = android.graphics.Color.BLACK
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }

    private var recordedContent: GraphicsLayer? = null

    private var overflowGroup: GraphicsLayer? = null

    private val redraw: () -> Unit = { invalidateDraw() }

    override fun onAttach() {
        overflow?.addListener(redraw)
    }

    override fun onDetach() {
        overflow?.removeListener(redraw)
        overflowGroup?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        overflowGroup = null
        contentShadows.release(requireGraphicsContext())
        softMasks.release()
        snapshotShadows.release()
    }

    fun update(
        shape: Shape,
        underlays: List<ResolvedLayer>,
        overlays: List<ResolvedLayer>,
        leadingShadows: List<ResolvedShadow>,
        contentBlend: LayerBlend,
        clipContent: Boolean,
        contentOverflows: Boolean,
        overflow: OverflowExtent?,
        underlayAlpha: State<Float>?,
        alpha: Float,
        rotation: Rotation,
        scale: Scale,
        context: Context,
    ) {
        if (this.shape != shape) geometry.invalidate()
        this.shape = shape
        this.underlays = underlays
        this.overlays = overlays
        this.leadingShadows = leadingShadows
        this.contentBlend = contentBlend
        this.clipContent = clipContent
        this.contentOverflows = contentOverflows
        if (this.overflow !== overflow) {
            this.overflow?.removeListener(redraw)
            overflow?.addListener(redraw)
            this.overflow = overflow
        }
        this.underlayAlpha = underlayAlpha
        this.alpha = alpha
        this.rotation = rotation
        this.scale = scale
        this.context = context
    }

    override fun ContentDrawScope.draw() {
        if (alpha <= 0f) return
        geometry.update(shape, size, layoutDirection, currentValueOf(LocalDensity))
        shadowSpace.update(rotation, scale, layoutDirection, density)
        snapshotShadows.beginPass()
        recordedContent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && castsContentShadows()) {
            contentShadows.record(requireGraphicsContext(), this) { drawClippedContent() }
        } else null
        reach = shadowReach()
        try {
            if (alpha >= 1f && !hasBlendedLayers()) {
                drawDecorated()
                return
            }
            groupPaint.alpha = alpha
            if (contentOverflows && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                drawOverflowGroup()
                return
            }
            if (contentOverflows) {
                drawContext.canvas.nativeCanvas.saveLayer(overflowBounds(), groupPaint.asFrameworkPaint())
            } else {
                drawContext.canvas.saveLayer(size.toRect().inflate(reach), groupPaint)
            }
            drawDecorated()
            drawContext.canvas.restore()
        } finally {
            recordedContent = null
        }
    }

    private fun ContentDrawScope.drawOverflowGroup() {
        val bounds = overflowBounds()
        val left = floor(bounds.left).toInt()
        val top = floor(bounds.top).toInt()
        val nodeSize = IntSize(ceil(bounds.right).toInt() - left, ceil(bounds.bottom).toInt() - top)
        val layer = overflowGroup ?: requireGraphicsContext().createGraphicsLayer().also { overflowGroup = it }
        val outer = drawContext
        layer.topLeft = IntOffset(left, top)
        layer.record(Density(density, fontScale), layoutDirection, nodeSize) {
            val savedCanvas = outer.canvas
            val savedLayer = outer.graphicsLayer
            outer.canvas = drawContext.canvas
            outer.graphicsLayer = drawContext.graphicsLayer
            try {
                val canvas = outer.canvas
                canvas.translate(-left.toFloat(), -top.toFloat())
                canvas.nativeCanvas.saveLayer(bounds, groupPaint.asFrameworkPaint())
                this@drawOverflowGroup.drawDecorated()
                canvas.restore()
            } finally {
                outer.canvas = savedCanvas
                outer.graphicsLayer = savedLayer
            }
        }
        drawLayer(layer)
    }

    private fun DrawScope.overflowBounds(): android.graphics.RectF {
        val extent = overflow ?: return OVERFLOW_BOUNDS
        val r = extent.around(size.toRect().inflate(reach))
        return android.graphics.RectF(r.left, r.top, r.right, r.bottom)
    }

    private fun ContentDrawScope.castsContentShadows(): Boolean =
        size.width > 0f && size.height > 0f &&
            (underlays.any { it.castsContentShadows() } || overlays.any { it.castsContentShadows() })

    private fun ResolvedLayer.castsContentShadows() = isEffects() && dropShadows.isNotEmpty()

    private fun ResolvedLayer.isEffects() = layer.kind is Decorator.Layer.Kind.Effects

    private fun ContentDrawScope.drawClippedContent() {
        if (clipContent) {
            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipPath(geometry.shapePath)
            drawContent()
            canvas.restore()
        } else {
            drawContent()
        }
    }

    private fun ContentDrawScope.drawDecorated() {
        leadingShadows.forEach { drawDropShadow(it, ringThickness = null) }
        val gate = underlayAlpha?.value ?: 1f
        underlays.forEach {
            blended(it) {
                if (!it.isEffects()) drawDropShadows(it)
                if (gate > 0f) {
                    drawLayer(it, gate)
                    drawInnerShadows(it, gate)
                }
            }
        }
        blendedContent {
            drawEffectsDropShadows()
            recordedContent?.let { drawLayer(it) } ?: drawClippedContent()
        }
        overlays.forEach {
            blended(it) {
                if (!it.isEffects()) drawDropShadows(it)
                drawLayer(it, 1f)
                drawInnerShadows(it, 1f)
            }
        }
    }

    private fun hasBlendedLayers(): Boolean =
        contentBlendPaint() != null || underlays.any { blendPaint(it) != null } || overlays.any { blendPaint(it) != null }

    private inline fun DrawScope.blended(resolved: ResolvedLayer, block: () -> Unit) {
        val paint = blendPaint(resolved) ?: return block()
        val canvas = drawContext.canvas.nativeCanvas
        val count = canvas.saveLayer(-reach, -reach, size.width + reach, size.height + reach, paint)
        block()
        canvas.restoreToCount(count)
    }

    private fun blendPaint(resolved: ResolvedLayer): android.graphics.Paint? =
        if (resolved.isEffects()) null else blendPaint(resolved.layer.blend)

    private fun contentBlendPaint(): android.graphics.Paint? =
        if (contentBlend == LayerBlend.DESTINATION_IN) null else blendPaint(contentBlend)

    private fun blendPaint(blend: LayerBlend): android.graphics.Paint? {
        if (blend == LayerBlend.NORMAL) return null
        if (blend !in blendPaints) blendPaints[blend] = blend.platformPaint()
        return blendPaints[blend]
    }

    private inline fun DrawScope.blendedContent(block: () -> Unit) {
        val paint = contentBlendPaint() ?: return block()
        val canvas = drawContext.canvas.nativeCanvas
        val count = canvas.saveLayer(if (contentOverflows) overflowBounds() else null, paint)
        block()
        canvas.restoreToCount(count)
    }

    private fun ContentDrawScope.drawEffectsDropShadows() {
        val content = recordedContent
        if (content != null) {
            underlays.forEach { if (it.isEffects()) contentShadows.draw(this, content, it.dropShadows, shadowSpace) }
            overlays.forEach { if (it.isEffects()) contentShadows.draw(this, content, it.dropShadows, shadowSpace) }
            return
        }
        if (!castsContentShadows()) return
        snapshotShadows.begin(ceil(size.width).toInt(), ceil(size.height).toInt()) { canvas ->
            val saved = drawContext.canvas
            drawContext.canvas = snapshotCanvas.wrap(canvas)
            try {
                drawClippedContent()
            } finally {
                drawContext.canvas = saved
            }
        }
        val canvas = drawContext.canvas.nativeCanvas
        underlays.forEach { if (it.isEffects()) snapshotShadows.draw(canvas, it.dropShadows, shadowSpace) }
        overlays.forEach { if (it.isEffects()) snapshotShadows.draw(canvas, it.dropShadows, shadowSpace) }
        snapshotShadows.end()
    }

    private fun shadowReach(): Float {
        var reach = 0f
        leadingShadows.forEach { reach = maxOf(reach, shadowSpace.reach(it)) }
        underlays.forEach { layer -> layer.dropShadows.forEach { reach = maxOf(reach, shadowSpace.reach(it)) } }
        overlays.forEach { layer -> layer.dropShadows.forEach { reach = maxOf(reach, shadowSpace.reach(it)) } }
        return if (reach.isFinite()) reach else 0f
    }

    private fun DrawScope.drawDropShadows(resolved: ResolvedLayer) {
        if (resolved.dropShadows.isEmpty()) return
        val paint = resolved.paint ?: return
        val ringThickness = if (resolved.layer.kind is Decorator.Layer.Kind.Stroke) {
            val thicknessPx = resolved.thickness * density
            if (thicknessPx <= 0f) return
            thicknessPx
        } else null
        val image = (paint as? LayerPaint.Image)?.let { imageBitmap(it.asset) }
        val coverage = paint.uniformAlpha(resolved.layer.kind, image?.hasAlpha())
        if (coverage == null) {
            drawSnapshotDropShadows(resolved)
            return
        }
        if (coverage <= 0f) return
        resolved.dropShadows.forEach { drawDropShadow(it, ringThickness, coverage) }
    }

    private fun DrawScope.drawSnapshotDropShadows(resolved: ResolvedLayer) {
        if (size.width <= 0f || size.height <= 0f) return
        val image = (resolved.paint as? LayerPaint.Image)?.let { imageBitmap(it.asset) }
        snapshotShadows.begin(ceil(size.width).toInt(), ceil(size.height).toInt(), resolved, geometry.version, image) { canvas ->
            val saved = drawContext.canvas
            drawContext.canvas = snapshotCanvas.wrap(canvas)
            try {
                drawLayer(resolved, 1f)
            } finally {
                drawContext.canvas = saved
            }
        }
        snapshotShadows.draw(drawContext.canvas.nativeCanvas, resolved.dropShadows, shadowSpace)
        snapshotShadows.end()
    }

    private fun DrawScope.drawDropShadow(shadow: ResolvedShadow, ringThickness: Float?, coverage: Float = 1f) {
        val blurPx = shadowSpace.blurPx(shadow)
        val canvas = drawContext.canvas.nativeCanvas
        val ring = ringThickness?.let { geometry.ringPath(it) }
        val color = (if (coverage < 1f) shadow.color.copy(alpha = shadow.color.alpha * coverage) else shadow.color).toArgb()
        if (blurPx > 0f && !SoftBlurMath.maskFilterBlurs(canvas.isHardwareAccelerated)) {
            val area = (ring ?: geometry.shapePath).asAndroidPath()
            val mask = softMasks.get(geometry, area, ringThickness ?: -1f, blurPx, size)
            softTintPaint.color = color
            mask.draw(canvas, shadowSpace.offsetX(shadow), shadowSpace.offsetY(shadow), softTintPaint)
            return
        }
        shadowPaint.color = color
        shadowPaint.maskFilter = if (blurPx > 0f) blurFilter(blurPx) else null
        canvas.save()
        canvas.translate(shadowSpace.offsetX(shadow), shadowSpace.offsetY(shadow))
        if (ring != null) canvas.drawPath(ring.asAndroidPath(), shadowPaint)
        else when (val outline = geometry.outline) {
            is Outline.Rectangle -> outline.rect.let { canvas.drawRect(it.left, it.top, it.right, it.bottom, shadowPaint) }
            is Outline.Rounded -> {
                val r = outline.roundRect
                if (r.isSimple) {
                    canvas.drawRoundRect(
                        r.left, r.top, r.right, r.bottom,
                        r.topLeftCornerRadius.x, r.topLeftCornerRadius.y, shadowPaint,
                    )
                } else {
                    canvas.drawPath(geometry.shapePath.asAndroidPath(), shadowPaint)
                }
            }
            is Outline.Generic, null -> canvas.drawPath(geometry.shapePath.asAndroidPath(), shadowPaint)
        }
        canvas.restore()
    }

    private fun DrawScope.drawInnerShadows(resolved: ResolvedLayer, alpha: Float) {
        if (resolved.innerShadows.isEmpty() || resolved.paint == null) return
        val thickness = when (resolved.layer.kind) {
            is Decorator.Layer.Kind.Effects -> return
            is Decorator.Layer.Kind.Stroke -> {
                val thicknessPx = resolved.thickness * density
                if (thicknessPx <= 0f) return
                thicknessPx
            }
            is Decorator.Layer.Kind.Fill, is Decorator.Layer.Kind.Image -> -1f
        }
        val area = (if (thickness > 0f) geometry.ringPath(thickness) else geometry.shapePath).asAndroidPath()
        val canvas = drawContext.canvas.nativeCanvas
        val soft = !SoftBlurMath.maskFilterBlurs(canvas.isHardwareAccelerated)
        innerLayerPaint.alpha = (255 * alpha).toInt()
        val masked = resolved.paint.shadesByAlpha()
        resolved.innerShadows.forEach { shadow ->
            val blurPx = shadow.blurRadius * density
            val dx = shadow.offset.x * density
            val dy = shadow.offset.y * density
            innerFillPaint.color = shadow.color.toArgb()
            val count = canvas.save()
            canvas.clipPath(area)
            canvas.saveLayer(null, if (alpha >= 1f) null else innerLayerPaint)
            canvas.drawPaint(innerFillPaint)
            if (soft && blurPx > 0f) {
                softMasks.get(geometry, area, thickness, blurPx, size).draw(canvas, dx, dy, softCutoutPaint)
            } else {
                innerCutoutPaint.maskFilter = if (blurPx > 0f) blurFilter(blurPx) else null
                canvas.save()
                canvas.translate(dx, dy)
                canvas.drawPath(area, innerCutoutPaint)
                canvas.restore()
            }
            if (masked) {
                canvas.saveLayer(null, innerMaskPaint)
                drawLayer(resolved, 1f)
                canvas.restore()
            }
            canvas.restoreToCount(count)
        }
    }

    private fun blurFilter(radiusPx: Float): BlurMaskFilter {
        if (radiusPx !in blurFilters && blurFilters.size >= MAX_BLUR_FILTERS) blurFilters.clear()
        return blurFilters.getOrPut(radiusPx) { BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL) }
    }

    private fun DrawScope.drawLayer(resolved: ResolvedLayer, alpha: Float) {
        val paint = resolved.paint ?: return
        when (val kind = resolved.layer.kind) {
            is Decorator.Layer.Kind.Effects -> Unit
            is Decorator.Layer.Kind.Fill -> drawArea(geometry.shapePath, paint, AspectRatio.FILL, alpha)
            is Decorator.Layer.Kind.Image -> drawArea(geometry.shapePath, paint, kind.aspect, alpha)
            is Decorator.Layer.Kind.Stroke -> {
                val thicknessPx = resolved.thickness * density
                if (thicknessPx > 0f) drawArea(geometry.ringPath(thicknessPx), paint, AspectRatio.FILL, alpha)
            }
        }
    }

    private fun DrawScope.drawArea(area: Path, paint: LayerPaint, aspect: AspectRatio, alpha: Float) {
        when (paint) {
            is LayerPaint.Solid -> drawPath(area, paint.color, alpha = alpha)
            is LayerPaint.Shader -> drawPath(area, paint.brush, alpha = alpha)
            is LayerPaint.CrossFade -> {
                drawContext.canvas.saveLayer(size.toRect(), crossFadeLayerPaint)
                drawPath(area, paint.start, alpha = alpha * (1f - paint.progress))
                drawPath(area, paint.end, alpha = alpha * paint.progress, blendMode = BlendMode.Plus)
                drawContext.canvas.restore()
            }
            is LayerPaint.Image -> {
                val bitmap = imageBitmap(paint.asset) ?: return
                imageMatrix(bitmap, size, aspect, bitmapMatrix)
                bitmapPaint.alpha = (255 * alpha).toInt()
                clipPath(area) {
                    drawIntoCanvas { it.nativeCanvas.drawBitmap(bitmap, bitmapMatrix, bitmapPaint) }
                }
            }
        }
    }

    private fun DrawScope.imageBitmap(asset: Asset.Composite<Asset.Image>): Bitmap? {
        val w = size.width.toInt()
        val h = size.height.toInt()
        if (w <= 0 || h <= 0) return null
        return getBitmap(context, asset, w, h, ScaleType.FIT_MAX)?.takeIf { it.width > 0 && it.height > 0 }
    }

    private companion object {
        const val MAX_BLUR_FILTERS = 4
    }
}

internal val OVERFLOW_BOUNDS = android.graphics.RectF(-1e5f, -1e5f, 1e5f, 1e5f)

internal fun LayerBlend.platformPaint(): android.graphics.Paint? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val mode = when (this) {
            LayerBlend.NORMAL -> return null
            LayerBlend.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
            LayerBlend.SCREEN -> android.graphics.BlendMode.SCREEN
            LayerBlend.OVERLAY -> android.graphics.BlendMode.OVERLAY
            LayerBlend.DARKEN -> android.graphics.BlendMode.DARKEN
            LayerBlend.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
            LayerBlend.COLOR_DODGE -> android.graphics.BlendMode.COLOR_DODGE
            LayerBlend.COLOR_BURN -> android.graphics.BlendMode.COLOR_BURN
            LayerBlend.HARD_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
            LayerBlend.DIFFERENCE -> android.graphics.BlendMode.DIFFERENCE
            LayerBlend.EXCLUSION -> android.graphics.BlendMode.EXCLUSION
            LayerBlend.HUE -> android.graphics.BlendMode.HUE
            LayerBlend.SATURATION -> android.graphics.BlendMode.SATURATION
            LayerBlend.COLOR -> android.graphics.BlendMode.COLOR
            LayerBlend.LUMINOSITY -> android.graphics.BlendMode.LUMINOSITY
            LayerBlend.SOURCE_ATOP -> android.graphics.BlendMode.SRC_ATOP
            LayerBlend.DESTINATION_OVER -> android.graphics.BlendMode.DST_OVER
            LayerBlend.DESTINATION_OUT -> android.graphics.BlendMode.DST_OUT
            LayerBlend.DESTINATION_IN -> android.graphics.BlendMode.DST_IN
            LayerBlend.PLUS_LIGHTER -> android.graphics.BlendMode.PLUS
        }
        return android.graphics.Paint().apply { blendMode = mode }
    }
    val skiaRenderer = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val mode = when (this) {
        LayerBlend.SCREEN -> PorterDuff.Mode.SCREEN
        LayerBlend.OVERLAY -> if (skiaRenderer) PorterDuff.Mode.OVERLAY else return null.also { reportBlendFallback(this) }
        LayerBlend.DARKEN -> if (skiaRenderer) PorterDuff.Mode.DARKEN else return null.also { reportBlendFallback(this) }
        LayerBlend.LIGHTEN -> if (skiaRenderer) PorterDuff.Mode.LIGHTEN else return null.also { reportBlendFallback(this) }
        LayerBlend.SOURCE_ATOP -> PorterDuff.Mode.SRC_ATOP
        LayerBlend.DESTINATION_OVER -> PorterDuff.Mode.DST_OVER
        LayerBlend.DESTINATION_OUT -> PorterDuff.Mode.DST_OUT
        LayerBlend.DESTINATION_IN -> PorterDuff.Mode.DST_IN
        LayerBlend.PLUS_LIGHTER -> PorterDuff.Mode.ADD
        LayerBlend.NORMAL -> return null
        else -> return null.also { reportBlendFallback(this) }
    }
    return android.graphics.Paint().apply { xfermode = PorterDuffXfermode(mode) }
}

private val blendFallbackReported = AtomicBoolean(false)

private fun reportBlendFallback(blend: LayerBlend) {
    if (!blendFallbackReported.compareAndSet(false, true)) return
    log(WARN) {
        "$LOG_PREFIX blend '${blend.id}' (and other modes this API level can't draw) is drawn as normal"
    }
}

private class ShadowSpace {
    private var sx = 1f
    private var sy = 1f
    private var cos = 1.0
    private var sin = 0.0
    private var meanScale = 1f
    private var density = 1f

    fun update(rotation: Rotation, scale: Scale, layoutDirection: LayoutDirection, density: Float) {
        sx = if (scale.x != 0f) scale.x else Float.MIN_VALUE
        sy = if (scale.y != 0f) scale.y else Float.MIN_VALUE
        val angle = Math.toRadians(-rotation.degreesIn(layoutDirection).toDouble())
        cos = kotlin.math.cos(angle)
        sin = kotlin.math.sin(angle)
        meanScale = kotlin.math.sqrt((sx * sy).coerceAtLeast(Float.MIN_VALUE))
        this.density = density
    }

    fun blurPx(shadow: ResolvedShadow): Float = shadow.blurRadius * density / meanScale

    fun offsetX(shadow: ResolvedShadow): Float {
        val ux = shadow.offset.x * density / sx
        val uy = shadow.offset.y * density / sy
        return (ux * cos - uy * sin).toFloat()
    }

    fun offsetY(shadow: ResolvedShadow): Float {
        val ux = shadow.offset.x * density / sx
        val uy = shadow.offset.y * density / sy
        return (ux * sin + uy * cos).toFloat()
    }

    fun reach(shadow: ResolvedShadow): Float =
        2f * blurPx(shadow) + kotlin.math.hypot(offsetX(shadow), offsetY(shadow))
}

private class SourceKey {
    private var layer: ResolvedLayer? = null
    private var version = 0
    private var image: Bitmap? = null

    fun take(layer: ResolvedLayer?, version: Int, image: Bitmap?): Boolean {
        val same = layer != null && layer === this.layer && version == this.version && image === this.image
        this.layer = layer
        this.version = version
        this.image = image
        return same
    }
}

private class ContentShadows {
    private class Group(val layer: GraphicsLayer) {
        private var blurPx = Float.NaN
        private var color = Color.Unspecified

        fun update(blurPx: Float, color: Color) {
            if (blurPx != this.blurPx) {
                this.blurPx = blurPx
                layer.renderEffect = if (blurPx > 0f) BlurEffect(blurPx, blurPx, TileMode.Decal) else null
            }
            if (color != this.color) {
                this.color = color
                layer.colorFilter = ColorFilter.tint(color)
            }
        }
    }

    private var graphicsContext: GraphicsContext? = null
    private var content: GraphicsLayer? = null
    private val groups = ArrayList<Group>()
    private var usedGroups = 0

    private var density = Density(1f)

    fun record(context: GraphicsContext, scope: ContentDrawScope, block: () -> Unit): GraphicsLayer {
        graphicsContext = context
        usedGroups = 0
        if (density.density != scope.density || density.fontScale != scope.fontScale) {
            density = Density(scope.density, scope.fontScale)
        }
        val layer = content ?: context.createGraphicsLayer().also { content = it }
        val outer = scope.drawContext
        val size = IntSize(ceil(scope.size.width).toInt(), ceil(scope.size.height).toInt())
        layer.record(density, scope.layoutDirection, size) {
            val savedCanvas = outer.canvas
            val savedLayer = outer.graphicsLayer
            outer.canvas = drawContext.canvas
            outer.graphicsLayer = drawContext.graphicsLayer
            try {
                block()
            } finally {
                outer.canvas = savedCanvas
                outer.graphicsLayer = savedLayer
            }
        }
        return layer
    }

    fun draw(scope: DrawScope, content: GraphicsLayer, shadows: List<ResolvedShadow>, space: ShadowSpace) {
        var start = 0
        while (start < shadows.size) {
            val head = shadows[start]
            var end = start + 1
            while (end < shadows.size && shadows[end].sharesLayerWith(head)) end++
            val group = nextGroup() ?: return
            val blurPx = space.blurPx(head)
            group.update(blurPx, head.color)
            val reach = ceil(2f * blurPx)
            val size = IntSize(
                ceil(scope.size.width + 2f * reach).toInt(),
                ceil(scope.size.height + 2f * reach).toInt(),
            )
            group.layer.record(density, scope.layoutDirection, size) {
                translate(reach, reach) { drawLayer(content) }
            }
            for (i in start until end) {
                val shadow = shadows[i]
                scope.translate(space.offsetX(shadow) - reach, space.offsetY(shadow) - reach) {
                    drawLayer(group.layer)
                }
            }
            start = end
        }
    }

    private fun nextGroup(): Group? {
        if (usedGroups == groups.size) {
            val context = graphicsContext ?: return null
            groups += Group(context.createGraphicsLayer())
        }
        return groups[usedGroups++]
    }

    fun release(context: GraphicsContext) {
        content?.let { context.releaseGraphicsLayer(it) }
        content = null
        groups.forEach { context.releaseGraphicsLayer(it.layer) }
        groups.clear()
        graphicsContext = null
    }

    private fun ResolvedShadow.sharesLayerWith(other: ResolvedShadow) =
        blurRadius == other.blurRadius && color == other.color
}

private class SnapshotShadows {
    private class Slot(val blur: SoftBlur) {
        val key = SourceKey()
        var blurPx = Float.NaN
        var width = 0
        var height = 0
    }

    private val slots = ArrayList<Slot>()
    private var used = 0
    private var sourceStart = 0
    private var width = 0
    private var height = 0
    private var layer: ResolvedLayer? = null
    private var version = 0
    private var image: Bitmap? = null
    private var source: ((android.graphics.Canvas) -> Unit)? = null
    private val tint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    fun beginPass() {
        used = 0
    }

    fun begin(
        width: Int,
        height: Int,
        layer: ResolvedLayer? = null,
        version: Int = 0,
        image: Bitmap? = null,
        source: (android.graphics.Canvas) -> Unit,
    ) {
        this.width = width
        this.height = height
        this.layer = layer
        this.version = version
        this.image = image
        this.source = source
        sourceStart = used
    }

    fun draw(canvas: android.graphics.Canvas, shadows: List<ResolvedShadow>, space: ShadowSpace) {
        val source = source ?: return
        shadows.forEach { shadow ->
            val blurPx = space.blurPx(shadow)
            val blur = blurFor(blurPx, source)
            tint.color = shadow.color.toArgb()
            blur.draw(canvas, space.offsetX(shadow), space.offsetY(shadow), tint)
        }
    }

    fun end() {
        source = null
        layer = null
        image = null
    }

    private fun blurFor(blurPx: Float, source: (android.graphics.Canvas) -> Unit): SoftBlur {
        for (i in sourceStart until used) if (slots[i].blurPx == blurPx) return slots[i].blur
        if (used == slots.size) slots += Slot(SoftBlur())
        val slot = slots[used++]
        val same = slot.key.take(layer, version, image) &&
            slot.blurPx == blurPx && slot.width == width && slot.height == height
        if (!same) {
            slot.blurPx = blurPx
            slot.width = width
            slot.height = height
            slot.blur.render(width, height, SoftBlurMath.sigmaOf(blurPx), source)
        }
        return slot.blur
    }

    fun release() {
        slots.forEach { it.blur.release() }
        slots.clear()
        used = 0
        sourceStart = 0
        end()
    }
}

private class WrappedCanvas {
    private var native: android.graphics.Canvas? = null
    private var wrapped: androidx.compose.ui.graphics.Canvas? = null

    fun wrap(canvas: android.graphics.Canvas): androidx.compose.ui.graphics.Canvas {
        wrapped?.takeIf { native === canvas }?.let { return it }
        native = canvas
        return androidx.compose.ui.graphics.Canvas(canvas).also { wrapped = it }
    }
}

internal fun imageMatrix(image: Bitmap, size: Size, aspect: AspectRatio, dst: Matrix): Matrix {
    val placement = imagePlacement(image.width, image.height, size, aspect)
    dst.setScale(placement.scaleX, placement.scaleY)
    dst.postTranslate(placement.dx, placement.dy)
    return dst
}

internal class ImagePlacement(val scaleX: Float, val scaleY: Float, val dx: Float, val dy: Float)

internal fun imagePlacement(width: Int, height: Int, size: Size, aspect: AspectRatio): ImagePlacement {
    val sx = size.width / width
    val sy = size.height / height
    return when (aspect) {
        AspectRatio.STRETCH -> ImagePlacement(sx, sy, 0f, 0f)
        AspectRatio.FILL, AspectRatio.FIT -> {
            val scale = if (aspect == AspectRatio.FILL) maxOf(sx, sy) else minOf(sx, sy)
            val dy = if (aspect == AspectRatio.FILL) 0f else (size.height - height * scale) / 2f
            ImagePlacement(scale, scale, (size.width - width * scale) / 2f, dy)
        }
    }
}

private class SoftShadowMasks {
    private class Mask(var version: Int, var thickness: Float, var blurPx: Float, val blur: SoftBlur)

    private val masks = ArrayList<Mask>(MAX_MASKS)
    private val solid = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    fun get(geometry: ShapeGeometry, area: android.graphics.Path, thickness: Float, blurPx: Float, size: Size): SoftBlur {
        val version = geometry.version
        masks.firstOrNull { it.version == version && it.thickness == thickness && it.blurPx == blurPx }
            ?.let { return it.blur }
        val iterator = masks.iterator()
        while (iterator.hasNext()) {
            val mask = iterator.next()
            if (mask.version != version) {
                mask.blur.release()
                iterator.remove()
            }
        }
        val mask = if (masks.size >= MAX_MASKS) masks.removeAt(0) else Mask(version, thickness, blurPx, SoftBlur())
        mask.version = version
        mask.thickness = thickness
        mask.blurPx = blurPx
        masks += mask
        mask.blur.render(ceil(size.width).toInt(), ceil(size.height).toInt(), SoftBlurMath.sigmaOf(blurPx)) {
            it.drawPath(area, solid)
        }
        return mask.blur
    }

    fun release() {
        masks.forEach { it.blur.release() }
        masks.clear()
    }

    private companion object {
        const val MAX_MASKS = 4
    }
}

private class ShapeGeometry {
    val shapePath = Path()
    var outline: Outline? = null
        private set
    private var size = Size.Unspecified
    private var layoutDirection: LayoutDirection? = null
    private var density = Float.NaN
    private val rings = HashMap<Float, Path>()

    var version = 0
        private set

    fun invalidate() {
        size = Size.Unspecified
    }

    fun update(shape: Shape, size: Size, layoutDirection: LayoutDirection, density: Density) {
        if (size == this.size && layoutDirection == this.layoutDirection && density.density == this.density) return
        this.size = size
        this.layoutDirection = layoutDirection
        this.density = density.density
        version++
        rings.clear()
        shapePath.reset()
        val outline = shape.createOutline(size, layoutDirection, density)
        this.outline = outline
        when (outline) {
            is Outline.Rectangle -> shapePath.addRect(outline.rect)
            is Outline.Rounded -> shapePath.addRoundRect(outline.roundRect)
            is Outline.Generic -> shapePath.addPath(outline.path)
        }
    }

    fun ringPath(thickness: Float): Path {
        if (thickness !in rings && rings.size >= MAX_RINGS) rings.clear()
        return rings.getOrPut(thickness) { buildRing(thickness) }
    }

    private fun buildRing(thickness: Float): Path {
        val stroke = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = thickness * 2f
        }
        val ring = android.graphics.Path()
        @Suppress("DEPRECATION")
        stroke.getFillPath(shapePath.asAndroidPath(), ring)
        ring.op(shapePath.asAndroidPath(), android.graphics.Path.Op.INTERSECT)
        return Path().apply { asAndroidPath().set(ring) }
    }

    private companion object {
        const val MAX_RINGS = 4
    }
}
