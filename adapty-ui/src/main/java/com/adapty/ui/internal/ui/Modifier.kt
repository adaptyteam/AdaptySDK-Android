@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.Shape
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
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorHorizontal
import com.adapty.ui.internal.ui.attributes.LocalOverflowAnchorVertical
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.hasAnyNegative
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.asDpOffset
import com.adapty.ui.internal.ui.attributes.asTransformOrigin
import com.adapty.ui.internal.ui.attributes.degreesIn
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
import com.adapty.ui.internal.ui.element.TextFieldElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.drawsOnlyDecorator
import androidx.compose.ui.input.pointer.pointerInput
import com.adapty.ui.internal.utils.LocalSafeAreaInsets
import com.adapty.ui.internal.utils.LocalScreenDimensions
import com.adapty.ui.internal.ui.element.BoxProvider
import com.adapty.ui.internal.ui.attributes.toExactDpInLayout
import androidx.compose.ui.graphics.Paint
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.store.Message
import androidx.compose.ui.graphics.toArgb
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.adapty.ui.internal.ui.attributes.DpOffset
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.element.BlurProvider
import com.adapty.ui.internal.ui.element.InnerShadowProvider
import com.adapty.ui.internal.ui.element.LocalActiveAnimations
import com.adapty.ui.internal.ui.element.activeAnimationsFor
import com.adapty.ui.internal.ui.element.hasVisualAnimations
import com.adapty.ui.internal.ui.element.pendingAppearAnimationsFor
import com.adapty.ui.internal.ui.element.OverlayContainerElement
import com.adapty.ui.internal.ui.element.ShadowProvider
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.ui.internal.utils.VisualValue

@InternalAdaptyApi
@Composable
public fun Modifier.fillWithBaseParams(element: UIElement): Modifier =
    fillWithBaseParams(element, backgroundAlpha = null)

@Composable
internal fun Modifier.fillWithBaseParams(
    element: UIElement,
    backgroundAlpha: State<Float>?,
): Modifier =
    fillWithBaseParams(
        element.baseProps,
        backgroundAlpha = backgroundAlpha,
        isOverlayContainer = element is OverlayContainerElement,
        handlesOwnFocus = element is TextFieldElement,
    ).blockTouchesBelowIfPainted(element)

@Composable
internal fun Modifier.blockTouchesBelowIfPainted(element: UIElement): Modifier =
    if (LocalBlocksTouchesBelow.current && !LocalInsideButton.current && element.paintsOwnPixels()) {
        this.pointerInput(Unit) {}
    } else {
        this
    }

@Composable
private fun UIElement.paintsOwnPixels(): Boolean {
    if (!drawsOnlyDecorator) return true
    val decorator = baseProps.shape ?: return false
    val fill = decorator.fill?.resolveAsset<Asset.Filling.Local>() ?: return false
    return fill.main !is Asset.Color || fill.cast<Asset.Color>().toComposeFill().color.alpha > 0f
}

@Composable
internal fun Modifier.fillWithBaseParams(
    baseProps: BaseProps,
    isOverlayContainer: Boolean = false,
    handlesOwnFocus: Boolean = false,
    backgroundAlpha: State<Float>? = null,
): Modifier {
    if (!baseProps.hasVisualAnimations()) {
        return fillWithBaseParamsStatic(baseProps, isOverlayContainer, handlesOwnFocus, backgroundAlpha)
    }
    val rotationProvider = rememberRotationProvider(baseProps)
    val scaleProvider = rememberScaleProvider(baseProps)
    val offsetProvider = rememberOffsetProvider(baseProps)
    val opacityProvider = LocalOpacityProvider.current ?: rememberOpacityProvider(baseProps)
    val boxProvider = rememberBoxProvider(baseProps)
    val shadowProvider = rememberShadowProvider(baseProps)
    val innerShadowProvider = rememberInnerShadowProvider(baseProps)
    val blurProvider = rememberBlurProvider(baseProps)

    val rotation = rotationProvider.rotation.value
    val scale = scaleProvider.scale.value
    val baseAlpha = opacityProvider.alpha.value
    val alpha = baseAlpha
    val anchorLayoutDirection = LocalLayoutDirection.current
    val rotationOrigin = rotation.anchor.asTransformOrigin(anchorLayoutDirection)
    val scaleOrigin = scale.anchor.asTransformOrigin(anchorLayoutDirection)

    val focusId = baseProps.focusId
    val focusRequester =
        if (focusId != null && !handlesOwnFocus) remember(focusId) { FocusRequester() } else null
    val focusCommand = LocalFocusCommand.current

    if (focusId != null && focusRequester != null && focusCommand?.focusId == focusId) {
        val dispatch = LocalDispatch.current
        LaunchedEffect(focusCommand) {
            runCatching { focusRequester.requestFocus() }
            dispatch(Message.FocusCommandConsumed)
        }
    }

    val backgroundShape = baseProps.shape?.type?.toComposeShape()

    var result = this
        .sizeAndMarginsOrSkip(baseProps, boxProvider)
        .let {
            if (rotation.anchor == scale.anchor && scale.x == scale.y)
                it.graphicsLayer(
                    rotationZ = rotation.degreesIn(anchorLayoutDirection),
                    transformOrigin = rotationOrigin,
                    scaleX = scale.x,
                    scaleY = scale.y,
                )
            else
                it
                    .graphicsLayer(
                        scaleX = scale.x,
                        scaleY = scale.y,
                        transformOrigin = scaleOrigin,
                    )
                    .graphicsLayer(
                        rotationZ = rotation.degreesIn(anchorLayoutDirection),
                        transformOrigin = rotationOrigin,
                    )
        }
        .offsetOrSkip(offsetProvider)
        .let { if (isOverlayContainer) it else it.blurOrSkip(blurProvider, shadowProvider.captureExpand) }
        .let {
            if (isOverlayContainer) it
            else it.shadowOrSkip(shadowProvider, backgroundShape ?: RectangleShape, alpha, rotation, scale)
        }
        .let { if (alpha != 1f) it.graphicsLayer(alpha = alpha) else it }
        .clipToShapeOrSkip(backgroundShape)
        .backgroundOrSkip(baseProps, innerShadowProvider, backgroundAlpha)

    if (focusRequester != null) {
        result = result.focusRequester(focusRequester).focusable()
    }

    return result
}

private val UnspecifiedDpState = androidx.compose.runtime.mutableStateOf(Dp.Unspecified)
private val StaticBoxProvider = BoxProvider(UnspecifiedDpState, UnspecifiedDpState)

@Composable
private fun Modifier.fillWithBaseParamsStatic(
    baseProps: BaseProps,
    isOverlayContainer: Boolean,
    handlesOwnFocus: Boolean,
    backgroundAlpha: State<Float>?,
): Modifier {
    val rotation = baseProps.rotation ?: Rotation.Default
    val scale = baseProps.scale ?: Scale.Default
    val alpha = LocalOpacityProvider.current?.alpha?.value ?: baseProps.opacity
    val anchorLayoutDirection = LocalLayoutDirection.current
    val rotationOrigin = rotation.anchor.asTransformOrigin(anchorLayoutDirection)
    val scaleOrigin = scale.anchor.asTransformOrigin(anchorLayoutDirection)

    val focusId = baseProps.focusId
    val focusRequester =
        if (focusId != null && !handlesOwnFocus) remember(focusId) { FocusRequester() } else null
    val focusCommand = LocalFocusCommand.current

    if (focusId != null && focusRequester != null && focusCommand?.focusId == focusId) {
        val dispatch = LocalDispatch.current
        LaunchedEffect(focusCommand) {
            runCatching { focusRequester.requestFocus() }
            dispatch(Message.FocusCommandConsumed)
        }
    }

    val backgroundShape = baseProps.shape?.type?.toComposeShape()

    val shadow = baseProps.shape?.shadow
    val shadowColor = shadow?.color?.resolveAsset<Asset.Filling.Local>()
        ?.castOrNull<Asset.Color>()?.toComposeFill()?.color
        ?: androidx.compose.ui.graphics.Color.Transparent
    val shadowBlurRadius = shadow?.blurRadius ?: 0f
    val shadowOffset = (shadow?.offset ?: Offset.Default).asDpOffset()
    val staticCaptureExpand =
        if (shadowColor.alpha == 0f) 0.dp
        else (shadowBlurRadius + maxOf(kotlin.math.abs(shadowOffset.x), kotlin.math.abs(shadowOffset.y))).dp

    val staticOffset = baseProps.offset?.asDpOffset()
    val blurRadius = baseProps.shape?.blurRadius ?: 0f

    var result = this
        .sizeAndMarginsOrSkip(baseProps, StaticBoxProvider)
        .let {
            if (rotation.anchor == scale.anchor && scale.x == scale.y)
                it.graphicsLayer(
                    rotationZ = rotation.degreesIn(anchorLayoutDirection),
                    transformOrigin = rotationOrigin,
                    scaleX = scale.x,
                    scaleY = scale.y,
                )
            else
                it
                    .graphicsLayer(
                        scaleX = scale.x,
                        scaleY = scale.y,
                        transformOrigin = scaleOrigin,
                    )
                    .graphicsLayer(
                        rotationZ = rotation.degreesIn(anchorLayoutDirection),
                        transformOrigin = rotationOrigin,
                    )
        }
        .let {
            if (staticOffset == null || (staticOffset.x == 0f && staticOffset.y == 0f)) it
            else it.then(StaticOffsetElement(staticOffset.x.dp, staticOffset.y.dp))
        }
        .let {
            if (isOverlayContainer || blurRadius <= 0f) it
            else it.blur(blurRadius.dp, staticCaptureExpand)
        }
        .let {
            if (isOverlayContainer) it
            else {
                val color =
                    if (alpha < 1f) shadowColor.copy(alpha = shadowColor.alpha * alpha) else shadowColor
                if (color.alpha == 0f) it
                else it.shadow(color, backgroundShape ?: RectangleShape, shadowBlurRadius.dp, shadowOffset, rotation, scale)
            }
        }
        .let { if (alpha != 1f) it.graphicsLayer(alpha = alpha) else it }
        .clipToShapeOrSkip(backgroundShape)
        .backgroundOrSkipStatic(baseProps, backgroundAlpha)

    if (focusRequester != null) {
        result = result.focusRequester(focusRequester).focusable()
    }

    return result
}

@Composable
private fun Modifier.backgroundOrSkipStatic(
    baseProps: BaseProps,
    backgroundAlpha: State<Float>?,
): Modifier {
    val decorator = baseProps.shape ?: return this
    var modifier = this
    val backgroundShape = decorator.type.toComposeShape()
    if (decorator.fill != null) {
        val background = decorator.fill.resolveAsset<Asset.Filling.Local>()
        if (background != null)
            modifier = modifier.background(background, backgroundShape, backgroundAlpha)
        val innerShadow = decorator.innerShadow
        if (innerShadow != null &&
            (background?.main is Asset.Color || background?.main is Asset.Gradient)) {
            val innerShadowColor = innerShadow.color?.resolveAsset<Asset.Filling.Local>()
                ?.castOrNull<Asset.Color>()?.toComposeFill()?.color
                ?: androidx.compose.ui.graphics.Color.Transparent
            if (innerShadowColor.alpha != 0f) {
                modifier = modifier.innerShadow(
                    innerShadowColor,
                    backgroundShape,
                    (innerShadow.blurRadius ?: 0f).dp,
                    (innerShadow.offset ?: Offset.Default).asDpOffset(),
                    backgroundAlpha,
                )
            }
        }
    }

    if (decorator.border != null) {
        val border = decorator.border.color.resolveAsset<Asset.Filling.Local>()
        val borderShape = decorator.border.shapeType.toComposeShape()
        val thickness = decorator.border.thickness.dp
        when (border?.main) {
            is Asset.Color -> {
                modifier = modifier.border(
                    thickness,
                    border.cast<Asset.Color>().toComposeFill().color,
                    borderShape,
                )
            }
            is Asset.Gradient -> {
                modifier = modifier.border(
                    thickness,
                    border.cast<Asset.Gradient>().toComposeFill().shader,
                    borderShape,
                )
            }
            is Asset.Image -> {
                modifier = modifier.border(
                    thickness,
                    androidx.compose.ui.graphics.Color.Transparent,
                    borderShape,
                )
            }
            else -> Unit
        }
    }
    return modifier
}

@Composable
internal fun Modifier.backgroundOrSkip(
    decorator: com.adapty.ui.internal.ui.attributes.Shape?,
): Modifier {
    val actualDecorator = decorator ?: return this
    var modifier = this
    val backgroundShape = actualDecorator.type.toComposeShape()
    modifier = modifier.clipToShape(backgroundShape)
    if (actualDecorator.fill != null) {
        val background = actualDecorator.fill.resolveAsset<Asset.Filling.Local>()
        if (background != null)
            modifier = modifier.background(background, backgroundShape)
    }

    if (actualDecorator.border != null) {
        val border = actualDecorator.border.color.resolveAsset<Asset.Filling.Local>()
        when (border?.main) {
            is Asset.Color -> {
                modifier = modifier.border(
                    actualDecorator.border.thickness.dp,
                    border.cast<Asset.Color>().toComposeFill().color,
                    actualDecorator.border.shapeType.toComposeShape(),
                )
            }
            is Asset.Gradient -> {
                modifier = modifier.border(
                    actualDecorator.border.thickness.dp,
                    border.cast<Asset.Gradient>().toComposeFill().shader,
                    actualDecorator.border.shapeType.toComposeShape(),
                )
            }
            else -> Unit
        }
    }
    return modifier
}

@Composable
internal fun Modifier.backgroundOrSkip(
    baseProps: BaseProps,
    innerShadowProvider: InnerShadowProvider? = null,
    backgroundAlpha: State<Float>? = null,
): Modifier {
    val decorator = baseProps.shape ?: return this
    var modifier = this
    val backgroundShape = decorator.type.toComposeShape()
    if (decorator.fill != null) {
        val background = decorator.fill.resolveAsset<Asset.Filling.Local>()
        if (background != null)
            modifier = modifier.background(background, backgroundShape, baseProps, backgroundAlpha)
        if (innerShadowProvider != null &&
            (background?.main is Asset.Color || background?.main is Asset.Gradient)) {
            modifier = modifier.innerShadowOrSkip(innerShadowProvider, backgroundShape, backgroundAlpha)
        }
    }

    if (decorator.border != null) {
        val border = decorator.border.color.resolveAsset<Asset.Filling.Local>()
        if (border != null) {
            modifier = modifier.border(border, decorator.border.shapeType.toComposeShape(), baseProps)
        }
    }
    return modifier
}

private data class ClipToShapeElement(
    private val shape: Shape,
) : ModifierNodeElement<ClipToShapeNode>() {
    override fun create() = ClipToShapeNode(shape)
    override fun update(node: ClipToShapeNode) {
        node.shape = shape
    }
}

private class ClipToShapeNode(
    var shape: Shape,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    
    private var useCustomClip: Boolean? = null
    private var cachedShape: Shape? = null
    
    override fun ContentDrawScope.draw() {
        val density = currentValueOf(LocalDensity)
        val layoutDirection = currentValueOf(LocalLayoutDirection)
        
        if (cachedShape != shape) {
            cachedShape = shape
            useCustomClip = shape.createOutline(Size(100f, 100f), layoutDirection, density) is Outline.Generic
        }
        
        if (useCustomClip == true) {
            val outline = shape.createOutline(size, layoutDirection, density)
            val path = (outline as Outline.Generic).path
            
            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipPath(path)
            drawContent()
            canvas.restore()
        } else {
            drawContent()
        }
    }
}

private fun Modifier.clipToShape(shape: Shape): Modifier {
    val density = Density(1f)
    val layoutDirection = LayoutDirection.Ltr
    val useCustomClip = shape.createOutline(Size(100f, 100f), layoutDirection, density) is Outline.Generic
    return if (useCustomClip) {
        this.then(ClipToShapeElement(shape))
    } else {
        this.clip(shape)
    }
}

private fun Modifier.clipToShapeOrSkip(shape: Shape?): Modifier {
    shape ?: return this
    return clipToShape(shape)
}

@Composable
private fun Modifier.background(
    background: Asset.Composite<Asset.Filling.Local>,
    shape: Shape,
    baseProps: BaseProps,
    backgroundAlpha: State<Float>?,
): Modifier {
    val backgroundAnimations = if (!baseProps.hasVisualAnimations()) null
    else activeAnimationsFor(Animation.Role.Background)
        ?: baseProps.pendingAppearAnimationsFor(Animation.Role.Background)

    val hasMixedAssetTypeAnimations = backgroundAnimations?.let { animations ->
        animations.any { anim ->
            val startVisualValue = anim.start as? VisualValue
            val endVisualValue = anim.end as? VisualValue
            if (startVisualValue != null && endVisualValue != null) {
                val startAsset = startVisualValue.resolveAsset<Asset.Filling.Local>()
                val endAsset = endVisualValue.resolveAsset<Asset.Filling.Local>()
                val startIsColor = startAsset?.castOrNull<Asset.Color>() != null
                val startIsGradient = startAsset?.castOrNull<Asset.Gradient>() != null
                val endIsColor = endAsset?.castOrNull<Asset.Color>() != null
                val endIsGradient = endAsset?.castOrNull<Asset.Gradient>() != null
                
                (startIsColor && endIsGradient) || (startIsGradient && endIsColor)
            } else false
        }
    } ?: false

    return if (hasMixedAssetTypeAnimations) {
        val gradientProvider = rememberGradientProvider(baseProps)
        val brush = gradientProvider.brush.value
        
        if (brush is CrossFadeGradientBrush) {
            this.backgroundWithCrossFade(brush, shape)
        } else {
            this.gatedFill(brush = brush, shape = shape, alpha = backgroundAlpha)
        }
    } else {
        when (background.main) {
            is Asset.Color -> {
                val colorProvider = rememberColorProvider(baseProps)
                this.gatedFill(color = colorProvider.value, shape = shape, alpha = backgroundAlpha)
            }
            is Asset.Gradient -> {
                val gradientProvider = rememberGradientProvider(baseProps)
                val brush = gradientProvider.brush.value
                this.gatedFill(brush = brush, shape = shape, alpha = backgroundAlpha)
            }
            is Asset.Image -> {
                val context = LocalContext.current
                this.then(ImageBackgroundElement(background, shape, context, backgroundAlpha))
            }
        }
    }
}

private fun Modifier.gatedFill(
    color: androidx.compose.ui.graphics.Color? = null,
    brush: Brush? = null,
    shape: Shape,
    alpha: State<Float>?,
): Modifier {
    if (alpha == null) {
        return if (color != null) this.background(color = color, shape = shape)
        else this.background(brush = brush!!, shape = shape)
    }
    return this.drawBehind {
        val a = alpha.value
        if (a <= 0f) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        if (color != null) drawOutline(outline, color = color, alpha = a)
        else drawOutline(outline, brush = brush!!, alpha = a)
    }
}

private data class ImageBackgroundElement(
    private val background: Asset.Composite<Asset.Filling.Local>,
    private val shape: Shape,
    private val context: android.content.Context,
    private val alpha: State<Float>?,
) : ModifierNodeElement<ImageBackgroundNode>() {
    override fun create() = ImageBackgroundNode(background, shape, context, alpha)
    override fun update(node: ImageBackgroundNode) {
        node.background = background
        node.shape = shape
        node.context = context
        node.alpha = alpha
    }
}

private class ImageBackgroundNode(
    var background: Asset.Composite<Asset.Filling.Local>,
    var shape: Shape,
    var context: android.content.Context,
    var alpha: State<Float>?,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val gatedPaint = android.graphics.Paint()

    override fun ContentDrawScope.draw() {
        val gateAlpha = alpha?.value ?: 1f
        if (gateAlpha <= 0f) {
            drawContent()
            return
        }
        val fill = background.cast<Asset.Image>().toComposeFill(context, size)
        if (fill != null) {
            val density = currentValueOf(LocalDensity)
            drawIntoCanvas { canvas ->
                canvas.save()
                if (shape != RectangleShape) {
                    val path = Path()
                    shape.createOutline(size, layoutDirection, density).let { outline ->
                        when (outline) {
                            is Outline.Rectangle -> path.addRect(outline.rect)
                            is Outline.Rounded -> path.addRoundRect(outline.roundRect)
                            is Outline.Generic -> path.addPath(outline.path)
                        }
                    }
                    canvas.clipPath(path)
                }
                val paint = if (gateAlpha >= 1f) fill.paint
                else gatedPaint.apply {
                    set(fill.paint)
                    alpha = (alpha * gateAlpha).toInt()
                }
                canvas.nativeCanvas.drawBitmap(fill.image, fill.matrix, paint)
                canvas.restore()
            }
        }
        drawContent()
    }
}

@Composable
private fun Modifier.background(
    background: Asset.Composite<Asset.Filling.Local>,
    shape: Shape,
    backgroundAlpha: State<Float>? = null,
): Modifier = when (background.main) {
    is Asset.Color -> {
        val fill = background.cast<Asset.Color>().toComposeFill()
        this.gatedFill(color = fill.color, shape = shape, alpha = backgroundAlpha)
    }
    is Asset.Gradient -> {
        val fill = background.cast<Asset.Gradient>().toComposeFill()
        this.gatedFill(brush = fill.shader, shape = shape, alpha = backgroundAlpha)
    }
    is Asset.Image -> {
        val context = LocalContext.current
        this.then(ImageBackgroundElement(background, shape, context, backgroundAlpha))
    }
}

@Composable
private fun Modifier.border(
    border: Asset.Composite<Asset.Filling.Local>,
    shape: Shape,
    baseProps: BaseProps,
): Modifier {
    val borderAnimations = if (!baseProps.hasVisualAnimations()) null
    else activeAnimationsFor(Animation.Role.Border)
        ?: baseProps.pendingAppearAnimationsFor(Animation.Role.Border)

    val hasMixedAssetTypeAnimations = borderAnimations?.let { animations ->
        val assets = resolveAssets()
        animations.any { anim ->
            val startBorder = anim.start as? Border
            val endBorder = anim.end as? Border
            if (startBorder?.color != null && endBorder?.color != null) {
                val startAsset = startBorder.color.resolveAsset<Asset.Filling.Local>()
                val endAsset = endBorder.color.resolveAsset<Asset.Filling.Local>()
                val startIsColor = startAsset?.castOrNull<Asset.Color>() != null
                val startIsGradient = startAsset?.castOrNull<Asset.Gradient>() != null
                val endIsColor = endAsset?.castOrNull<Asset.Color>() != null
                val endIsGradient = endAsset?.castOrNull<Asset.Gradient>() != null
                
                (startIsColor && endIsGradient) || (startIsGradient && endIsColor)
            } else false
        }
    } ?: false

    return if (hasMixedAssetTypeAnimations) {
        val gradientProvider = rememberBorderGradientProvider(baseProps)
        val thicknessProvider = rememberBorderThicknessProvider(baseProps)
        val brush = gradientProvider.brush.value
        
        if (brush is CrossFadeGradientBrush) {
            this.borderWithCrossFade(brush, shape, thicknessProvider.value)
        } else {
            this.border(
                width = thicknessProvider.value,
                brush = brush,
                shape = shape
            )
        }
    } else {
        when (border.main) {
            is Asset.Color -> {
                val colorProvider = rememberBorderColorProvider(baseProps)
                val thicknessProvider = rememberBorderThicknessProvider(baseProps)
                this.border(
                    width = thicknessProvider.value,
                    color = colorProvider.value,
                    shape = shape
                )
            }
            is Asset.Gradient -> {
                val gradientProvider = rememberBorderGradientProvider(baseProps)
                val thicknessProvider = rememberBorderThicknessProvider(baseProps)
                val brush = gradientProvider.brush.value
                this.border(
                    width = thicknessProvider.value,
                    brush = brush,
                    shape = shape
                )
            }
            is Asset.Image -> {
                val colorProvider = rememberBorderColorProvider(baseProps)
                val thicknessProvider = rememberBorderThicknessProvider(baseProps)
                this.border(
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
): Modifier {
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
    
    return this
        .then(startBorder)
        .graphicsLayer {
            alpha = 1f - brush.crossFadeProgress
        }
        .then(endBorder)
        .graphicsLayer {
            alpha = brush.crossFadeProgress
        }
}

@Composable
internal fun Modifier.sizeAndMarginsOrSkip(
    baseProps: BaseProps,
    boxProvider: BoxProvider,
): Modifier {
    return this
        .sideDimensionOrSkip(baseProps.widthSpec, baseProps.padding, boxProvider)
        .sideDimensionOrSkip(baseProps.heightSpec, baseProps.padding, boxProvider)
        .marginsOrSkip(baseProps.padding)
}

@Composable
internal fun Modifier.allowVerticalOverflow(): Modifier {
    val anchor = LocalOverflowAnchorVertical.current
    return Modifier
        .wrapContentHeight(anchor, unbounded = true)
        .then(this)
}

@Composable
internal fun Modifier.allowHorizontalOverflow(): Modifier {
    val anchor = LocalOverflowAnchorHorizontal.current
    return Modifier
        .wrapContentWidth(anchor, unbounded = true)
        .then(this)
}

@Suppress("DEPRECATION")
internal object ProposeMinHeightWhenShorter : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val minHeight = constraints.minHeight
        val proposeMin = !constraints.hasBoundedHeight && minHeight > 0 && try {
            NaturalHeightQuery.run { measurable.maxIntrinsicHeight(constraints.maxWidth) } < minHeight
        } catch (e: IllegalStateException) {
            false
        }
        val placeable =
            if (proposeMin)
                measurable.measure(constraints.copy(maxHeight = minHeight))
            else
                measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}

internal fun Modifier.fillBoundedHeightOrIntrinsic(): Modifier = this.layout { measurable, constraints ->
    val targetHeight = if (constraints.hasBoundedHeight) {
        constraints.maxHeight
    } else {
        try {
            measurable.minIntrinsicHeight(constraints.maxWidth)
        } catch (e: IllegalStateException) {
            null
        }
    }
    val placeable =
        if (targetHeight != null)
            measurable.measure(constraints.copy(minHeight = targetHeight, maxHeight = targetHeight))
        else
            measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

internal fun Modifier.intrinsicHeightOrHug(): Modifier = this.layout { measurable, constraints ->
    val targetHeight = try {
        measurable.minIntrinsicHeight(constraints.maxWidth)
    } catch (e: IllegalStateException) {
        null
    }
    val placeable =
        if (targetHeight != null)
            measurable.measure(constraints.copy(minHeight = targetHeight, maxHeight = targetHeight))
        else
            measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

@Suppress("DEPRECATION")
internal data class FixedWidthHeightIntrinsics(private val width: Dp) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(minOf(width, this@FixedWidthHeightIntrinsics.width.roundToPx()))

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(minOf(width, this@FixedWidthHeightIntrinsics.width.roundToPx()))

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}

@Suppress("DEPRECATION")
internal data class FixedHeightWidthIntrinsics(private val height: Dp) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(minOf(height, this@FixedHeightWidthIntrinsics.height.roundToPx()))

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(minOf(height, this@FixedHeightWidthIntrinsics.height.roundToPx()))

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)
}

@Composable
internal fun Modifier.sideDimensionOrSkip(
    sideDimension: DimSpec?, 
    margins: EdgeEntities?,
    boxProvider: BoxProvider,
): Modifier = when (sideDimension) {
    null -> this
    is DimSpec.FillMax -> when (sideDimension.axis) {
        DimSpec.Axis.X -> this.fillMaxWidth()
        DimSpec.Axis.Y -> this.fillMaxHeight()
    }
    is DimSpec.Min -> when (val axis = sideDimension.axis) {
        DimSpec.Axis.X -> this.widthIn(
            min = (sideDimension.value.toExactDp(axis) + margins.horizontalSumOrDefault).coerceAtLeast(0.dp),
            max = sideDimension.maxValue?.let { maxValue -> maxValue.toExactDp(axis) + margins.horizontalSumOrDefault }
                ?.takeIf { it > 0.dp } ?: Dp.Unspecified,
        )
        DimSpec.Axis.Y -> this.heightIn(
            min = (sideDimension.value.toExactDp(axis) + margins.verticalSumOrDefault).coerceAtLeast(0.dp),
            max = sideDimension.maxValue?.let { maxValue -> maxValue.toExactDp(axis) + margins.verticalSumOrDefault }
                ?.takeIf { it > 0.dp } ?: Dp.Unspecified,
        ).then(
            if (sideDimension.value is DimUnit.ScreenFraction) ProposeMinHeightWhenShorter
            else Modifier
        )
    }
    is DimSpec.Specified -> when (val axis = sideDimension.axis) {
        DimSpec.Axis.X -> {
            val animatedWidth = boxProvider.width.value.takeIf { it.isSpecified }
            val width = if (animatedWidth != null) {
                (animatedWidth + margins.horizontalSumOrDefault).coerceAtLeast(0.dp)
            } else {
                (sideDimension.value.toExactDp(axis) + margins.horizontalSumOrDefault).coerceAtLeast(0.dp)
            }
            this.width(width).then(FixedWidthHeightIntrinsics(width))
        }
        DimSpec.Axis.Y -> {
            val animatedHeight = boxProvider.height.value.takeIf { it.isSpecified }
            val height = if (animatedHeight != null) {
                (animatedHeight + margins.verticalSumOrDefault).coerceAtLeast(0.dp)
            } else {
                (sideDimension.value.toExactDp(axis) + margins.verticalSumOrDefault).coerceAtLeast(0.dp)
            }
            this.height(height).then(FixedHeightWidthIntrinsics(height))
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
            .width(IntrinsicSize.Max)
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

@Composable
internal fun Modifier.marginsOrSkip(margins: EdgeEntities?): Modifier {
    if (margins == null) return this
    val (start, top, end, bottom) = margins
    val startDp = start.toExactDp(DimSpec.Axis.X)
    val topDp = top.toExactDp(DimSpec.Axis.Y)
    val endDp = end.toExactDp(DimSpec.Axis.X)
    val bottomDp = bottom.toExactDp(DimSpec.Axis.Y)
    return if (margins.hasAnyNegative) {
        this.paddingAllowNegative(startDp, topDp, endDp, bottomDp)
    } else {
        this.padding(PaddingValues(startDp, topDp, endDp, bottomDp))
    }
}

@Composable
internal fun Modifier.negativePaddingInset(margins: EdgeEntities): Modifier {
    val (start, top, end, bottom) = margins
    return this.paddingAllowNegative(
        start.toExactDp(DimSpec.Axis.X).coerceAtMost(0.dp),
        top.toExactDp(DimSpec.Axis.Y).coerceAtMost(0.dp),
        end.toExactDp(DimSpec.Axis.X).coerceAtMost(0.dp),
        bottom.toExactDp(DimSpec.Axis.Y).coerceAtMost(0.dp),
    )
}

private fun Modifier.paddingAllowNegative(
    start: Dp,
    top: Dp,
    end: Dp,
    bottom: Dp,
): Modifier = this.layout { measurable, constraints ->
    val l = start.roundToPx()
    val t = top.roundToPx()
    val r = end.roundToPx()
    val b = bottom.roundToPx()
    val hSum = l + r
    val vSum = t + b
    val placeable = measurable.measure(constraints.offset(-hSum, -vSum))
    val width = constraints.constrainWidth(placeable.width + hSum)
    val height = constraints.constrainHeight(placeable.height + vSum)
    layout(width, height) {
        placeable.placeRelative(l, t)
    }
}

internal fun Modifier.fitImageWithinBounds(ratio: Float, constraints: Constraints): Modifier =
    if (constraints.hasBoundedHeight && constraints.maxWidth / ratio > constraints.maxHeight)
        fillMaxHeight().aspectRatio(ratio)
    else
        fillMaxWidth().aspectRatio(ratio)

@Composable
internal fun Modifier.offsetOrSkip(offset: Offset?): Modifier {
    if (offset == null || offset.consumed) return this
    
    val safeAreaInsets = LocalSafeAreaInsets.current
    val screenDimensions = LocalScreenDimensions.current
    val layoutDirection = LocalLayoutDirection.current
    
    val offsetX = offset.x.toExactDpInLayout(DimSpec.Axis.X, safeAreaInsets, screenDimensions, layoutDirection)
    val offsetY = offset.y.toExactDpInLayout(DimSpec.Axis.Y, safeAreaInsets, screenDimensions, layoutDirection)
    
    return this.then(StaticOffsetElement(offsetX, offsetY))
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

private data class StaticOffsetElement(
    private val offsetX: Dp,
    private val offsetY: Dp,
) : ModifierNodeElement<StaticOffsetNode>() {
    override fun create() = StaticOffsetNode(offsetX, offsetY)
    override fun update(node: StaticOffsetNode) {
        node.offsetX = offsetX
        node.offsetY = offsetY
    }
}

private class StaticOffsetNode(
    var offsetX: Dp,
    var offsetY: Dp,
) : Modifier.Node(), LayoutModifierNode {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val offsetXPx = offsetX.roundToPx()
        val offsetYPx = offsetY.roundToPx()
        
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(offsetXPx, offsetYPx)
        }
    }
}

private data class BackgroundWithCrossFadeElement(
    private val crossFadeBrush: CrossFadeGradientBrush,
    private val shape: Shape,
    private val isDarkTheme: Boolean,
) : ModifierNodeElement<BackgroundWithCrossFadeNode>() {
    override fun create() = BackgroundWithCrossFadeNode(crossFadeBrush, shape, isDarkTheme)
    override fun update(node: BackgroundWithCrossFadeNode) {
        node.crossFadeBrush = crossFadeBrush
        node.shape = shape
        node.isDarkTheme = isDarkTheme
    }
}

private class BackgroundWithCrossFadeNode(
    var crossFadeBrush: CrossFadeGradientBrush,
    var shape: Shape,
    var isDarkTheme: Boolean,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        val backgroundColor = if (isDarkTheme) {
            androidx.compose.ui.graphics.Color(0xFF121212)
        } else {
            androidx.compose.ui.graphics.Color(0xFFF5F5F5)
        }
        
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
        
        drawContent()
    }
}

@Composable
private fun Modifier.backgroundWithCrossFade(
    crossFadeBrush: CrossFadeGradientBrush,
    shape: Shape
): Modifier {
    val isDarkTheme = isSystemInDarkTheme()
    return this.then(BackgroundWithCrossFadeElement(crossFadeBrush, shape, isDarkTheme))
}

internal fun Modifier.shadowOrSkip(
    shadow: ShadowProvider,
    shape: Shape,
    contentAlpha: Float = 1f,
    rotation: Rotation = Rotation.Default,
    scale: Scale = Scale.Default,
): Modifier {
    val color = shadow.color.value.let {
        if (contentAlpha < 1f) it.copy(alpha = it.alpha * contentAlpha) else it
    }
    if (color.alpha == 0f) return this
    return this.shadow(
        color,
        shape,
        shadow.blurRadius.value.dp,
        shadow.offset.value,
        rotation,
        scale,
    )
}

private data class ShadowElement(
    private val color: androidx.compose.ui.graphics.Color,
    private val shape: Shape,
    private val blurRadius: Dp,
    private val offset: DpOffset,
    private val rotation: Rotation,
    private val scale: Scale,
) : ModifierNodeElement<ShadowNode>() {
    override fun create() = ShadowNode(color, shape, blurRadius, offset, rotation, scale)
    override fun update(node: ShadowNode) {
        node.color = color
        node.shape = shape
        node.blurRadius = blurRadius
        node.offset = offset
        node.rotation = rotation
        node.scale = scale
    }
}

private class ShadowNode(
    var color: androidx.compose.ui.graphics.Color,
    var shape: Shape,
    var blurRadius: Dp,
    var offset: DpOffset,
    var rotation: Rotation,
    var scale: Scale,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val paint = Paint()
    private var cachedBlurPx: Float = Float.NaN
    private var cachedMaskFilter: BlurMaskFilter? = null

    override fun ContentDrawScope.draw() {
        val density = currentValueOf(LocalDensity)

        val blurPx = with(density) { blurRadius.toPx() }
        val offsetXPx = with(density) { offset.x.dp.toPx() }
        val offsetYPx = with(density) { offset.y.dp.toPx() }

        val sx = if (scale.x != 0f) scale.x else Float.MIN_VALUE
        val sy = if (scale.y != 0f) scale.y else Float.MIN_VALUE
        val angle = Math.toRadians(-rotation.degrees.toDouble())
        val cosA = kotlin.math.cos(angle)
        val sinA = kotlin.math.sin(angle)
        val ux = offsetXPx / sx
        val uy = offsetYPx / sy
        val compOffsetX = (ux * cosA - uy * sinA).toFloat()
        val compOffsetY = (ux * sinA + uy * cosA).toFloat()
        val meanScale = kotlin.math.sqrt((sx * sy).coerceAtLeast(Float.MIN_VALUE))
        val effectiveBlurPx = blurPx / meanScale

        if (cachedBlurPx != effectiveBlurPx) {
            cachedBlurPx = effectiveBlurPx
            cachedMaskFilter = if (effectiveBlurPx > 0f) BlurMaskFilter(effectiveBlurPx, BlurMaskFilter.Blur.NORMAL) else null
        }

        val outline = shape.createOutline(size, layoutDirection, density)

        paint.asFrameworkPaint().apply {
            this.color = this@ShadowNode.color.toArgb()
            this.maskFilter = cachedMaskFilter
        }

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(compOffsetX, compOffsetY)

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

        drawContent()
    }
}

internal fun Modifier.shadow(
    color: androidx.compose.ui.graphics.Color,
    shape: Shape,
    blurRadius: Dp,
    offset: DpOffset,
    rotation: Rotation = Rotation.Default,
    scale: Scale = Scale.Default,
): Modifier = this.then(ShadowElement(color, shape, blurRadius, offset, rotation, scale))

internal fun Modifier.innerShadowOrSkip(
    innerShadow: InnerShadowProvider,
    shape: Shape,
    gateAlpha: State<Float>? = null,
): Modifier {
    val color = innerShadow.color.value
    if (color.alpha == 0f) return this
    val blurRadius = innerShadow.blurRadius.value
    val offset = innerShadow.offset.value
    return this.innerShadow(color, shape, blurRadius.dp, offset, gateAlpha)
}

private data class InnerShadowElement(
    private val color: androidx.compose.ui.graphics.Color,
    private val shape: Shape,
    private val blurRadius: Dp,
    private val offset: DpOffset,
    private val gateAlpha: State<Float>?,
) : ModifierNodeElement<InnerShadowNode>() {
    override fun create() = InnerShadowNode(color, shape, blurRadius, offset, gateAlpha)
    override fun update(node: InnerShadowNode) {
        node.color = color
        node.shape = shape
        node.blurRadius = blurRadius
        node.offset = offset
        node.gateAlpha = gateAlpha
    }
}

private class InnerShadowNode(
    var color: androidx.compose.ui.graphics.Color,
    var shape: Shape,
    var blurRadius: Dp,
    var offset: DpOffset,
    var gateAlpha: State<Float>?,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val shadowPaint = android.graphics.Paint()
    private val gateLayerPaint = android.graphics.Paint()
    private val cutoutPaint = android.graphics.Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    private val clipPaint = android.graphics.Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val outlinePath = android.graphics.Path()

    private var cachedBlurPx: Float = Float.NaN
    private var cachedMaskFilter: BlurMaskFilter? = null

    override fun ContentDrawScope.draw() {
        drawContent()

        val w = size.width.toInt()
        val h = size.height.toInt()
        if (w <= 0 || h <= 0) return
        val gate = gateAlpha?.value ?: 1f
        if (gate <= 0f) return

        val density = currentValueOf(LocalDensity)

        val blurPx = with(density) { blurRadius.toPx() }
        val offsetXPx = with(density) { offset.x.dp.toPx() }
        val offsetYPx = with(density) { offset.y.dp.toPx() }

        if (cachedBlurPx != blurPx) {
            cachedBlurPx = blurPx
            cachedMaskFilter = if (blurPx > 0f) BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL) else null
        }

        shadowPaint.color = this@InnerShadowNode.color.toArgb()
        cutoutPaint.maskFilter = cachedMaskFilter

        val outline = shape.createOutline(size, layoutDirection, density)

        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.saveLayer(null, if (gate >= 1f) null else gateLayerPaint.apply { alpha = (255 * gate).toInt() })

            nc.drawPaint(shadowPaint)

            nc.save()
            nc.translate(offsetXPx, offsetYPx)
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
}

internal fun Modifier.innerShadow(
    color: androidx.compose.ui.graphics.Color,
    shape: Shape,
    blurRadius: Dp,
    offset: DpOffset,
    gateAlpha: State<Float>? = null,
): Modifier = this.then(InnerShadowElement(color, shape, blurRadius, offset, gateAlpha))

internal fun Modifier.blurOrSkip(
    blurProvider: BlurProvider,
    captureExpand: Dp = 0.dp,
): Modifier {
    val radius = blurProvider.radius.value
    if (radius <= 0f) return this
    return this.blur(radius.dp, captureExpand)
}

internal val ShadowProvider.captureExpand: Dp
    get() {
        if (color.value.alpha == 0f) return 0.dp
        val o = offset.value
        return (blurRadius.value + maxOf(kotlin.math.abs(o.x), kotlin.math.abs(o.y))).dp
    }

internal fun Modifier.blur(blurRadius: Dp, captureExpand: Dp = 0.dp): Modifier =
    this.then(BlurElement(blurRadius, captureExpand))

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

    private var cachedBitmap: android.graphics.Bitmap? = null
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
