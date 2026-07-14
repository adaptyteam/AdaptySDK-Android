@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.DpOffset
import com.adapty.ui.internal.ui.attributes.Point
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.attributes.easing
import com.adapty.ui.internal.ui.element.AnimationBehavior
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.BoxProvider
import com.adapty.ui.internal.ui.element.OffsetProvider
import com.adapty.ui.internal.ui.element.OpacityProvider
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.RotationProvider
import com.adapty.ui.internal.ui.element.ScaleProvider
import com.adapty.ui.internal.ui.element.BrushProvider
import com.adapty.ui.internal.ui.element.ShadowProvider
import com.adapty.ui.internal.ui.element.colorBehaviour
import com.adapty.ui.internal.ui.element.offsetBehaviour
import com.adapty.ui.internal.ui.element.opacityBehaviour
import com.adapty.ui.internal.ui.element.rotationBehaviour
import com.adapty.ui.internal.ui.element.scaleBehaviour
import com.adapty.ui.internal.ui.element.borderColorBehaviour
import com.adapty.ui.internal.ui.element.borderThicknessBehaviour
import kotlinx.coroutines.delay
import com.adapty.ui.internal.ui.element.widthBehaviour
import com.adapty.ui.internal.ui.element.heightBehaviour
import com.adapty.ui.internal.ui.element.blurBehaviour
import com.adapty.ui.internal.ui.element.BlurProvider
import com.adapty.ui.internal.ui.element.InnerShadowProvider
import com.adapty.ui.internal.ui.element.LocalRoleSequences
import com.adapty.ui.internal.ui.element.innerShadowBlurRadiusBehaviour
import com.adapty.ui.internal.ui.element.innerShadowColorBehaviour
import com.adapty.ui.internal.ui.element.innerShadowOffsetBehaviour
import com.adapty.ui.internal.ui.element.shadowBlurRadiusBehaviour
import com.adapty.ui.internal.ui.element.shadowColorBehaviour
import com.adapty.ui.internal.ui.element.shadowOffsetBehaviour
import com.adapty.ui.internal.utils.areAnimationsDisabled

@Composable
internal fun <T> rememberAnimatedValue(
    behavior: AnimationBehavior<T>,
    converter: TwoWayConverter<T, out AnimationVector>
): State<T> {
    val (animations, initialValue) = when (behavior) {
        is AnimationBehavior.Static -> emptyList<Animation<T>>() to behavior.value
        is AnimationBehavior.Animated ->
            behavior.singleValueAnimsOrdered to
                (behavior.singleValueAnimsOrdered.firstOrNull()?.start ?: behavior.defaultValue)
        is AnimationBehavior.None -> emptyList<Animation<T>>() to behavior.zero
    }
    return rememberAnimatedValueWithRunner(animations, initialValue, converter)
}

@Composable
private fun <T> rememberBindableAnimatedValue(
    behavior: AnimationBehavior<T>,
    converter: TwoWayConverter<T, out AnimationVector>,
): State<T> {
    val animatedValue = rememberAnimatedValue(behavior, converter)
    val cellWritten = remember { mutableStateOf(false) }
    val behaviorIsAnimated = behavior is AnimationBehavior.Animated &&
        behavior.singleValueAnimsOrdered.isNotEmpty()
    val staticFallback = when (behavior) {
        is AnimationBehavior.Static -> behavior.value
        is AnimationBehavior.Animated -> behavior.defaultValue
        is AnimationBehavior.None -> behavior.zero
    }
    val staticState = rememberUpdatedState(staticFallback)
    if (behaviorIsAnimated && !cellWritten.value) {
        cellWritten.value = true
    }
    return if (cellWritten.value || behaviorIsAnimated) animatedValue else staticState
}

@Composable
internal fun <T> rememberAnimatedValueWithRunner(
    animations: List<Animation<T>>,
    initialValue: T,
    converter: TwoWayConverter<T, out AnimationVector>,
): State<T> {
    val seq = animations.firstOrNull()?.role
        ?.let { role -> LocalRoleSequences.current[role] } ?: -1L
    val runner = remember(animations, seq) {
        if (animations.isEmpty()) null else AnimationRunner(animations)
    }

    val hasFiniteEnd = remember(animations) {
        animations.isNotEmpty() && animations.all { it.repeatMaxCount != Int.MAX_VALUE }
    }

    val anim = remember { Animatable(initialValue, converter) }
    val context = LocalContext.current
    val animationsDisabled = remember(context) {
        context.areAnimationsDisabled()
    }

    val playedSequences = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver<MutableSet<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableSet() },
        ),
    ) { mutableSetOf<Long>() }

    LaunchedEffect(runner, animationsDisabled) {
        val activeRunner = runner ?: return@LaunchedEffect

        val isReplay = seq >= 0L && seq in playedSequences
        if (isReplay && hasFiniteEnd) {
            val longest = animations.maxByOrNull { it.startDelayMillis + it.durationMillis }
            val snapValue = when {
                longest == null -> initialValue
                longest.repeatMode == Animation.RepeatMode.PingPong -> longest.start
                else -> longest.end
            }
            anim.snapTo(snapValue)
            return@LaunchedEffect
        }
        if (seq >= 0L) playedSequences.add(seq)

        val firstStart = animations.minByOrNull { it.startDelayMillis }?.start
        if (firstStart != null && firstStart != anim.value) {
            anim.snapTo(firstStart)
        }
        while (true) {
            val step = activeRunner.next() ?: break
            val primitive = step.primitive

            if (primitive.isSnap) {
                if (primitive.delayMillis > 0) {
                    delay(primitive.delayMillis)
                }
                anim.snapTo(primitive.to)
            } else if (animationsDisabled) {
                delay(primitive.delayMillis.coerceAtLeast(500L))
                anim.snapTo(primitive.to)
            } else {
                anim.animateTo(
                    targetValue = primitive.to,
                    animationSpec = tween(
                        durationMillis = primitive.durationMillis,
                        delayMillis = primitive.delayMillis.toInt(),
                        easing = primitive.easing
                    )
                )
            }

            step.markPlayed()
        }
    }

    return remember { derivedStateOf { anim.value } }
}

internal class AnimationRunner<T>(
    private val animations: List<Animation<T>>,
) {
    private data class State<T>(
        val anim: Animation<T>,
        var nextStartTime: Long,
        var repeatCount: Int = 0,
        var reversePhase: Boolean = false,
        var needsReset: Boolean = false,
    ) {
        fun canRepeat(): Boolean =
            anim.repeatMaxCount == Int.MAX_VALUE || repeatCount < anim.repeatMaxCount

        fun nextPrimitive(elapsed: Long): Pair<AnimationPrimitive<T>, () -> Unit>? {
            if (!canRepeat()) return null

            val delay = (nextStartTime - elapsed).coerceAtLeast(0L)
            val to = when {
                anim.repeatMode == Animation.RepeatMode.PingPong && reversePhase -> anim.start
                anim.repeatMode == Animation.RepeatMode.Normal && needsReset -> anim.start
                else -> anim.end
            }
            val primitive = AnimationPrimitive(
                to = to,
                durationMillis = if (needsReset) 0 else anim.durationMillis,
                delayMillis = delay,
                easing = anim.easing,
                isSnap = needsReset
            )

            val advance = {
                val animationEndTime = elapsed + delay + primitive.durationMillis
                if (anim.repeatMode == Animation.RepeatMode.PingPong) {
                    reversePhase = !reversePhase
                    if (!reversePhase) {
                        repeatCount++
                        nextStartTime = animationEndTime + anim.repeatDelayMillis
                    } else {
                        nextStartTime = animationEndTime + anim.pingPongDelayMillis
                    }
                } else if (anim.repeatMode == Animation.RepeatMode.Normal) {
                    if (needsReset) {
                        needsReset = false
                        nextStartTime = animationEndTime
                    } else {
                        needsReset = true
                        repeatCount++
                        nextStartTime = animationEndTime + anim.repeatDelayMillis
                    }
                } else {
                    repeatCount++
                    nextStartTime = animationEndTime + anim.repeatDelayMillis
                }
            }

            return primitive to advance
        }
    }

    private val states = animations.map {
        State(
            anim = it,
            nextStartTime = it.startDelayMillis.toLong(),
        )
    }

    private var elapsed: Long = 0L

    data class Step<T>(
        val primitive: AnimationPrimitive<T>,
        val markPlayed: () -> Unit
    )

    fun next(): Step<T>? {
        while (true) {
            val available = states.mapNotNull { state ->
                val result = state.nextPrimitive(elapsed)
                result
            }

            if (available.isNotEmpty()) {
                val (primitive, advance) = available.minByOrNull { it.first.delayMillis } ?: return null

                elapsed += primitive.delayMillis
                advance()
                elapsed += primitive.durationMillis

                return Step(primitive, markPlayed = {})
            }

            val nextStart = states.minOfOrNull { it.nextStartTime } ?: return null
            if (nextStart > elapsed) {
                elapsed = nextStart
                continue
            }
            return null
        }
    }
}

internal data class AnimationPrimitive<T>(
    val to: T,
    val durationMillis: Int,
    val delayMillis: Long,
    val easing: Easing,
    val isSnap: Boolean = false,
)

@Composable
internal fun rememberOffsetProvider(baseProps: BaseProps): OffsetProvider {
    val offset = rememberBindableAnimatedValue(baseProps.offsetBehaviour, OffsetVectorConverter)
    return OffsetProvider(offset)
}

private val OffsetVectorConverter: TwoWayConverter<DpOffset, AnimationVector> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.y, it.x) },
        convertFromVector = {
            val vec = it as AnimationVector2D
            DpOffset(vec.v1, vec.v2)
        }
    )

private val DpVectorConverter: TwoWayConverter<Dp, AnimationVector> =
    TwoWayConverter(
        convertToVector = { AnimationVector1D(it.value) },
        convertFromVector = {
            val vec = it as AnimationVector1D
            vec.value.dp
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

@Composable
internal fun rememberBoxProvider(baseProps: BaseProps): BoxProvider {
    val width = rememberBindableAnimatedValue(baseProps.widthBehaviour(), DpVectorConverter)
    val height = rememberBindableAnimatedValue(baseProps.heightBehaviour(), DpVectorConverter)
    return BoxProvider(width, height)
}

@Composable
internal fun rememberScaleProvider(baseProps: BaseProps): ScaleProvider {
    val scale = rememberBindableAnimatedValue(baseProps.scaleBehaviour, ScaleVectorConverter)
    return ScaleProvider(scale)
}

@Composable
internal fun rememberRotationProvider(baseProps: BaseProps): RotationProvider {
    val rotation = rememberBindableAnimatedValue(baseProps.rotationBehaviour, RotationVectorConverter)
    return RotationProvider(rotation)
}

@Composable
internal fun rememberOpacityProvider(baseProps: BaseProps): OpacityProvider {
    val alpha = rememberBindableAnimatedValue(baseProps.opacityBehaviour, Float.VectorConverter)
    return OpacityProvider(alpha)
}

@Composable
internal fun rememberColorProvider(baseProps: BaseProps): State<Color> =
    rememberBindableAnimatedValue(baseProps.colorBehaviour(), Color.VectorConverter(ColorSpaces.Srgb))

@Composable
internal fun rememberBorderColorProvider(baseProps: BaseProps): State<Color> =
    rememberBindableAnimatedValue(baseProps.borderColorBehaviour(), Color.VectorConverter(ColorSpaces.Srgb))

@Composable
internal fun rememberBorderThicknessProvider(baseProps: BaseProps): State<Dp> =
    rememberBindableAnimatedValue(baseProps.borderThicknessBehaviour(), DpVectorConverter)

@Composable
internal fun rememberBorderGradientProvider(baseProps: BaseProps): BrushProvider =
    rememberGradientProvider(baseProps, Animation.Role.Border)

@Composable
internal fun rememberBlurProvider(baseProps: BaseProps): BlurProvider {
    val radius = rememberBindableAnimatedValue(baseProps.blurBehaviour, Float.VectorConverter)
    return BlurProvider(radius)
}

@Composable
internal fun rememberShadowProvider(baseProps: BaseProps): ShadowProvider {
    val color = rememberBindableAnimatedValue(baseProps.shadowColorBehaviour(), Color.VectorConverter(ColorSpaces.Srgb))
    val blurRadius = rememberBindableAnimatedValue(baseProps.shadowBlurRadiusBehaviour(), Float.VectorConverter)
    val offset = rememberBindableAnimatedValue(baseProps.shadowOffsetBehaviour(), OffsetVectorConverter)
    return ShadowProvider(color, blurRadius, offset)
}

@Composable
internal fun rememberInnerShadowProvider(baseProps: BaseProps): InnerShadowProvider {
    val color = rememberBindableAnimatedValue(baseProps.innerShadowColorBehaviour(), Color.VectorConverter(ColorSpaces.Srgb))
    val blurRadius = rememberBindableAnimatedValue(baseProps.innerShadowBlurRadiusBehaviour(), Float.VectorConverter)
    val offset = rememberBindableAnimatedValue(baseProps.innerShadowOffsetBehaviour(), OffsetVectorConverter)
    return InnerShadowProvider(color, blurRadius, offset)
}

internal val LocalOpacityProvider = androidx.compose.runtime.compositionLocalOf<OpacityProvider?> { null }
