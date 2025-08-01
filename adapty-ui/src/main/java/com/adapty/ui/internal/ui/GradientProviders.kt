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
import androidx.compose.ui.graphics.lerp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.element.AnimationBehavior
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.BrushProvider
import com.adapty.ui.internal.ui.element.ResolveAssets
import com.adapty.ui.internal.ui.element.gradientBehaviour
import com.adapty.ui.internal.ui.element.borderGradientBehaviour
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toGradientAsset
import com.adapty.ui.internal.ui.attributes.Border
import com.adapty.ui.internal.utils.getAsset

@Composable
internal fun rememberGradientProvider(
    baseProps: BaseProps,
    resolveAssets: ResolveAssets
): BrushProvider {
    return rememberGradientProvider(baseProps, resolveAssets, Animation.Role.Background)
}

@Composable
internal fun rememberGradientProvider(
    baseProps: BaseProps,
    resolveAssets: ResolveAssets,
    role: Animation.Role
): BrushProvider {
    val animations = baseProps.onAppear
        ?.filter { anim -> anim.role == role }
        ?.takeIf { it.isNotEmpty() }
        ?.sortedBy { it.startDelayMillis }

    return if (!animations.isNullOrEmpty()) {
        val brush = rememberAssetBasedGradientAnimation(animations, baseProps, resolveAssets, role)
        BrushProvider(brush)
    } else {
        val staticBehavior = when (role) {
            Animation.Role.Background -> baseProps.gradientBehaviour(resolveAssets)
            Animation.Role.Border -> baseProps.borderGradientBehaviour(resolveAssets)
            else -> AnimationBehavior.Static(Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
        }
        when (staticBehavior) {
            is AnimationBehavior.Static -> {
                val brush = rememberUpdatedState(staticBehavior.value)
                BrushProvider(brush)
            }
            else -> {
                val brush = rememberUpdatedState(Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                BrushProvider(brush)
            }
        }
    }
}

@Composable
private fun rememberAssetBasedGradientAnimation(
    animations: List<Animation<*>>,
    baseProps: BaseProps,
    resolveAssets: ResolveAssets,
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
    
    val (startAssetId, endAssetId) = when (role) {
        Animation.Role.Background -> {
            val startId = firstAnim?.start as? String
            val endId = firstAnim?.end as? String
            startId to endId
        }
        Animation.Role.Border -> {
            val startBorder = firstAnim?.start as? Border
            val endBorder = firstAnim?.end as? Border
            startBorder?.color to endBorder?.color
        }
        else -> null to null
    }
    
    val startAsset = startAssetId?.let { assets.getAsset<Asset.Filling.Local>(it) }
    val endAsset = endAssetId?.let { assets.getAsset<Asset.Filling.Local>(it) }

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
        Animation.Role.Background -> baseProps.shape?.fill?.assetId?.let { assetId ->
            val fallbackAsset = assets.getAsset<Asset.Filling.Local>(assetId)
            fallbackAsset?.castOrNull<Asset.Gradient>()
        }
        Animation.Role.Border -> baseProps.shape?.border?.color?.let { assetId ->
            val fallbackAsset = assets.getAsset<Asset.Filling.Local>(assetId)
            fallbackAsset?.castOrNull<Asset.Gradient>()
        }
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
                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
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
                        val center = Offset(size.width * x0, size.height * y0)
                        val radius = (Offset(size.width * x1, size.height * y1) - center).getDistance()
                        RadialGradientShader(
                            center = center,
                            radius = radius,
                            colors = interpolatedColors,
                            colorStops = interpolatedStops
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