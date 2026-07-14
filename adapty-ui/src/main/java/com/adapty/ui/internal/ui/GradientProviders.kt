@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui

import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import android.content.res.Resources
import androidx.compose.ui.graphics.lerp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.element.AnimationBehavior
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.BrushProvider
import com.adapty.ui.internal.ui.element.LocalActiveAnimations
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.pendingAppearAnimationsFor
import com.adapty.ui.internal.ui.element.gradientBehaviour
import com.adapty.ui.internal.ui.element.borderGradientBehaviour
import com.adapty.ui.internal.ui.attributes.adjustRadialColorStops
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toGradientAsset
import com.adapty.ui.internal.ui.attributes.Border
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.color
import com.adapty.ui.internal.utils.resolveAsset

@Composable
internal fun rememberGradientProvider(
    baseProps: BaseProps,
): BrushProvider {
    return rememberGradientProvider(baseProps, Animation.Role.Background)
}

@Composable
internal fun rememberGradientProvider(
    baseProps: BaseProps,
    role: Animation.Role
): BrushProvider {
    val animations = LocalActiveAnimations.current.value?.animations
        ?.filter { anim -> anim.role == role }
        ?.takeIf { it.isNotEmpty() }
        ?.sortedBy { it.startDelayMillis }

    return if (!animations.isNullOrEmpty()) {
        val brush = rememberAssetBasedGradientAnimation(animations, baseProps, role)
        BrushProvider(brush)
    } else {
        pendingAppearStartBrush(baseProps, role)?.let { startBrush ->
            return BrushProvider(rememberUpdatedState(startBrush))
        }
        val staticBehavior = when (role) {
            Animation.Role.Background -> baseProps.gradientBehaviour()
            Animation.Role.Border -> baseProps.borderGradientBehaviour()
            else -> AnimationBehavior.Static(Brush.color(Color.Transparent))
        }
        when (staticBehavior) {
            is AnimationBehavior.Static -> {
                val brush = rememberUpdatedState(staticBehavior.value)
                BrushProvider(brush)
            }
            else -> {
                val brush = rememberUpdatedState(Brush.color(Color.Transparent))
                BrushProvider(brush)
            }
        }
    }
}

@Composable
private fun pendingAppearStartBrush(baseProps: BaseProps, role: Animation.Role): Brush? {
    val firstPending = baseProps.pendingAppearAnimationsFor(role)?.firstOrNull() ?: return null
    val startValue = when (role) {
        Animation.Role.Background -> firstPending.start as? VisualValue
        Animation.Role.Border -> (firstPending.start as? Border)?.color
        else -> null
    }
    val startAsset = startValue?.resolveAsset<Asset.Filling.Local>() ?: return null
    startAsset.castOrNull<Asset.Gradient>()?.let { gradient ->
        return gradient.toComposeFill().shader
    }
    startAsset.castOrNull<Asset.Color>()?.let { color ->
        return Brush.color(color.toComposeFill().color)
    }
    return null
}

@Composable
private fun rememberAssetBasedGradientAnimation(
    animations: List<Animation<*>>,
    baseProps: BaseProps,
    role: Animation.Role
): State<Brush> {
    val assets = resolveAssets()
    
    val animatedProgress = rememberAnimatedValue(
        AnimationBehavior.Animated(
            animations.map { anim ->
                Animation(
                    0f, 1f,
                    anim.durationMillis,
                    anim.startDelayMillis,
                    anim.repeatDelayMillis,
                    anim.pingPongDelayMillis,
                    anim.interpolator,
                    anim.repeatMode,
                    anim.repeatMaxCount,
                    anim.role
                )
            }.filterIsInstance<Animation<Float>>(),
            0f
        ),
        Float.VectorConverter
    )

    val firstAnim = animations.firstOrNull()
    
    val (startValue, endValue) = when (role) {
        Animation.Role.Background -> {
            val startId = firstAnim?.start as? VisualValue
            val endId = firstAnim?.end as? VisualValue
            startId to endId
        }
        Animation.Role.Border -> {
            val startBorder = firstAnim?.start as? Border
            val endBorder = firstAnim?.end as? Border
            startBorder?.color to endBorder?.color
        }
        else -> null to null
    }

    val startAsset: Asset.Composite<Asset.Filling.Local>? = startValue?.resolveAsset()

    val endAsset: Asset.Composite<Asset.Filling.Local>? = endValue?.resolveAsset()

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
    
    val fallbackGradient = when (role) {
        Animation.Role.Background -> baseProps.shape?.fill?.resolveAsset<Asset.Filling.Local>()?.castOrNull<Asset.Gradient>()
        Animation.Role.Border -> baseProps.shape?.border?.color?.resolveAsset<Asset.Filling.Local>()?.castOrNull<Asset.Gradient>()
        else -> null
    }

    return remember {
        derivedStateOf {
            val progress = animatedProgress.value
            
            if (startGradient != null && endGradient != null) {
                createInterpolatedGradientFromAssets(startGradient, endGradient, progress)
            } else if (fallbackGradient != null) {
                fallbackGradient.toComposeFill().shader
            } else {
                Brush.color(Color.Transparent)
            }
        }
    }
}

private fun createInterpolatedGradientFromAssets(
    startGradient: Asset.Composite<Asset.Gradient>,
    endGradient: Asset.Composite<Asset.Gradient>,
    progress: Float
): Brush {
    val startData = startGradient.main
    val endData = endGradient.main
    
    if (startData.type == endData.type &&
        startData.values.size == endData.values.size) {
        
        if (startData.values.size < 2) {
            val startColor = startData.values.firstOrNull()?.let { Color(it.color.value) } ?: Color.Transparent
            val endColor = endData.values.firstOrNull()?.let { Color(it.color.value) } ?: Color.Transparent
            return Brush.color(lerp(startColor, endColor, progress))
        }

        return object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val startColors = startData.values.map { Color(it.color.value) }
                val endColors = endData.values.map { Color(it.color.value) }
                val startStops = startData.values.map { it.p }
                val endStops = endData.values.map { it.p }
                
                val interpolatedColors = startColors.zip(endColors) { startColor, endColor ->
                    lerp(startColor, endColor, progress)
                }
                
                val interpolatedStops = startStops.zip(endStops) { startStop, endStop ->
                    startStop + (endStop - startStop) * progress
                }
                
                val (x0, y0, x1, y1) = startData.points
                return when (startData.type) {
                    Asset.Gradient.Type.LINEAR -> {
                        LinearGradientShader(
                            from = Offset(size.width * x0, size.height * y0),
                            to = Offset(size.width * x1, size.height * y1),
                            colors = interpolatedColors,
                            colorStops = interpolatedStops
                        )
                    }
                    Asset.Gradient.Type.RADIAL -> {
                        val density = Resources.getSystem().displayMetrics.density
                        val center = Offset(size.width * x0, size.height * y0)
                        val (radiusPx, adjustedStops) = adjustRadialColorStops(
                            interpolatedStops.zip(interpolatedColors).toTypedArray(),
                            x1 * density,
                            y1 * density,
                        )
                        RadialGradientShader(
                            center = center,
                            radius = radiusPx,
                            colors = adjustedStops.map { it.second },
                            colorStops = adjustedStops.map { it.first },
                        )
                    }
                    Asset.Gradient.Type.CONIC -> {
                        SweepGradientShader(
                            center = Offset(size.width * x0, size.height * y0),
                            colors = interpolatedColors,
                            colorStops = interpolatedStops
                        )
                    }
                }
            }
        }
    } else {
        return CrossFadeGradientBrush(startGradient, endGradient, progress)
    }
}

internal class CrossFadeGradientBrush(
    private val startGradient: Asset.Composite<Asset.Gradient>,
    private val endGradient: Asset.Composite<Asset.Gradient>,
    private val progress: Float
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        return (startGradient.toComposeFill().shader as ShaderBrush).createShader(size)
    }
    
    val startBrush: Brush get() = startGradient.toComposeFill().shader
    val endBrush: Brush get() = endGradient.toComposeFill().shader
    val crossFadeProgress: Float get() = progress
    val isCrossFade: Boolean get() = true
} 