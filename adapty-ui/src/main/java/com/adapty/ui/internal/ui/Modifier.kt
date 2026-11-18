@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.AnimationTarget
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.target
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
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.attributes.verticalSumOrDefault
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.Border
import com.adapty.ui.internal.ui.attributes.BlendMode
import com.adapty.ui.internal.ui.attributes.Decorator
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.CustomElement
import com.adapty.ui.internal.ui.element.OffsetProvider
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.TextFieldElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.drawsOnlyDecorator
import androidx.compose.ui.input.pointer.PointerEventPass
import com.adapty.ui.internal.ui.element.hasOpacityAnimation
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
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.element.BlurProvider
import com.adapty.ui.internal.ui.element.LocalActiveAnimations
import com.adapty.ui.internal.ui.element.activeAnimationsFor
import com.adapty.ui.internal.ui.element.hasVisualAnimations
import com.adapty.ui.internal.ui.element.pendingAppearAnimationsFor
import com.adapty.ui.internal.ui.element.OverlayContainerElement
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
        handlesOwnFocus = element is TextFieldElement || element is CustomElement,
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
    val decorator = baseProps.decorator ?: return false
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

    val decorator = if (isOverlayContainer) null else baseProps.decoratorOrShadowHost()
    val resolvedDecorator = decorator?.resolve(rememberDecoratorAnimations(baseProps, decorator))
    val overflow = rememberOverflowExtent(resolvedDecorator)

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
        .let {
            if (isOverlayContainer) it
            else it.elementBlendOrSkip(baseProps.blend, resolvedDecorator, blurProvider.radius.value.dp, overflow)
        }
        .let {
            if (isOverlayContainer) it
            else it.blurOrSkip(blurProvider, resolvedDecorator?.shadowCaptureExpand ?: 0.dp)
        }
        .let { if (alpha != 1f && resolvedDecorator == null) it.graphicsLayer(alpha = alpha) else it }
        .let {
            val resolved = resolvedDecorator ?: return@let it
            it.decorator(
                resolved,
                backgroundAlpha,
                alpha = alpha,
                rotation = rotation,
                scale = scale,
                overflow = overflow,
            )
        }
        .overflowExtent(
            overflow,
            tracks = baseProps.tracksOverflow(resolvedDecorator, alpha),
            reach = (resolvedDecorator?.shadowCaptureExpand ?: 0.dp) + blurProvider.radius.value.dp,
        )

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

    val resolvedDecorator = baseProps.decorator?.resolve(animations = null)
    val overflow = rememberOverflowExtent(resolvedDecorator)

    val staticOffset = baseProps.offset?.asDpOffset()
    val blurRadius = baseProps.decorator?.blurRadius ?: 0f

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
            if (isOverlayContainer) it
            else it.elementBlendOrSkip(baseProps.blend, resolvedDecorator, blurRadius.dp, overflow)
        }
        .let {
            if (isOverlayContainer || blurRadius <= 0f) it
            else it.blur(blurRadius.dp, resolvedDecorator?.shadowCaptureExpand ?: 0.dp)
        }
        .let { if (alpha != 1f && resolvedDecorator == null) it.graphicsLayer(alpha = alpha) else it }
        .let {
            val resolved = resolvedDecorator ?: return@let it
            it.decorator(
                resolved,
                backgroundAlpha,
                alpha = alpha,
                rotation = rotation,
                scale = scale,
                overflow = overflow,
            )
        }
        .overflowExtent(
            overflow,
            tracks = baseProps.tracksOverflow(resolvedDecorator, alpha),
            reach = (resolvedDecorator?.shadowCaptureExpand ?: 0.dp) + blurRadius.dp,
        )

    if (focusRequester != null) {
        result = result.focusRequester(focusRequester).focusable()
    }

    return result
}

@Composable
internal fun BaseProps.tracksOverflow(decorator: ResolvedDecorator?, alpha: Float): Boolean =
    (blend != BlendMode.NORMAL && blend != BlendMode.DESTINATION_IN) ||
        decorator != null && (decorator.blends || alpha != 1f || hasOpacityAnimation() || LocalOpacityProvider.current != null)

@Composable
internal fun rememberOverflowExtent(decorator: ResolvedDecorator?): OverflowExtent? {
    val extent = remember { OverflowExtent() }
    return extent.takeIf { decorator == null || decorator.decorator.clip == Decorator.Clip.NONE }
}

@Composable
internal fun BaseProps.resolveMainDecorator(): ResolvedDecorator? {
    val decorator = decoratorOrShadowHost() ?: return null
    return decorator.resolve(rememberDecoratorAnimations(this, decorator))
}

@Composable
internal fun Modifier.overlayMainDecoratorOrSkip(
    resolved: ResolvedDecorator?,
    rotation: Rotation,
    scale: Scale,
    overflow: OverflowExtent? = null,
): Modifier {
    resolved ?: return this
    return this.decorator(resolved, rotation = rotation, scale = scale, overflow = overflow)
}

private fun BaseProps.decoratorOrShadowHost(): Decorator? =
    decorator ?: if (hasShadowAnimations()) Decorator.background(null) else null

private fun BaseProps.hasShadowAnimations(): Boolean =
    eventHandlers?.any { handler -> handler.animations.any { it.target == LEGACY_SHADOW } } ?: false

private val LEGACY_SHADOW = AnimationTarget(Animation.Role.Shadow, Decorator.LEGACY_ID)
private val LEGACY_INNER_SHADOW = AnimationTarget(Animation.Role.InnerShadow, Decorator.LEGACY_ID)

private val DECORATOR_ROLES = setOf(
    Animation.Role.Background, Animation.Role.Border, Animation.Role.Shadow, Animation.Role.InnerShadow,
)

private fun BaseProps.decoratorTargets(): Set<AnimationTarget> =
    eventHandlers.orEmpty()
        .flatMap { handler -> handler.animations }
        .filter { it.role in DECORATOR_ROLES }
        .mapTo(mutableSetOf()) { it.target }

@Composable
private fun rememberDecoratorAnimations(baseProps: BaseProps, decorator: Decorator): DecoratorAnimations? {
    if (!baseProps.hasVisualAnimations()) return null
    val targets = baseProps.decoratorTargets()
    if (targets.isEmpty()) return null
    val layers = mutableMapOf<Decorator.Layer, LayerOverride>()
    val shadows = mutableMapOf<Decorator.Effect, ResolvedShadow?>()
    val allLayers = decorator.underlays + decorator.overlays
    allLayers.forEachIndexed { layerIndex, layer ->
        key(layerIndex) {
            val id = layer.id
            if (id != null) animatedLayer(baseProps, layer, id, targets)?.let { layers[layer] = it }
            layer.effects.forEachIndexed { effectIndex, effect ->
                val target = effect.id?.let { AnimationTarget(effect.kind.animationRole, it) }
                if (target != null && target in targets) key(effectIndex) {
                    shadows[effect] = animatedShadow(baseProps, target, effect)
                }
            }
        }
    }
    val legacyEffects = allLayers.flatMap { it.effects }.filter { it.id == Decorator.LEGACY_ID }
    val leadingShadow = if (LEGACY_SHADOW in targets && legacyEffects.none { it.kind == Decorator.Effect.Kind.DROP })
        animatedShadow(baseProps, LEGACY_SHADOW, null)
    else null
    val legacyBackground = decorator.underlays.firstOrNull {
        it.id == Decorator.LEGACY_ID && (it.kind is Decorator.Layer.Kind.Fill || it.kind is Decorator.Layer.Kind.Image)
    }
    val addedInner = if (
        legacyBackground != null && LEGACY_INNER_SHADOW in targets &&
        legacyEffects.none { it.kind == Decorator.Effect.Kind.INNER }
    ) animatedShadow(baseProps, LEGACY_INNER_SHADOW, null) else null
    return DecoratorAnimations(layers, shadows, leadingShadow, legacyBackground, addedInner)
}

private val Decorator.Effect.Kind.animationRole: Animation.Role
    get() = when (this) {
        Decorator.Effect.Kind.DROP -> Animation.Role.Shadow
        Decorator.Effect.Kind.INNER -> Animation.Role.InnerShadow
    }

@Composable
private fun animatedLayer(
    baseProps: BaseProps,
    layer: Decorator.Layer,
    id: Int,
    targets: Set<AnimationTarget>,
): LayerOverride? = when (val kind = layer.kind) {
    is Decorator.Layer.Kind.Fill -> animatedFill(baseProps, id, kind.asset, targets)
    is Decorator.Layer.Kind.Image -> animatedFill(baseProps, id, kind.asset, targets)
    is Decorator.Layer.Kind.Stroke -> {
        val target = AnimationTarget(Animation.Role.Border, id)
        if (target in targets) {
            val stroke = kind.asset.resolveAsset<Asset.Filling.Local>()?.let { animatedStrokePaint(it, baseProps, id, kind.asset) }
            LayerOverride(stroke, rememberBorderThicknessProvider(baseProps, id, kind.thickness).value.value)
        } else null
    }
    Decorator.Layer.Kind.Effects -> null
}

@Composable
private fun animatedFill(
    baseProps: BaseProps,
    id: Int,
    asset: VisualValue,
    targets: Set<AnimationTarget>,
): LayerOverride? {
    if (AnimationTarget(Animation.Role.Background, id) !in targets) return null
    val paint = asset.resolveAsset<Asset.Filling.Local>()?.let { animatedFillPaint(it, baseProps, id, asset) }
    return paint?.let { LayerOverride(it, null) }
}

@Composable
private fun animatedShadow(baseProps: BaseProps, target: AnimationTarget, base: Decorator.Effect?): ResolvedShadow? {
    val shadow = if (target.role == Animation.Role.Shadow) {
        rememberShadowProvider(baseProps, target.targetId, base)
            .let { ResolvedShadow(it.color.value, it.blurRadius.value, it.offset.value) }
    } else {
        rememberInnerShadowProvider(baseProps, target.targetId, base)
            .let { ResolvedShadow(it.color.value, it.blurRadius.value, it.offset.value) }
    }
    return shadow.takeIf { it.color.alpha > 0f }
}

@Composable
private fun animatedFillPaint(
    fill: Asset.Composite<Asset.Filling.Local>,
    baseProps: BaseProps,
    id: Int,
    base: VisualValue,
): LayerPaint? {
    val target = AnimationTarget(Animation.Role.Background, id)
    return if (hasMixedAssetTypeAnimations(baseProps, target)) {
        rememberGradientProvider(baseProps, target, base).brush.value.toLayerPaint()
    } else when (fill.main) {
        is Asset.Color -> LayerPaint.Solid(rememberColorProvider(baseProps, id, base).value)
        is Asset.Gradient -> rememberGradientProvider(baseProps, target, base).brush.value.toLayerPaint()
        is Asset.Image -> null
    }
}

@Composable
private fun animatedStrokePaint(
    stroke: Asset.Composite<Asset.Filling.Local>,
    baseProps: BaseProps,
    id: Int,
    base: VisualValue,
): LayerPaint? {
    val target = AnimationTarget(Animation.Role.Border, id)
    return if (hasMixedAssetTypeAnimations(baseProps, target)) {
        rememberBorderGradientProvider(baseProps, id, base).brush.value.toLayerPaint()
    } else when (stroke.main) {
        is Asset.Color -> LayerPaint.Solid(rememberBorderColorProvider(baseProps, id, base).value)
        is Asset.Gradient -> rememberBorderGradientProvider(baseProps, id, base).brush.value.toLayerPaint()
        is Asset.Image -> null
    }
}

private fun Brush.toLayerPaint(): LayerPaint =
    if (this is CrossFadeGradientBrush) LayerPaint.CrossFade(startBrush, endBrush, crossFadeProgress)
    else LayerPaint.Shader(this)

@Composable
private fun hasMixedAssetTypeAnimations(baseProps: BaseProps, target: AnimationTarget): Boolean {
    val animations = activeAnimationsFor(target) ?: baseProps.pendingAppearAnimationsFor(target) ?: return false
    return animations.any { anim ->
        val (start, end) = when (target.role) {
            Animation.Role.Border -> (anim.start as? Border)?.color to (anim.end as? Border)?.color
            else -> anim.start as? VisualValue to anim.end as? VisualValue
        }
        if (start == null || end == null) return@any false
        val startMain = start.resolveAsset<Asset.Filling.Local>()?.main
        val endMain = end.resolveAsset<Asset.Filling.Local>()?.main
        (startMain is Asset.Color && endMain is Asset.Gradient) || (startMain is Asset.Gradient && endMain is Asset.Color)
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

@Suppress("DEPRECATION")
internal object ShrinkHeightIntrinsics : LayoutModifier {
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
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        VStackMinHeightQuery.idealMinIntrinsicHeight(measurable, width)

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
            .then(ShrinkHeightIntrinsics)
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

internal fun Modifier.blurOrSkip(
    blurProvider: BlurProvider,
    captureExpand: Dp = 0.dp,
): Modifier {
    val radius = blurProvider.radius.value
    if (radius <= 0f) return this
    return this.blur(radius.dp, captureExpand)
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

internal fun Modifier.consumePointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}
