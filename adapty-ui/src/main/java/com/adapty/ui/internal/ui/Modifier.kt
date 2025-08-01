@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.asTransformOrigin
import com.adapty.ui.internal.ui.attributes.horizontalSumOrDefault
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toComposeShape
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.verticalSumOrDefault
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.Border
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.OffsetProvider
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.ui.element.BoxProvider
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import android.graphics.BlurMaskFilter
import com.adapty.ui.internal.ui.attributes.DpOffset
import com.adapty.ui.internal.ui.attributes.Point
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.element.ShadowProvider

@InternalAdaptyApi
public fun Modifier.fillWithBaseParams(element: UIElement, resolveAssets: ResolveAssets): Modifier = composed {
    val rotationProvider = rememberRotationProvider(element.baseProps)
    val scaleProvider = rememberScaleProvider(element.baseProps)
    val offsetProvider = rememberOffsetProvider(element.baseProps)
    val opacityProvider = rememberOpacityProvider(element.baseProps)
    val boxProvider = rememberBoxProvider(element.baseProps)
    val shadowProvider = rememberShadowProvider(element.baseProps, resolveAssets)

    val rotation = rotationProvider.rotation.value
    val scale = scaleProvider.scale.value
    val alpha = opacityProvider.alpha.value
    val transformOrigin = (listOf(scale.anchor, rotation.anchor).firstOrNull { it != Point.NormalizedCenter } ?: Point.NormalizedCenter).asTransformOrigin()

    return@composed this
        .sizeAndMarginsOrSkip(element, boxProvider)
        .shadowOrSkip(
            shadowProvider,
            element.baseProps.shape?.type?.toComposeShape() ?: RectangleShape,
            transformOrigin,
            rotation,
            scale,
        )
        .offsetOrSkip(offsetProvider)
        .graphicsLayer(
            rotationZ = rotation.degrees,
            transformOrigin = transformOrigin,
            scaleX = scale.x,
            scaleY = scale.y,
            alpha = alpha,
        )
        .backgroundOrSkip(element.baseProps, resolveAssets)
}

internal fun Modifier.backgroundOrSkip(
    decorator: com.adapty.ui.internal.ui.attributes.Shape?,
    resolveAssets: ResolveAssets,
): Modifier = composed {
    val decorator = decorator ?: return@composed this
    var modifier = this
    val backgroundShape = decorator.type.toComposeShape()
    modifier = modifier.clipToShape(backgroundShape)
    if (decorator.fill != null) {
        val background = resolveAssets().getAsset<Asset.Filling.Local>(decorator.fill.assetId)
        if (background != null)
            modifier = modifier.background(background, backgroundShape)
    }

    if (decorator.border != null) {
        val border = resolveAssets().getAsset<Asset.Filling.Local>(decorator.border.color)
        when (border?.main) {
            is Asset.Color -> {
                modifier = modifier.border(
                    decorator.border.thickness.dp,
                    border.cast<Asset.Color>().toComposeFill().color,
                    decorator.border.shapeType.toComposeShape(),
                )
            }
            is Asset.Gradient -> {
                modifier = modifier.border(
                    decorator.border.thickness.dp,
                    border.cast<Asset.Gradient>().toComposeFill().shader,
                    decorator.border.shapeType.toComposeShape(),
                )
            }
            else -> Unit
        }
    }
    modifier
}

internal fun Modifier.backgroundOrSkip(
    baseProps: BaseProps,
    resolveAssets: ResolveAssets,
): Modifier = composed {
    val decorator = baseProps.shape ?: return@composed this
    var modifier = this
    val backgroundShape = decorator.type.toComposeShape()
    modifier = modifier.clipToShape(backgroundShape)
    if (decorator.fill != null) {
        val background = resolveAssets().getAsset<Asset.Filling.Local>(decorator.fill.assetId)
        if (background != null)
            modifier = modifier.background(background, backgroundShape, baseProps, resolveAssets)
    }

    if (decorator.border != null) {
        val border = resolveAssets().getAsset<Asset.Filling.Local>(decorator.border.color)
        if (border != null) {
            modifier = modifier.border(border, decorator.border.shapeType.toComposeShape(), baseProps, resolveAssets)
        }
    }
    modifier
}

private fun Modifier.clipToShape(
    shape: Shape,
) = composed {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val useCustomClip = remember(shape) {
        shape.createOutline(Size(100f, 100f), layoutDirection, density) is Outline.Generic
    }

    if (useCustomClip) {
        drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, density)
            val path = (outline as Outline.Generic).path

            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipPath(path)
            drawContent()
            canvas.restore()
        }
    } else {
        clip(shape)
    }
}

private fun Modifier.background(
    background: Asset.Composite<Asset.Filling.Local>,
    shape: Shape,
    baseProps: BaseProps,
    resolveAssets: ResolveAssets,
): Modifier = composed {
    val backgroundAnimations = baseProps.onAppear
        ?.filter { anim -> anim.role == Animation.Role.Background }
        ?.takeIf { it.isNotEmpty() }

    val hasMixedAssetTypeAnimations = backgroundAnimations?.let { animations ->
        val assets = resolveAssets()
        animations.any { anim ->
            val startAssetId = anim.start as? String
            val endAssetId = anim.end as? String
            if (startAssetId != null && endAssetId != null) {
                val startAsset = assets.getAsset<Asset.Filling.Local>(startAssetId)
                val endAsset = assets.getAsset<Asset.Filling.Local>(endAssetId)
                val startIsColor = startAsset?.castOrNull<Asset.Color>() != null
                val startIsGradient = startAsset?.castOrNull<Asset.Gradient>() != null
                val endIsColor = endAsset?.castOrNull<Asset.Color>() != null
                val endIsGradient = endAsset?.castOrNull<Asset.Gradient>() != null
                
                (startIsColor && endIsGradient) || (startIsGradient && endIsColor)
            } else false
        }
    } ?: false

    if (hasMixedAssetTypeAnimations) {
        val gradientProvider = rememberGradientProvider(baseProps, resolveAssets)
        val brush = gradientProvider.brush.value
        
        if (brush is CrossFadeGradientBrush) {
            backgroundWithCrossFade(brush, shape)
        } else {
            background(brush = brush, shape = shape)
        }
    } else {
        when (background.main) {
            is Asset.Color -> {
                val colorProvider = rememberColorProvider(baseProps, resolveAssets)
                background(color = colorProvider.value, shape = shape)
            }
            is Asset.Gradient -> {
                val gradientProvider = rememberGradientProvider(baseProps, resolveAssets)
                val brush = gradientProvider.brush.value
                background(brush = brush, shape = shape)
            }
            is Asset.Image -> {
                val context = LocalContext.current
                drawBehind {
                    val fill = background.cast<Asset.Image>().toComposeFill(context, size) ?: return@drawBehind
                    drawIntoCanvas { canvas ->
                        canvas.save()
                        if (shape != RectangleShape) {
                            val path = Path()
                            shape.createOutline(size, layoutDirection, this).let { outline ->
                                when (outline) {
                                    is Outline.Rectangle -> path.addRect(outline.rect)
                                    is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                                    is Outline.Generic -> path.addPath(outline.path)
                                }
                            }
                            canvas.clipPath(path)
                        }
                        canvas.nativeCanvas.drawBitmap(fill.image, fill.matrix, fill.paint)
                        canvas.restore()
                    }
                }
            }
        }
    }
}

private fun Modifier.background(
    background: Asset.Composite<Asset.Filling.Local>,
    shape: Shape,
): Modifier = composed {
    when (background.main) {
        is Asset.Color -> {
            val fill = background.cast<Asset.Color>().toComposeFill()
            background(color = fill.color, shape = shape)
        }
        is Asset.Gradient -> {
            val fill = background.cast<Asset.Gradient>().toComposeFill()
            background(brush = fill.shader, shape = shape)
        }
        is Asset.Image -> {
            val context = LocalContext.current
            drawBehind {
                val fill = background.cast<Asset.Image>().toComposeFill(context, size) ?: return@drawBehind
                drawIntoCanvas { canvas ->
                    canvas.save()
                    if (shape != RectangleShape) {
                        val path = Path()
                        shape.createOutline(size, layoutDirection, this).let { outline ->
                            when (outline) {
                                is Outline.Rectangle -> path.addRect(outline.rect)
                                is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                                is Outline.Generic -> path.addPath(outline.path)
                            }
                        }
                        canvas.clipPath(path)
                    }
                    canvas.nativeCanvas.drawBitmap(fill.image, fill.matrix, fill.paint)
                    canvas.restore()
                }
            }
        }
    }
}

private fun Modifier.border(
    border: Asset.Composite<Asset.Filling.Local>,
    shape: Shape,
    baseProps: BaseProps,
    resolveAssets: ResolveAssets,
): Modifier = composed {
    val borderAnimations = baseProps.onAppear
        ?.filter { anim -> anim.role == Animation.Role.Border }
        ?.takeIf { it.isNotEmpty() }

    val hasMixedAssetTypeAnimations = borderAnimations?.let { animations ->
        val assets = resolveAssets()
        animations.any { anim ->
            val startBorder = anim.start as? Border
            val endBorder = anim.end as? Border
            if (startBorder?.color != null && endBorder?.color != null) {
                val startAsset = assets.getAsset<Asset.Filling.Local>(startBorder.color)
                val endAsset = assets.getAsset<Asset.Filling.Local>(endBorder.color)
                val startIsColor = startAsset?.castOrNull<Asset.Color>() != null
                val startIsGradient = startAsset?.castOrNull<Asset.Gradient>() != null
                val endIsColor = endAsset?.castOrNull<Asset.Color>() != null
                val endIsGradient = endAsset?.castOrNull<Asset.Gradient>() != null
                
                (startIsColor && endIsGradient) || (startIsGradient && endIsColor)
            } else false
        }
    } ?: false

    if (hasMixedAssetTypeAnimations) {
        val gradientProvider = rememberBorderGradientProvider(baseProps, resolveAssets)
        val thicknessProvider = rememberBorderThicknessProvider(baseProps)
        val brush = gradientProvider.brush.value
        
        if (brush is CrossFadeGradientBrush) {
            borderWithCrossFade(brush, shape, thicknessProvider.value)
        } else {
            border(
                width = thicknessProvider.value,
                brush = brush,
                shape = shape
            )
        }
    } else {
        when (border.main) {
            is Asset.Color -> {
                val colorProvider = rememberBorderColorProvider(baseProps, resolveAssets)
                val thicknessProvider = rememberBorderThicknessProvider(baseProps)
                border(
                    width = thicknessProvider.value,
                    color = colorProvider.value,
                    shape = shape
                )
            }
            is Asset.Gradient -> {
                val gradientProvider = rememberBorderGradientProvider(baseProps, resolveAssets)
                val thicknessProvider = rememberBorderThicknessProvider(baseProps)
                val brush = gradientProvider.brush.value
                border(
                    width = thicknessProvider.value,
                    brush = brush,
                    shape = shape
                )
            }
            is Asset.Image -> {
                val colorProvider = rememberBorderColorProvider(baseProps, resolveAssets)
                val thicknessProvider = rememberBorderThicknessProvider(baseProps)
                border(
                    width = thicknessProvider.value,
                    color = colorProvider.value,
                    shape = shape
                )
            }
        }
    }
}

private fun Modifier.borderWithCrossFade(
    brush: CrossFadeGradientBrush,
    shape: Shape,
    thickness: Dp
): Modifier = composed {
    val startBorder = border(
        width = thickness,
        brush = brush.startBrush,
        shape = shape
    )
    
    val endBorder = border(
        width = thickness,
        brush = brush.endBrush,
        shape = shape
    )
    
    this
        .then(startBorder)
        .graphicsLayer {
            alpha = 1f - brush.crossFadeProgress
        }
        .then(endBorder)
        .graphicsLayer {
            alpha = brush.crossFadeProgress
        }
}

internal fun Modifier.sizeAndMarginsOrSkip(
    element: UIElement,
    boxProvider: BoxProvider,
): Modifier {
    val baseProps = element.baseProps
    val margins = baseProps.padding
    return this
        .sideDimensionOrSkip(baseProps.widthSpec, margins, boxProvider)
        .sideDimensionOrSkip(baseProps.heightSpec, margins, boxProvider)
        .marginsOrSkip(margins)
}

internal fun Modifier.sideDimensionOrSkip(
    sideDimension: DimSpec?, 
    margins: EdgeEntities?,
    boxProvider: BoxProvider,
): Modifier = composed {
    when (sideDimension) {
        null -> this
        is DimSpec.FillMax -> when (sideDimension.axis) {
            DimSpec.Axis.X -> this.fillMaxWidth()
            DimSpec.Axis.Y -> this.fillMaxHeight()
        }
        is DimSpec.Min -> when (val axis = sideDimension.axis) {
            DimSpec.Axis.X -> this.widthIn(
                min = sideDimension.value.toExactDp(axis) + margins.horizontalSumOrDefault,
                max = sideDimension.maxValue?.let { maxValue -> maxValue.toExactDp(axis) + margins.horizontalSumOrDefault }
                    ?.takeIf { it > 0.dp } ?: Dp.Unspecified,
            )
            DimSpec.Axis.Y -> this.heightIn(
                min = sideDimension.value.toExactDp(axis) + margins.verticalSumOrDefault,
                max = sideDimension.maxValue?.let { maxValue -> maxValue.toExactDp(axis) + margins.verticalSumOrDefault }
                    ?.takeIf { it > 0.dp } ?: Dp.Unspecified,
            )
        }
        is DimSpec.Specified -> when (val axis = sideDimension.axis) {
            DimSpec.Axis.X -> {
                val animatedWidth = boxProvider.width.value.takeIf { it.isSpecified }
                if (animatedWidth != null) {
                    this.width(animatedWidth + margins.horizontalSumOrDefault)
                } else {
                    this.width(sideDimension.value.toExactDp(axis) + margins.horizontalSumOrDefault)
                }
            }
            DimSpec.Axis.Y -> {
                val animatedHeight = boxProvider.height.value.takeIf { it.isSpecified }
                if (animatedHeight != null) {
                    this.height(animatedHeight + margins.verticalSumOrDefault)
                } else {
                    this.height(sideDimension.value.toExactDp(axis) + margins.verticalSumOrDefault)
                }
            }
        }
        is DimSpec.Shrink -> when (val axis = sideDimension.axis) {
            DimSpec.Axis.X -> this
                .widthIn(
                    min = (sideDimension.min.toExactDp(axis) + margins.horizontalSumOrDefault)
                        .takeIf { it > 0.dp } ?: Dp.Unspecified,
                    max = sideDimension.maxValue?.let { maxValue -> maxValue.toExactDp(axis) + margins.horizontalSumOrDefault }
                        ?.takeIf { it > 0.dp } ?: Dp.Unspecified,
                )
                .width(IntrinsicSize.Min)
            DimSpec.Axis.Y -> this
                .heightIn(
                    min = (sideDimension.min.toExactDp(axis) + margins.verticalSumOrDefault)
                        .takeIf { it > 0.dp } ?: Dp.Unspecified,
                    max = sideDimension.maxValue?.let { maxValue -> maxValue.toExactDp(axis) + margins.verticalSumOrDefault }
                        ?.takeIf { it > 0.dp } ?: Dp.Unspecified,
                )
                .height(IntrinsicSize.Min)
        }
    }
}

internal fun Modifier.marginsOrSkip(margins: EdgeEntities?): Modifier = composed {
    if (margins == null)
        return@composed this
    val (start, top, end, bottom) = margins
    val paddingValues = listOf(start, top, end, bottom).mapIndexed { i, dimUnit ->
        dimUnit.toExactDp(if (i % 2 == 0) DimSpec.Axis.X else DimSpec.Axis.Y)
    }.let { values ->
        PaddingValues(values[0], values[1], values[2], values[3])
    }
    this
        .padding(paddingValues)
}

internal fun Modifier.offsetOrSkip(offset: Offset?): Modifier = composed {
    if (offset == null || offset.consumed)
        return@composed this@composed
    offset(offset.x.toExactDp(DimSpec.Axis.X), offset.y.toExactDp(DimSpec.Axis.X))
}

internal fun Modifier.offsetOrSkip(offsetProvider: OffsetProvider): Modifier {
    if (!offsetProvider.active) return this
    return this.offset {
        IntOffset(
            offsetProvider.offset.value.x.dp.roundToPx(),
            offsetProvider.offset.value.y.dp.roundToPx()
        )
    }
}

private fun Modifier.backgroundWithCrossFade(
    crossFadeBrush: CrossFadeGradientBrush,
    shape: Shape
): Modifier = composed {
    val backgroundColor = if (isSystemInDarkTheme()) {
        androidx.compose.ui.graphics.Color(0xFF121212)
    } else {
        androidx.compose.ui.graphics.Color(0xFFF5F5F5)
    }
    this.then(
        Modifier.drawBehind {
            val startAlpha = 1f - crossFadeBrush.crossFadeProgress
            val endAlpha = crossFadeBrush.crossFadeProgress
            drawRect(
                color = backgroundColor,
                alpha = 1f
            )
            drawRect(
                brush = crossFadeBrush.startBrush,
                alpha = startAlpha
            )
            drawRect(
                brush = crossFadeBrush.endBrush,
                alpha = endAlpha
            )
        }
    )
}

internal fun Modifier.shadowOrSkip(
    shadow: ShadowProvider,
    shape: Shape,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    rotation: Rotation = Rotation.Default,
    scale: Scale = Scale.Default,
): Modifier {
    if (shadow.blurRadius.value == 0f && shadow.offset.value.x == 0f && shadow.offset.value.y == 0f) return this
    return this.shadow(
        shadow.color.value,
        shape,
        shadow.blurRadius.value.dp,
        shadow.offset.value,
        transformOrigin,
        rotation,
        scale,
    )
}

internal fun Modifier.shadow(
    color: androidx.compose.ui.graphics.Color,
    shape: Shape,
    blurRadius: Dp,
    offset: DpOffset,
    transformOrigin: TransformOrigin,
    rotation: Rotation,
    scale: Scale,
) = composed {
    val paint = remember { Paint() }
    val density = LocalDensity.current

    val blurPx = with(density) { blurRadius.toPx() }
    val offsetXPx = with(density) { offset.x.dp.toPx() }
    val offsetYPx = with(density) { offset.y.dp.toPx() }

    val maskFilter = remember(blurPx) {
        if (blurPx > 0f) BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) else null
    }

    drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)

        val pivotX = size.width * transformOrigin.pivotFractionX
        val pivotY = size.height * transformOrigin.pivotFractionY

        paint.asFrameworkPaint().apply {
            this.color = color.toArgb()
            this.maskFilter = maskFilter
        }

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetXPx, offsetYPx)
            canvas.translate(pivotX, pivotY)
            canvas.scale(scale.x, scale.y)
            canvas.rotate(rotation.degrees)
            canvas.translate(-pivotX, -pivotY)

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
}
