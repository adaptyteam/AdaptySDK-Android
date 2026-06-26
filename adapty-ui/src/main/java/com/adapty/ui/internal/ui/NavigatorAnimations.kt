@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.DpOffset
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.Point
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import androidx.compose.ui.platform.LocalLayoutDirection
import com.adapty.ui.internal.ui.attributes.asDpOffset
import com.adapty.ui.internal.ui.attributes.asTransformOrigin
import com.adapty.ui.internal.ui.attributes.degreesIn
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.resolveAsset

@Composable
internal fun Modifier.applyAnimations(animations: List<Animation<*>>): Modifier {
    if (animations.isEmpty()) return this

    val opacityAnims = animations.filterByRole<Float>(Animation.Role.Opacity)
    val offsetAnims = animations.filterByRole<Offset>(Animation.Role.Offset)
    val scaleAnims = animations.filterByRole<Scale>(Animation.Role.Scale)
    val rotationAnims = animations.filterByRole<Rotation>(Animation.Role.Rotation)

    val opacity = rememberAnimatedNavigatorValue(
        opacityAnims,
        initialValue = opacityAnims.firstOrNull()?.start ?: 1f,
        converter = Float.VectorConverter,
    )

    val offsetDpAnims = offsetAnims.mapToDpOffset()
    val offset = rememberAnimatedNavigatorValue(
        offsetDpAnims,
        initialValue = offsetAnims.firstOrNull()?.start?.asDpOffset() ?: DpOffset(0f, 0f),
        converter = OffsetVectorConverter,
    )

    val scale = rememberAnimatedNavigatorValue(
        scaleAnims,
        initialValue = scaleAnims.firstOrNull()?.start ?: Scale.Default,
        converter = ScaleVectorConverter,
    )

    val rotation = rememberAnimatedNavigatorValue(
        rotationAnims,
        initialValue = rotationAnims.firstOrNull()?.start ?: Rotation.Default,
        converter = RotationVectorConverter,
    )

    val rotationVal = rotation.value
    val scaleVal = scale.value
    val alpha = opacity.value
    val anchorLayoutDirection = LocalLayoutDirection.current

    return this
        .let {
            if (rotationVal.anchor == scaleVal.anchor && scaleVal.x == scaleVal.y)
                it.graphicsLayer(
                    rotationZ = rotationVal.degreesIn(anchorLayoutDirection),
                    transformOrigin = rotationVal.anchor.asTransformOrigin(anchorLayoutDirection),
                    scaleX = scaleVal.x,
                    scaleY = scaleVal.y,
                )
            else
                it
                    .graphicsLayer(
                        scaleX = scaleVal.x,
                        scaleY = scaleVal.y,
                        transformOrigin = scaleVal.anchor.asTransformOrigin(anchorLayoutDirection),
                    )
                    .graphicsLayer(
                        rotationZ = rotationVal.degreesIn(anchorLayoutDirection),
                        transformOrigin = rotationVal.anchor.asTransformOrigin(anchorLayoutDirection),
                    )
        }
        .offset {
            IntOffset(
                offset.value.x.dp.roundToPx(),
                offset.value.y.dp.roundToPx(),
            )
        }
        .let { if (alpha != 1f) it.graphicsLayer(alpha = alpha) else it }
}

@Composable
internal fun animateBackgroundColor(
    animation: Animation<*>?,
    staticColor: Color,
): State<Color> {
    if (animation == null) {
        return remember(staticColor) { derivedStateOf { staticColor } }
    }

    val (startColor, endColor) = when (animation.role) {
        Animation.Role.Opacity -> {
            val startAlpha = (animation.start as? Float) ?: return remember(staticColor) { derivedStateOf { staticColor } }
            val endAlpha = (animation.end as? Float) ?: return remember(staticColor) { derivedStateOf { staticColor } }
            staticColor.copy(alpha = staticColor.alpha * startAlpha) to
                    staticColor.copy(alpha = staticColor.alpha * endAlpha)
        }
        Animation.Role.Background -> {
            val start = (animation.start as? VisualValue)?.resolveAsset<Asset.Filling.Local>()
                ?.castOrNull<Asset.Color>()?.toComposeFill()?.color
            val end = (animation.end as? VisualValue)?.resolveAsset<Asset.Filling.Local>()
                ?.castOrNull<Asset.Color>()?.toComposeFill()?.color
            if (start == null || end == null) {
                return remember(staticColor) { derivedStateOf { staticColor } }
            }
            start to end
        }
        else -> return remember(staticColor) { derivedStateOf { staticColor } }
    }

    val colorAnim = Animation(
        startColor,
        endColor,
        animation.durationMillis,
        animation.startDelayMillis,
        animation.repeatDelayMillis,
        animation.pingPongDelayMillis,
        animation.interpolator,
        animation.repeatMode,
        animation.repeatMaxCount,
        animation.role,
    )

    return rememberAnimatedNavigatorValue(
        listOf(colorAnim),
        initialValue = startColor,
        converter = Color.VectorConverter(ColorSpaces.Srgb),
    )
}

internal fun totalDurationMillis(animations: List<Animation<*>>): Long {
    if (animations.isEmpty()) return 0L
    return animations.maxOf { it.startDelayMillis.toLong() + it.durationMillis.toLong() }
}

internal fun totalDurationMillis(bgAnim: Animation<*>?, contentAnims: List<Animation<*>>): Long {
    val bgDuration = bgAnim?.let { it.startDelayMillis.toLong() + it.durationMillis.toLong() } ?: 0L
    val contentDuration = totalDurationMillis(contentAnims)
    return maxOf(bgDuration, contentDuration)
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> List<Animation<*>>.filterByRole(role: Animation.Role): List<Animation<T>> =
    filter { it.role == role && it.start is T && it.end is T } as List<Animation<T>>

@Composable
private fun List<Animation<Offset>>.mapToDpOffset(): List<Animation<DpOffset>> =
    mapNotNull { anim ->
        val start = anim.start.asDpOffset()
        val end = anim.end.asDpOffset()
        Animation(
            start, end,
            anim.durationMillis, anim.startDelayMillis,
            anim.repeatDelayMillis, anim.pingPongDelayMillis,
            anim.interpolator, anim.repeatMode, anim.repeatMaxCount,
            anim.role,
        )
    }

@Composable
private fun <T> rememberAnimatedNavigatorValue(
    animations: List<Animation<T>>,
    initialValue: T,
    converter: TwoWayConverter<T, out AnimationVector>,
): State<T> {
    if (animations.isEmpty()) {
        return remember(initialValue) { derivedStateOf { initialValue } }
    }
    return rememberAnimatedValueWithRunner(animations, initialValue, converter)
}

private val OffsetVectorConverter: TwoWayConverter<DpOffset, AnimationVector> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.y, it.x) },
        convertFromVector = {
            val vec = it as AnimationVector2D
            DpOffset(vec.v1, vec.v2)
        }
    )

private val ScaleVectorConverter: TwoWayConverter<Scale, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = { AnimationVector4D(it.y, it.x, it.anchor.y, it.anchor.x) },
        convertFromVector = { Scale(Point(it.v1, it.v2), Point(it.v3, it.v4)) }
    )

private val RotationVectorConverter: TwoWayConverter<Rotation, AnimationVector3D> =
    TwoWayConverter(
        convertToVector = { AnimationVector3D(it.degrees, it.anchor.y, it.anchor.x) },
        convertFromVector = { Rotation(it.v1, Point(it.v2, it.v3)) }
    )
