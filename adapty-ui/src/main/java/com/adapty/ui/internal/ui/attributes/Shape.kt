@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.CircleShape
import com.adapty.ui.internal.ui.RectWithArcShape
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.color
import com.adapty.ui.internal.utils.getBitmap
import kotlin.math.roundToInt

@InternalAdaptyApi
public class Shape internal constructor(
    internal val fill: VisualValue?,
    internal val type: Type,
    internal val border: Border?,
    internal val shadow: Shadow?,
    internal val innerShadow: Shadow?,
    internal val blurRadius: Float? = null,
) {
    public sealed class Type {
        public class Rectangle internal constructor(internal val cornerRadius: CornerRadius?): Type()
        public object Circle: Type()
        public class RectWithArc internal constructor(internal val arcHeight: Float): Type() {
            internal companion object {
                const val ABS_ARC_HEIGHT = 32f
            }
        }
    }

    internal class CornerRadius(
        val topLeft: Float,
        val topRight: Float,
        val bottomRight: Float,
        val bottomLeft: Float,
    ) {
        constructor(value: Float): this(value, value, value, value)
    }

    internal class Border(
        val color: VisualValue,
        val shapeType: Type,
        val thickness: Float,
    )

    internal companion object {
        fun plain(assetId: String) =
            Shape(fill = VisualValue.assetId(assetId), type = Type.Rectangle(null), border = null, shadow = null, innerShadow = null)

        fun plainAny(value: String) =
            Shape(fill = VisualValue.any(StringSource.Value(value)), type = Type.Rectangle(null), border = null, shadow = null, innerShadow = null)
    }
}

internal sealed class ComposeFill {
    class Color(val color: androidx.compose.ui.graphics.Color): ComposeFill()
    class Gradient(val shader: Brush): ComposeFill()
    class Image(val image: Bitmap, val matrix: Matrix, val paint: Paint): ComposeFill()
}

internal fun Asset.Composite<Asset.Color>.toComposeFill(): ComposeFill.Color {
    return ComposeFill.Color(Color(this.main.value))
}

internal fun Asset.Composite<Asset.Color>.toGradientAsset(
    targetGradient: Asset.Composite<Asset.Gradient>
): Asset.Composite<Asset.Gradient> {
    val color = this.main
    val target = targetGradient.main
    
    val gradientAsset = Asset.Gradient(
        type = target.type,
        values = target.values.map { Asset.Gradient.Value(it.p, color) },
        points = target.points,
        customId = color.customId
    )
    
    return Asset.Composite(gradientAsset, this.fallback?.let { fallbackColor ->
        val fallbackTarget = targetGradient.fallback ?: target
        Asset.Gradient(
            type = fallbackTarget.type,
            values = fallbackTarget.values.map { Asset.Gradient.Value(it.p, fallbackColor) },
            points = fallbackTarget.points,
            customId = fallbackColor.customId
        )
    })
}

internal fun Asset.Composite<Asset.Color>.toGradientAsset(): Asset.Composite<Asset.Gradient> {
    val stubGradientAsset = Asset.Composite(
        main = Asset.Gradient(
            type = Asset.Gradient.Type.LINEAR,
            values = listOf(
                Asset.Gradient.Value(0f, Asset.Color(android.graphics.Color.TRANSPARENT)),
                Asset.Gradient.Value(1f, Asset.Color(android.graphics.Color.TRANSPARENT))
            ),
            points = Asset.Gradient.Points(0f, 0f, 1f, 1f),
        )
    )
    
    return toGradientAsset(stubGradientAsset)
}

internal fun Asset.Composite<Asset.Gradient>.toComposeFill(): ComposeFill.Gradient {
    val gradient = this.main
    val colorStops = gradient.values.map { (point, color) -> point to Color(color.value) }.toTypedArray()

    if (colorStops.size < 2) {
        return ComposeFill.Gradient(Brush.color(colorStops.firstOrNull()?.second ?: Color.Transparent))
    }

    val (x0, y0, x1, y1) = gradient.points
    val shader = when (gradient.type) {
        Asset.Gradient.Type.LINEAR -> {
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val from = Offset(size.width * x0, size.height * y0)
                    val to = Offset(size.width * x1, size.height * y1)

                    return LinearGradientShader(
                        from = from,
                        to = to,
                        colorStops = colorStops.map { it.first },
                        colors = colorStops.map { it.second },
                    )
                }
            }
        }
        Asset.Gradient.Type.RADIAL -> {
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val density = Resources.getSystem().displayMetrics.density
                    val center = Offset(size.width * x0, size.height * y0)
                    val (radiusPx, adjustedStops) = adjustRadialColorStops(colorStops, x1 * density, y1 * density)
                    return RadialGradientShader(
                        center = center,
                        radius = radiusPx,
                        colorStops = adjustedStops.map { it.first },
                        colors = adjustedStops.map { it.second },
                    )
                }
            }
        }
        Asset.Gradient.Type.CONIC -> {
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val center = Offset(size.width * x0, size.height * y0)
                    return SweepGradientShader(
                        center = center,
                        colorStops = colorStops.map { it.first },
                        colors = colorStops.map { it.second },
                    )
                }
            }
        }
    }
    return ComposeFill.Gradient(shader)
}

internal fun adjustRadialColorStops(
    colorStops: Array<Pair<Float, Color>>,
    startRadiusPx: Float,
    endRadiusPx: Float,
): Pair<Float, List<Pair<Float, Color>>> {
    val fallbackRadius = endRadiusPx.coerceAtLeast(MIN_RADIAL_RADIUS_PX)
    if (colorStops.isEmpty()) return fallbackRadius to emptyList()
    if (startRadiusPx == endRadiusPx) {
        val edge = colorStops.last().second
        return fallbackRadius to listOf(0f to edge, 1f to edge)
    }
    val shaderRadius = maxOf(startRadiusPx, endRadiusPx).coerceAtLeast(MIN_RADIAL_RADIUS_PX)
    var mapped = colorStops.map { (stop, color) ->
        (startRadiusPx + stop * (endRadiusPx - startRadiusPx)) / shaderRadius to color
    }
    if (endRadiusPx < startRadiusPx) mapped = mapped.reversed()
    val result = mutableListOf<Pair<Float, Color>>()
    for ((i, pair) in mapped.withIndex()) {
        val (stop, color) = pair
        if (stop <= 0f) {
            val next = mapped.getOrNull(i + 1) ?: continue
            if (next.first <= 0f) continue
            result.add(0f to lerp(color, next.second, (0f - stop) / (next.first - stop)))
        } else {
            if (result.isEmpty()) result.add(0f to color)
            result.add(stop to color)
        }
    }
    if (result.isEmpty()) {
        val edge = mapped.last().second
        return shaderRadius to listOf(0f to edge, 1f to edge)
    }
    return shaderRadius to result
}

private const val MIN_RADIAL_RADIUS_PX = 0.001f

internal fun Asset.Composite<Asset.Image>.toComposeFill(context: Context, size: Size): ComposeFill.Image? {
    if (!(size.width > 0 && size.height > 0))
        return null

    val image = getBitmap(context, this, size.width.roundToInt(), size.height.roundToInt(), Asset.Image.ScaleType.FIT_MAX)
        ?: return null

    if (!(image.width > 0 && image.height > 0))
        return null

    val paint = Paint()
    val matrix = Matrix()

    val scale = kotlin.math.max(
        size.width / image.width,
        size.height / image.height
    )

    matrix.reset()
    matrix.setScale(scale, scale)
    matrix.postTranslate(
        (size.width - image.width * scale) / 2f,
        0f,
    )

    return ComposeFill.Image(image, matrix, paint)
}

@Composable
internal fun Shape.Type.toComposeShape(): androidx.compose.ui.graphics.Shape {
    return when (this) {
        is Shape.Type.Circle -> CircleShape
        is Shape.Type.RectWithArc -> RectWithArcShape(with(LocalDensity.current) { arcHeight.dp.toPx() })
        is Shape.Type.Rectangle -> {
            val radius = cornerRadius
            if (radius != null)
                RoundedCornerShape(radius.topLeft.dp, radius.topRight.dp, radius.bottomRight.dp, radius.bottomLeft.dp)
            else
                RectangleShape
        }
    }
}