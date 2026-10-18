@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.rememberPlatformContext
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.script.toJsFloat
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.toComposeFill
import com.adapty.ui.internal.ui.attributes.toEasing
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.utils.OneWayBinding
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.resolveAsset
import kotlinx.coroutines.delay

@InternalAdaptyUiApi
public class RadialProgressElement internal constructor(
    internal val assetId: VisualValue,
    internal val value: OneWayBinding,
    internal val durationMillis: Int,
    internal val min: Float,
    internal val max: Float,
    internal val skipAnimationOnOverflow: Boolean,
    internal val thickness: Float?,
    internal val sweepAngle: Float,
    internal val startAngle: Float,
    internal val clockwise: Boolean,
    internal val roundedCaps: Boolean,
    internal val clip: Boolean,
    internal val interpolator: Interpolator,
    internal val actions: List<Action>,
    override val baseProps: BaseProps,
) : UIElement {

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
            label = "radial_progress",
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
        val fillColor = fillAsset?.castOrNull<Asset.Color>()?.toComposeFill()?.color ?: Color.Transparent
        val fillBrush = fillAsset?.castOrNull<Asset.Gradient>()?.toComposeFill()?.shader
        val fillImage = fillAsset?.castOrNull<Asset.Image>()
        val context = rememberPlatformContext()

        val currentSweep = sweepAngle * animatedValue
        val directedSweep = if (clockwise) currentSweep else -currentSweep
        val thicknessDp = thickness

        val canvasModifier = if (clip) modifier.clipToBounds() else modifier

        Canvas(modifier = canvasModifier.fillMaxSize()) {
            val minDim = minOf(size.width, size.height)
            if (thicknessDp != null) {
                val strokeWidthPx = thicknessDp * density
                val arcSize = minDim - strokeWidthPx
                if (arcSize <= 0f) return@Canvas
                val topLeft = Offset(
                    (size.width - arcSize) / 2f,
                    (size.height - arcSize) / 2f,
                )
                val cap = if (roundedCaps && currentSweep < 360f) StrokeCap.Round else StrokeCap.Butt
                val style = Stroke(width = strokeWidthPx, cap = cap)

                val imageFill = fillImage?.toComposeFill(context, size)
                if (imageFill != null || fillBrush != null) {
                    drawRadialProgress(
                        topLeft = topLeft,
                        diameter = arcSize,
                        startAngle = startAngle,
                        sweepAngle = directedSweep,
                        strokeWidth = strokeWidthPx,
                        roundCaps = cap == StrokeCap.Round,
                        brush = fillBrush,
                        imageFill = imageFill,
                    )
                } else {
                    drawArc(
                        color = fillColor,
                        startAngle = startAngle,
                        sweepAngle = directedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = style,
                    )
                }
            } else {
                val topLeft = Offset(
                    (size.width - minDim) / 2f,
                    (size.height - minDim) / 2f,
                )

                val imageFill = fillImage?.toComposeFill(context, size)
                if (imageFill != null) {
                    drawRadialProgress(
                        topLeft = topLeft,
                        diameter = minDim,
                        startAngle = startAngle,
                        sweepAngle = directedSweep,
                        strokeWidth = null,
                        roundCaps = false,
                        brush = null,
                        imageFill = imageFill,
                    )
                } else if (fillBrush != null) {
                    drawArc(
                        brush = fillBrush,
                        startAngle = startAngle,
                        sweepAngle = directedSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = Size(minDim, minDim),
                        style = Fill,
                    )
                } else {
                    drawArc(
                        color = fillColor,
                        startAngle = startAngle,
                        sweepAngle = directedSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = Size(minDim, minDim),
                        style = Fill,
                    )
                }
            }
        }
    }
}
