@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius as ComposeCornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape as ComposeShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsFloat
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.fitImageWithinBounds
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.ZeroIntrinsicsModifier
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.evaluateComposeImageAlignment
import com.adapty.ui.internal.ui.attributes.toComposeContentScale
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toEasing
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.utils.OneWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getBitmap
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.resolveAsset
import kotlinx.coroutines.delay

@InternalAdaptyApi
public class LinearProgressElement internal constructor(
    internal val orientation: Orientation,
    internal val assetId: VisualValue,
    internal val value: OneWayBinding,
    internal val durationMillis: Int,
    internal val min: Float,
    internal val max: Float,
    internal val skipAnimationOnOverflow: Boolean,
    internal val cornerRadius: Shape.CornerRadius?,
    internal val imageAspect: AspectRatio,
    internal val clip: Boolean,
    internal val interpolator: Interpolator,
    internal val actions: List<Action>,
    override val baseProps: BaseProps,
) : UIElement {

    internal sealed class Orientation {
        class Horizontal(val align: HorizontalAlign) : Orientation()
        class Vertical(val align: VerticalAlign) : Orientation()
    }

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val state = resolveState()
        val screen = LocalScreenInstance.current
        val rawValue = state[value].toJsFloat() ?: 0f
        val span = max - min
        val targetValue = if (span > 0f) ((rawValue - min) / span).coerceIn(0f, 1f) else 0f
        val skipAnimation = skipAnimationOnOverflow && (rawValue < min || rawValue > max)

        var prevRaw by remember { mutableFloatStateOf(rawValue) }
        val animatedValue by animateFloatAsState(
            targetValue = targetValue,
            animationSpec = if (skipAnimation) snap() else tween(
                durationMillis = durationMillis,
                easing = interpolator.toEasing(),
            ),
            label = "progress",
        )

        LaunchedEffect(rawValue) {
            if (rawValue != prevRaw) {
                prevRaw = rawValue
                if (actions.isNotEmpty()) {
                    delay(if (skipAnimation) 0L else durationMillis.toLong())
                    dispatch(Message.ActionsRequested(actions, screen))
                }
            }
        }

        val fillAsset = assetId.resolveAsset<Asset.Filling.Local>()
        val fillColor = fillAsset?.castOrNull<Asset.Color>()?.toComposeFill()?.color
        val fillBrush = fillAsset?.castOrNull<Asset.Gradient>()?.toComposeFill()?.shader
        val fillImage = fillAsset?.castOrNull<Asset.Image>()
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val context = LocalContext.current
        val imageContentScale = imageAspect.toComposeContentScale()
        val imageAlignment = imageAspect.evaluateComposeImageAlignment(LocalContentAlignment.current)

        val shape = cornerRadius?.let {
            RoundedCornerShape(it.topLeft.dp, it.topRight.dp, it.bottomRight.dp, it.bottomLeft.dp)
        }

        BoxWithConstraints(modifier = modifier.then(ZeroIntrinsicsModifier)) {
            val imageBitmap = remember(fillImage?.main?.source, isSystemInDarkTheme, constraints.maxWidth, constraints.maxHeight) {
                fillImage?.let {
                    getBitmap(context, it, constraints.maxWidth, constraints.maxHeight, Asset.Image.ScaleType.FIT_MAX)
                        ?.asImageBitmap()
                }
            }
            val isHorizontal = orientation is Orientation.Horizontal

            @Composable
            fun assetContent() {
                when {
                    imageBitmap != null -> Image(
                        bitmap = imageBitmap,
                        contentDescription = null,
                        contentScale = imageContentScale,
                        alignment = imageAlignment,
                        modifier = Modifier.fillMaxSize(),
                    )
                    fillBrush != null -> Box(modifier = Modifier.fillMaxSize().background(brush = fillBrush))
                    fillColor != null -> Box(modifier = Modifier.fillMaxSize().background(color = fillColor))
                }
            }

            if (clip) {
                val maskShape = if (isHorizontal) {
                    val hAlign = (orientation as Orientation.Horizontal).align
                    HorizontalProgressMaskShape(animatedValue, hAlign, cornerRadius)
                } else {
                    val vAlign = (orientation as Orientation.Vertical).align
                    VerticalProgressMaskShape(animatedValue, vAlign, cornerRadius)
                }
                val fitBitmap = imageBitmap?.takeIf { imageAspect == AspectRatio.FIT }
                if (fitBitmap != null) {
                    val ratio = fitBitmap.width.toFloat() / fitBitmap.height.toFloat()
                    val fitModifier = Modifier.fitImageWithinBounds(ratio, constraints).clip(maskShape)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = fitBitmap,
                            contentDescription = null,
                            modifier = fitModifier,
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().clip(maskShape)) {
                        assetContent()
                    }
                }
            } else if (isHorizontal) {
                val hAlign = (orientation as Orientation.Horizontal).align
                val alignment: Alignment = when (hAlign) {
                    HorizontalAlign.START -> Alignment.CenterStart
                    HorizontalAlign.END -> Alignment.CenterEnd
                    HorizontalAlign.LEFT -> AbsoluteAlignment.CenterLeft
                    HorizontalAlign.RIGHT -> AbsoluteAlignment.CenterRight
                    HorizontalAlign.CENTER -> Alignment.Center
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
                    val fillModifier = Modifier.fillMaxWidth(animatedValue).fillMaxHeight()
                    val clippedModifier = if (shape != null) fillModifier.clip(shape) else fillModifier
                    Box(modifier = clippedModifier) {
                        assetContent()
                    }
                }
            } else {
                val vAlign = (orientation as Orientation.Vertical).align
                val alignment: Alignment = when (vAlign) {
                    VerticalAlign.TOP -> Alignment.TopCenter
                    VerticalAlign.CENTER -> Alignment.Center
                    VerticalAlign.BOTTOM -> Alignment.BottomCenter
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
                    val fillModifier = Modifier.fillMaxWidth().fillMaxHeight(animatedValue)
                    val clippedModifier = if (shape != null) fillModifier.clip(shape) else fillModifier
                    Box(modifier = clippedModifier) {
                        assetContent()
                    }
                }
            }
        }
    }
}

private class HorizontalProgressMaskShape(
    private val progress: Float,
    private val align: HorizontalAlign,
    private val cornerRadius: Shape.CornerRadius?,
) : ComposeShape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val fillWidth = size.width * progress.coerceIn(0f, 1f)
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val offsetX = when (align) {
            HorizontalAlign.START, HorizontalAlign.LEFT ->
                if (isRtl) size.width - fillWidth else 0f
            HorizontalAlign.END, HorizontalAlign.RIGHT ->
                if (isRtl) 0f else size.width - fillWidth
            HorizontalAlign.CENTER -> (size.width - fillWidth) / 2f
            else -> if (isRtl) size.width - fillWidth else 0f
        }
        val rect = Rect(offsetX, 0f, offsetX + fillWidth, size.height)
        return Outline.Generic(buildProgressPath(rect, cornerRadius, density))
    }
}

private class VerticalProgressMaskShape(
    private val progress: Float,
    private val align: VerticalAlign,
    private val cornerRadius: Shape.CornerRadius?,
) : ComposeShape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val fillHeight = size.height * progress.coerceIn(0f, 1f)
        val offsetY = when (align) {
            VerticalAlign.TOP -> 0f
            VerticalAlign.CENTER -> (size.height - fillHeight) / 2f
            else -> size.height - fillHeight
        }
        val rect = Rect(0f, offsetY, size.width, offsetY + fillHeight)
        return Outline.Generic(buildProgressPath(rect, cornerRadius, density))
    }
}

private fun buildProgressPath(rect: Rect, cornerRadius: Shape.CornerRadius?, density: Density): Path {
    return Path().apply {
        if (cornerRadius == null) {
            addRect(rect)
            return@apply
        }
        val maxR = minOf(rect.width, rect.height) / 2f
        val tl = with(density) { cornerRadius.topLeft.dp.toPx() }.coerceAtMost(maxR)
        val tr = with(density) { cornerRadius.topRight.dp.toPx() }.coerceAtMost(maxR)
        val br = with(density) { cornerRadius.bottomRight.dp.toPx() }.coerceAtMost(maxR)
        val bl = with(density) { cornerRadius.bottomLeft.dp.toPx() }.coerceAtMost(maxR)
        addRoundRect(
            RoundRect(
                rect = rect,
                topLeft = ComposeCornerRadius(tl),
                topRight = ComposeCornerRadius(tr),
                bottomRight = ComposeCornerRadius(br),
                bottomLeft = ComposeCornerRadius(bl),
            )
        )
    }
}
