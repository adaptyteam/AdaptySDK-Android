@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.StringWrapper
import com.adapty.ui.internal.text.toPlainString
import com.adapty.ui.internal.text.toStringId
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
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public object UnknownElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public class ReferenceElement internal constructor(internal val id: String): UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = {}
}

@InternalAdaptyApi
public object SkippedElement: UIElement {
    override val baseProps: BaseProps = BaseProps.EMPTY

    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
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
     internal val onAppear: List<Animation<*>>? = null,
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
public sealed class Action {
    public class OpenUrl internal constructor(internal val url: String): Action()
    public class Custom internal constructor(internal val customId: String): Action()
    public class SelectProduct internal constructor(internal val productId: String, internal val groupId: String): Action()
    public class UnselectProduct internal constructor(internal val groupId: String): Action()
    public class PurchaseProduct internal constructor(internal val productId: String): Action()
    public class PurchaseSelectedProduct internal constructor(internal val groupId: String): Action()
    public object RestorePurchases: Action()
    public class OpenScreen internal constructor(internal val screenId: String): Action()
    public object CloseCurrentScreen: Action()
    public class SwitchSection internal constructor(internal val sectionId: String, internal val index: Int): Action()
    public object ClosePaywall: Action()
    public object Unknown: Action()

    @Composable
    internal fun resolve(resolveText: ResolveText): Action? {
        return when(this) {
            is OpenUrl -> {
                val actualUrl = kotlin.runCatching { url.toStringId() }.getOrElse { e ->
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't extract value for${url}: ${e.localizedMessage})" }
                    null
                }?.let { resolveText(it, null) }?.toPlainString()
                return if (actualUrl != null)
                    OpenUrl(actualUrl)
                else {
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't find a string value for this id (${url})" }
                    null
                }
            }
            else -> this
        }
    }
}

internal fun RowScope.fillModifierWithScopedParams(element: UIElement, modifier: Modifier): Modifier {
    var modifier = modifier
    val weight = element.baseProps.weight
    if (weight != null)
        modifier = modifier.weight(weight)
    return modifier
}

internal fun ColumnScope.fillModifierWithScopedParams(element: UIElement, modifier: Modifier): Modifier {
    var modifier = modifier
    val weight = element.baseProps.weight
    if (weight != null)
        modifier = modifier.weight(weight)
    return modifier
}

@Composable
internal fun UIElement.render(
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
) {
    render(
        resolveAssets,
        resolveText,
        resolveState,
        eventCallback,
        Modifier.fillWithBaseParams(this, resolveAssets)
    )
}

@Composable
internal fun UIElement.render(
    resolveAssets: ResolveAssets,
    resolveText: ResolveText,
    resolveState: ResolveState,
    eventCallback: EventCallback,
    modifier: Modifier,
) {
    render(
        toComposable(
            resolveAssets,
            resolveText,
            resolveState,
            eventCallback,
            modifier,
        )
    )
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
internal class BrushProvider(val brush: State<Brush>)

internal val BaseProps.opacityBehaviour: AnimationBehavior<Float>
    get() = findAnimationBehaviour(Animation.Role.Opacity, opacity)

internal val BaseProps.rotationBehaviour: AnimationBehavior<Rotation>
    get() = findAnimationBehaviour(Animation.Role.Rotation, Rotation.Default)

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
    get() = findAnimationBehaviour(Animation.Role.Scale, Scale.Default)

@Composable
internal fun BaseProps.colorBehaviour(resolveAssets: ResolveAssets): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.Background,
        {
            val color =
                shape?.fill?.assetId?.let { assetId -> resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? String)?.let { assetId ->
                        resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? String)?.let { assetId ->
                        resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
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
                shape?.fill?.assetId?.let { assetId -> resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
                    ?: Color.Transparent,
            )
        },
    )

@Composable
internal fun BaseProps.gradientBehaviour(resolveAssets: ResolveAssets): AnimationBehavior<Brush> =
    findAnimationBehaviour(
        Animation.Role.Background,
        {
            val brush =
                shape?.fill?.assetId?.let { assetId ->
                    val asset = resolveAssets().getAsset<Asset.Filling.Local>(assetId)
                    (asset?.castOrNull<Asset.Gradient>()?.toComposeFill()
                        ?: asset?.castOrNull<Asset.Color>()?.toGradientAsset()?.toComposeFill())?.shader
                }
            AnimationBehavior.Static(brush ?: Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val startAssetId = anim.start as? String
                    val endAssetId = anim.end as? String
                    
                    val startAsset = startAssetId?.let { resolveAssets().getAsset<Asset.Filling.Local>(it) }
                    val endAsset = endAssetId?.let { resolveAssets().getAsset<Asset.Filling.Local>(it) }

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
                shape?.fill?.assetId?.let { assetId ->
                    val asset = resolveAssets().getAsset<Asset.Filling.Local>(assetId)
                    (asset?.castOrNull<Asset.Gradient>()?.toComposeFill()
                        ?: asset?.castOrNull<Asset.Color>()?.toGradientAsset()?.toComposeFill())?.shader
                } ?: Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            )
        },
    )

@Composable
internal fun BaseProps.borderColorBehaviour(resolveAssets: ResolveAssets): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.Border,
        {
            val color =
                shape?.border?.color?.let { assetId -> resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Border)?.color?.let { assetId ->
                        resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? Border)?.color?.let { assetId ->
                        resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
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
                shape?.border?.color?.let { assetId -> resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
                    ?: Color.Transparent,
            )
        },
    )

@Composable
internal fun BaseProps.borderGradientBehaviour(resolveAssets: ResolveAssets): AnimationBehavior<Brush> =
    findAnimationBehaviour(
        Animation.Role.Border,
        {
            val brush =
                shape?.border?.color?.let { assetId ->
                    val asset = resolveAssets().getAsset<Asset.Filling.Local>(assetId)
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
                    
                    val startAsset = startAssetId?.let { resolveAssets().getAsset<Asset.Filling.Local>(it) }
                    val endAsset = endAssetId?.let { resolveAssets().getAsset<Asset.Filling.Local>(it) }

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
                shape?.border?.color?.let { assetId ->
                    val asset = resolveAssets().getAsset<Asset.Filling.Local>(assetId)
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
                    val start = (anim.start as? Box)?.height?.toExactDp(DimSpec.Axis.X) ?: return@mapNotNull null
                    val end = (anim.end as? Box)?.height?.toExactDp(DimSpec.Axis.X) ?: return@mapNotNull null
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
internal fun BaseProps.shadowColorBehaviour(resolveAssets: ResolveAssets): AnimationBehavior<Color> =
    findAnimationBehaviour(
        Animation.Role.Shadow,
        {
            val color =
                shape?.shadow?.color?.let { assetId -> resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                    ?.castOrNull<Asset.Color>()
                    ?.toComposeFill()?.color
            AnimationBehavior.Static(color ?: Color.Transparent)
        },
        { anims ->
            AnimationBehavior.Animated(
                anims.mapNotNull { anim ->
                    val start = (anim.start as? Shadow)?.color?.let { assetId ->
                        resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
                        ?.castOrNull<Asset.Color>()
                        ?.toComposeFill()?.color
                        ?: return@mapNotNull null
                    val end = (anim.end as? Shadow)?.color?.let { assetId ->
                        resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
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
                shape?.shadow?.color?.let { assetId -> resolveAssets().getAsset<Asset.Filling.Local>(assetId) }
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

private inline fun <reified T> BaseProps.findAnimationBehaviour(
    role: Animation.Role,
    defaultValue: T,
): AnimationBehavior<T> =
    findAnimationBehaviour(
        role,
        { AnimationBehavior.Static(defaultValue) },
        { anims -> AnimationBehavior.Animated(anims.mapNotNull { it.takeIf { it.start is T && it.end is T } }.filterIsInstance<Animation<T>>(), defaultValue) },
    )

private inline fun <reified T> BaseProps.findAnimationBehaviour(
    role: Animation.Role,
    createDefaultValue: () -> AnimationBehavior<T>,
    createAnimatedValue: (anims: Iterable<Animation<*>>) -> AnimationBehavior<T>,
): AnimationBehavior<T> =
    onAppear
        ?.filter { anim -> anim.role == role }
        ?.takeIf { it.isNotEmpty() }
        ?.sortedBy { it.startDelayMillis }
        ?.let { anims -> createAnimatedValue(anims) }
        ?: createDefaultValue()

public typealias ResolveAssets = () -> Assets
public typealias ResolveText = @Composable (stringId: StringId, textAttrs: Attributes?) -> StringWrapper?
public typealias ResolveState = () -> Map<String, Any>

