@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.script.StateAccessor
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.Border
import com.adapty.ui.internal.ui.attributes.Box
import com.adapty.ui.internal.ui.attributes.toGradientAsset
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DpOffset
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.attributes.Shadow
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.asDpOffset
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toExactDp
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.event.AnimationPlayback
import com.adapty.ui.internal.ui.event.LocalEventDispatcher
import com.adapty.ui.internal.ui.event.rememberActiveAnimations
import com.adapty.ui.internal.ui.event.appearAnimations
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.ui.rememberOpacityProvider
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.resolveAsset
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

internal val LocalActiveAnimations = staticCompositionLocalOf<State<AnimationPlayback?>> {
    mutableStateOf(null)
}

internal val LocalRoleSequences = compositionLocalOf<Map<Animation.Role, Long>> { emptyMap() }

@Composable
internal fun activeAnimationsFor(role: Animation.Role): List<Animation<*>>? =
    LocalActiveAnimations.current.value?.animations
        ?.filter { anim -> anim.role == role }
        ?.takeIf { it.isNotEmpty() }

@Composable
internal fun BaseProps.pendingAppearAnimationsFor(role: Animation.Role): List<Animation<*>>? {
    if (LocalActiveAnimations.current.value != null) return null
    val handlers = eventHandlers ?: return null
    val dispatcher = LocalEventDispatcher.current
    val entry = LocalScreenInstance.current
    val instanceId = entry.screenInstanceId
    return handlers.appearAnimations(
        predictedFireCount = { eventId -> dispatcher.fireCount(instanceId, eventId) + 1 },
        currentTransitionId = entry.transitionId,
    )
        .filter { anim -> anim.role == role }
        .takeIf { it.isNotEmpty() }
}

public object UnknownElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public object SkippedElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public data class BaseProps internal constructor(
     internal val widthSpec: DimSpec? = null,
     internal val heightSpec: DimSpec? = null,
     internal val weight: Float? = null,
     internal val shape: Shape? = null,
     internal val padding: EdgeEntities? = null,
     internal val offset: Offset? = null,
     internal val opacity: Float = 1f,
     internal val focusId: String? = null,
     internal val rotation: Rotation? = null,
     internal val scale: Scale? = null,
     internal val uiEnabled: com.adapty.ui.internal.utils.OneWayBinding? = null,
     internal val eventHandlers: List<com.adapty.ui.internal.ui.event.EventHandler>? = null,
) {
    internal companion object {
        val EMPTY = BaseProps()
    }
}

@InternalAdaptyApi
public interface Container<T> {
    public var content: T
}
internal interface SingleContainer: Container<UIElement>
internal interface MultiContainer: Container<List<UIElement>>

@InternalAdaptyApi
public sealed class Condition {
    public class SelectedSection internal constructor(internal val sectionId: String, internal val index: Int): Condition()
    public class SelectedProduct internal constructor(internal val productId: String, internal val groupId: String): Condition()
    public object Unknown: Condition()
}

@InternalAdaptyApi
public class Action internal constructor(
    internal val func: String,
    internal val params: Map<String, Any?>,
    internal val scope: Scope,
)

internal fun RowScope.fillModifierWithScopedParams(element: UIElement, modifier: Modifier): Modifier {
    var modifier = modifier
    val props = element.layoutRelevantProps
    val weight = props.weight
    if (weight != null)
        modifier = modifier.weight(weight)
    else if (props.widthSpec is DimSpec.FillMax)
        modifier = modifier.weight(1f)
    return modifier
}

internal fun ColumnScope.fillModifierWithScopedParams(element: UIElement, modifier: Modifier): Modifier {
    var modifier = modifier
    val props = element.layoutRelevantProps
    val weight = props.weight
    if (weight != null)
        modifier = modifier.weight(weight)
    else if (props.heightSpec is DimSpec.FillMax)
        modifier = modifier.weight(1f)
    return modifier
}

@OptIn(InternalAdaptyApi::class)
internal fun UIElement.explicitlyFillsRowHeight(): Boolean =
    layoutRelevantProps.heightSpec is DimSpec.FillMax

@OptIn(InternalAdaptyApi::class)
internal fun UIElement.isVerticallyFlexible(): Boolean {
    when (layoutRelevantProps.heightSpec) {
        is DimSpec.Specified, is DimSpec.Shrink, is DimSpec.Min -> return false
        is DimSpec.FillMax -> return true
        null -> Unit
    }
    return when (this) {
        is BoxWithoutContentElement -> true
        is SingleContainer -> content.isVerticallyFlexible()
        is MultiContainer -> content.any { it.isVerticallyFlexible() }
        else -> false
    }
}

@Composable
internal fun UIElement.withActiveAnimations(
    dispatch: (Message) -> Unit,
    content: @Composable () -> Unit,
) {
    withActiveAnimations(baseProps, dispatch, content = content)
}

@Composable
internal fun withActiveAnimations(
    baseProps: BaseProps,
    dispatch: (Message) -> Unit,
    content: @Composable () -> Unit,
) {
    val activeAnimations = rememberActiveAnimations(baseProps, dispatch)
    CompositionLocalProvider(
        LocalActiveAnimations provides activeAnimations,
        LocalRoleSequences provides (activeAnimations.value?.sequenceByRole ?: emptyMap()),
    ) {
        val uiEnabledBinding = baseProps.uiEnabled
        val parentEnabled = com.adapty.ui.internal.ui.LocalUiEnabled.current
        val selfEnabled = if (uiEnabledBinding != null) {
            com.adapty.ui.internal.ui.resolveState()[uiEnabledBinding] as? Boolean ?: true
        } else true
        val visible = if (baseProps.hasOpacityAnimation()) {
            val alpha = rememberOpacityProvider(baseProps).alpha
            remember(alpha) { derivedStateOf { alpha.value > 0f } }.value
        } else {
            baseProps.opacity > 0f
        }
        val isEnabled = parentEnabled && selfEnabled && visible
        if (isEnabled != parentEnabled) {
            CompositionLocalProvider(
                com.adapty.ui.internal.ui.LocalUiEnabled provides isEnabled
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

internal fun BaseProps.hasOpacityAnimation(): Boolean =
    eventHandlers?.any { handler ->
        handler.animations.any { it.role == Animation.Role.Opacity }
    } ?: false

@Composable
internal fun UIElement.render(
    dispatch: (Message) -> Unit,
) {
    withActiveAnimations(dispatch) {
        render(
            dispatch,
            Modifier.fillWithBaseParams(this@render)
        )
    }
}

@Composable
internal fun UIElement.render(
    dispatch: (Message) -> Unit,
    modifier: Modifier,
) {
    render(toComposable(dispatch, modifier))
}

@Composable
internal fun UIElement.render(
    toComposable: @Composable () -> Unit,
) {
    toComposable
        .invoke()
}

internal sealed class AnimationBehavior<T> {
    data class Static<T>(val value: T) : AnimationBehavior<T>()
    data class Animated<T>(val singleValueAnimsOrdered: List<Animation<T>>, val defaultValue: T) : AnimationBehavior<T>()
    class None<T>(val zero: T) : AnimationBehavior<T>()
}

internal class RotationProvider(val rotation: State<Rotation>)
internal class OpacityProvider(val alpha: State<Float>)
internal class OffsetProvider(
    val offset: State<DpOffset>,
    val active: Boolean = true,
)
internal class ScaleProvider(val scale: State<Scale>)
internal class BoxProvider(val width: State<Dp>, val height: State<Dp>)
internal class ShadowProvider(val color: State<Color>, val blurRadius: State<Float>, val offset: State<DpOffset>)
internal class InnerShadowProvider(val color: State<Color>, val blurRadius: State<Float>, val offset: State<DpOffset>)
internal class BlurProvider(val radius: State<Float>)
internal class BrushProvider(val brush: State<Brush>)

internal val BaseProps.opacityBehaviour: AnimationBehavior<Float>
    @Composable get() = findAnimationBehaviour(Animation.Role.Opacity, opacity)

internal val BaseProps.blurBehaviour: AnimationBehavior<Float>
    @Composable get() = findAnimationBehaviour(Animation.Role.Blur, shape?.blurRadius ?: 0f)

internal val BaseProps.rotationBehaviour: AnimationBehavior<Rotation>
    @Composable get() = findAnimationBehaviour(Animation.Role.Rotation, rotation ?: Rotation.Default)

internal val BaseProps.offsetBehaviour: AnimationBehavior<DpOffset>
    @Composable get() = findAnimationBehaviour(
        Animation.Role.Offset,
        {
            AnimationBehavior.Static((offset ?: Offset.Default).asDpOffset())
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = anim.start as? Offset ?: return@mapNotNull null
                    val end = anim.end as? Offset ?: return@mapNotNull null
                    Animation(
                        DpOffset(
                            start.y.toExactDp(DimSpec.Axis.Y).value,
                            start.x.toExactDp(DimSpec.Axis.X).value,
                        ),
                        DpOffset(
                            end.y.toExactDp(DimSpec.Axis.Y).value,
                            end.x.toExactDp(DimSpec.Axis.X).value,
                        ),
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                (offset ?: Offset.Default).asDpOffset(),
            )
        },
    )

internal val BaseProps.scaleBehaviour: AnimationBehavior<Scale>
    @Composable get() = findAnimationBehaviour(Animation.Role.Scale, scale ?: Scale.Default)

@Composable
internal fun BaseProps.colorBehaviour(): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.Background,
        {
            val color =
                shape?.fill?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? VisualValue)?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? VisualValue)?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.fill?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
                    ?: Color.Transparent,
            )
        },
    )

@Composable
internal fun BaseProps.gradientBehaviour(): AnimationBehavior<Brush> =
    findAnimationBehaviour(
        Animation.Role.Background,
        {
            val brush =
                shape?.fill?.let { value ->
                    val asset = value.resolveAsset<Asset.Filling.Local>()
                    (asset?.castOrNull<Asset.Gradient>()?.toComposeFill()
                        ?: asset?.castOrNull<Asset.Color>()?.toGradientAsset()?.toComposeFill())?.shader
                }
            AnimationBehavior.Static(brush ?: Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val startAssetId = anim.start as? VisualValue
                    val endAssetId = anim.end as? VisualValue
                    
                    val startAsset = startAssetId?.resolveAsset<Asset.Filling.Local>()
                    val endAsset = endAssetId?.resolveAsset<Asset.Filling.Local>()

                    val startAssetAsGradient = startAsset?.castOrNull<Asset.Gradient>()
                    val startAssetAsColor = startAsset?.castOrNull<Asset.Color>()
                    val endAssetAsGradient = endAsset?.castOrNull<Asset.Gradient>()
                    val endAssetAsColor = endAsset?.castOrNull<Asset.Color>()

                    val startGradient = when {
                        startAssetAsGradient != null -> startAssetAsGradient
                        startAssetAsColor != null && endAssetAsGradient != null -> {
                            startAssetAsColor.toGradientAsset(endAssetAsGradient)
                        }
                        else -> null
                    }
                    
                    val endGradient = when {
                        endAssetAsGradient != null -> endAssetAsGradient
                        endAssetAsColor != null && startAssetAsGradient != null -> {
                            endAssetAsColor.toGradientAsset(startAssetAsGradient)
                        }
                        else -> null
                    }

                    val startBrush = startGradient?.toComposeFill()?.shader ?: return@mapNotNull null
                    val endBrush = endGradient?.toComposeFill()?.shader ?: return@mapNotNull null
                    
                    Animation(
                        startBrush,
                        endBrush,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.fill?.let { value ->
                    val asset = value.resolveAsset<Asset.Filling.Local>()
                    (asset?.castOrNull<Asset.Gradient>()?.toComposeFill()
                        ?: asset?.castOrNull<Asset.Color>()?.toGradientAsset()?.toComposeFill())?.shader
                } ?: Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            )
        },
    )

@Composable
internal fun BaseProps.borderColorBehaviour(): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.Border,
        {
            val color =
                shape?.border?.color?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Border)?.color?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? Border)?.color?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.border?.color?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
                    ?: Color.Transparent,
            )
        },
    )

@Composable
internal fun BaseProps.borderGradientBehaviour(): AnimationBehavior<Brush> =
    findAnimationBehaviour(
        Animation.Role.Border,
        {
            val brush =
                shape?.border?.color?.let { value ->
                    val asset = value.resolveAsset<Asset.Filling.Local>()
                    (asset?.castOrNull<Asset.Gradient>()?.toComposeFill()
                        ?: asset?.castOrNull<Asset.Color>()?.toGradientAsset()?.toComposeFill())?.shader
                }
            AnimationBehavior.Static(brush ?: Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val startAssetId = (anim.start as? Border)?.color
                    val endAssetId = (anim.end as? Border)?.color
                    
                    val startAsset = startAssetId?.resolveAsset<Asset.Filling.Local>()
                    val endAsset = endAssetId?.resolveAsset<Asset.Filling.Local>()

                    val startAssetAsGradient = startAsset?.castOrNull<Asset.Gradient>()
                    val startAssetAsColor = startAsset?.castOrNull<Asset.Color>()
                    val endAssetAsGradient = endAsset?.castOrNull<Asset.Gradient>()
                    val endAssetAsColor = endAsset?.castOrNull<Asset.Color>()

                    val startGradient = when {
                        startAssetAsGradient != null -> startAssetAsGradient
                        startAssetAsColor != null && endAssetAsGradient != null -> {
                            startAssetAsColor.toGradientAsset(endAssetAsGradient)
                        }
                        else -> null
                    }
                    
                    val endGradient = when {
                        endAssetAsGradient != null -> endAssetAsGradient
                        endAssetAsColor != null && startAssetAsGradient != null -> {
                            endAssetAsColor.toGradientAsset(startAssetAsGradient)
                        }
                        else -> null
                    }

                    val startBrush = startGradient?.toComposeFill()?.shader ?: return@mapNotNull null
                    val endBrush = endGradient?.toComposeFill()?.shader ?: return@mapNotNull null
                    
                    Animation(
                        startBrush,
                        endBrush,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.border?.color?.let { value ->
                    val asset = value.resolveAsset<Asset.Filling.Local>()
                    (asset?.castOrNull<Asset.Gradient>()?.toComposeFill()
                        ?: asset?.castOrNull<Asset.Color>()?.toGradientAsset()?.toComposeFill())?.shader
                } ?: Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            )
        },
    )

@Composable
internal fun BaseProps.borderThicknessBehaviour(): AnimationBehavior<Dp> =
    findAnimationBehaviour(
        Animation.Role.Border,
        {
            val thickness = shape?.border?.thickness?.dp ?: 0.dp
            AnimationBehavior.Static(thickness)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Border)?.thickness?.dp ?: return@mapNotNull null
                    val end = (anim.end as? Border)?.thickness?.dp ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.border?.thickness?.dp ?: 0.dp,
            )
        },
    )

@Composable
internal fun BaseProps.widthBehaviour(): AnimationBehavior<Dp> =
    findAnimationBehaviour(
        Animation.Role.Box,
        {
            val width = widthSpec?.let { spec ->
                when (spec) {
                    is DimSpec.Specified -> spec.value.toExactDp(DimSpec.Axis.X)
                    else -> null
                }
            } ?: Dp.Unspecified
            AnimationBehavior.Static(width)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Box)?.width?.toExactDp(DimSpec.Axis.X) ?: return@mapNotNull null
                    val end = (anim.end as? Box)?.width?.toExactDp(DimSpec.Axis.X) ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                widthSpec?.let { spec ->
                    when (spec) {
                        is DimSpec.Specified -> spec.value.toExactDp(DimSpec.Axis.X)
                        else -> Dp.Unspecified
                    }
                } ?: Dp.Unspecified,
            )
        },
    )

@Composable
internal fun BaseProps.heightBehaviour(): AnimationBehavior<Dp> =
    findAnimationBehaviour(
        Animation.Role.Box,
        {
            val height = heightSpec?.let { spec ->
                when (spec) {
                    is DimSpec.Specified -> spec.value.toExactDp(DimSpec.Axis.Y)
                    else -> null
                }
            } ?: Dp.Unspecified
            AnimationBehavior.Static(height)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Box)?.height?.toExactDp(DimSpec.Axis.Y) ?: return@mapNotNull null
                    val end = (anim.end as? Box)?.height?.toExactDp(DimSpec.Axis.Y) ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                heightSpec?.let { spec ->
                    when (spec) {
                        is DimSpec.Specified -> spec.value.toExactDp(DimSpec.Axis.Y)
                        else -> Dp.Unspecified
                    }
                } ?: Dp.Unspecified,
            )
        },
    )

@Composable
internal fun BaseProps.shadowColorBehaviour(): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.Shadow,
        {
            val color =
                shape?.shadow?.color?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.color?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.color?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.shadow?.color?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
                    ?: Color.Transparent,
            )
        },
    )

@Composable
internal fun BaseProps.shadowBlurRadiusBehaviour(): AnimationBehavior<Float> =
    findAnimationBehaviour(
        Animation.Role.Shadow,
        {
            val blurRadius = shape?.shadow?.blurRadius ?: 0f
            AnimationBehavior.Static(blurRadius)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.blurRadius ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.blurRadius ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.shadow?.blurRadius ?: 0f,
            )
        },
    )

@Composable
internal fun BaseProps.shadowOffsetBehaviour(): AnimationBehavior<DpOffset> =
    findAnimationBehaviour(
        Animation.Role.Shadow,
        {
            val offset = (shape?.shadow?.offset ?: Offset.Default).asDpOffset()
            AnimationBehavior.Static(offset)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.offset ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.offset ?: return@mapNotNull null
                    Animation(
                        DpOffset(
                            start.y.toExactDp(DimSpec.Axis.Y).value,
                            start.x.toExactDp(DimSpec.Axis.X).value,
                        ),
                        DpOffset(
                            end.y.toExactDp(DimSpec.Axis.Y).value,
                            end.x.toExactDp(DimSpec.Axis.X).value,
                        ),
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                (shape?.shadow?.offset ?: Offset.Default).asDpOffset(),
            )
        },
    )

@Composable
internal fun BaseProps.innerShadowColorBehaviour(): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.InnerShadow,
        {
            val color =
                shape?.innerShadow?.color?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.color?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.color?.resolveAsset<Asset.Filling.Local>()
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.innerShadow?.color?.resolveAsset<Asset.Filling.Local>()
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
                    ?: Color.Transparent,
            )
        },
    )

@Composable
internal fun BaseProps.innerShadowBlurRadiusBehaviour(): AnimationBehavior<Float> =
    findAnimationBehaviour(
        Animation.Role.InnerShadow,
        {
            val blurRadius = shape?.innerShadow?.blurRadius ?: 0f
            AnimationBehavior.Static(blurRadius)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.blurRadius ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.blurRadius ?: return@mapNotNull null
                    Animation(
                        start,
                        end,
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                shape?.innerShadow?.blurRadius ?: 0f,
            )
        },
    )

@Composable
internal fun BaseProps.innerShadowOffsetBehaviour(): AnimationBehavior<DpOffset> =
    findAnimationBehaviour(
        Animation.Role.InnerShadow,
        {
            val offset = (shape?.innerShadow?.offset ?: Offset.Default).asDpOffset()
            AnimationBehavior.Static(offset)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.offset ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.offset ?: return@mapNotNull null
                    Animation(
                        DpOffset(
                            start.y.toExactDp(DimSpec.Axis.Y).value,
                            start.x.toExactDp(DimSpec.Axis.X).value,
                        ),
                        DpOffset(
                            end.y.toExactDp(DimSpec.Axis.Y).value,
                            end.x.toExactDp(DimSpec.Axis.X).value,
                        ),
                        anim.durationMillis,
                        anim.startDelayMillis,
                        anim.repeatDelayMillis,
                        anim.pingPongDelayMillis,
                        anim.interpolator,
                        anim.repeatMode,
                        anim.repeatMaxCount,
                        anim.role,
                    )
                },
                (shape?.innerShadow?.offset ?: Offset.Default).asDpOffset(),
            )
        },
    )

@Composable
private inline fun <reified T> BaseProps.findAnimationBehaviour(
    role: Animation.Role,
    defaultValue: T,
): AnimationBehavior<T> =
    findAnimationBehaviour(
        role,
        { AnimationBehavior.Static(defaultValue) },
        { anims -> AnimationBehavior.Animated(anims.mapNotNull { it.takeIf { it.start is T && it.end is T } }.filterIsInstance<Animation<T>>(), defaultValue) },
    )

@Composable
private inline fun <reified T> BaseProps.findAnimationBehaviour(
    role: Animation.Role,
    createDefaultValue: () -> AnimationBehavior<T>,
    createAnimatedValue: (anims: Iterable<Animation<*>>) -> AnimationBehavior<T>,
): AnimationBehavior<T> {
    activeAnimationsFor(role)
        ?.sortedBy { it.startDelayMillis }
        ?.let { anims -> return createAnimatedValue(anims) }
    pendingAppearAnimationsFor(role)?.let { anims ->
        val pending = createAnimatedValue(anims)
        if (pending is AnimationBehavior.Animated && pending.singleValueAnimsOrdered.isNotEmpty())
            return AnimationBehavior.Static(pending.singleValueAnimsOrdered.first().start)
    }
    return createDefaultValue()
}

public typealias ResolveAssets = () -> Assets
public typealias ResolveText = @Composable (stringId: StringId, textAttrs: Attributes?) -> StringWrapper?
public typealias ResolveState = @Composable () -> StateAccessor
