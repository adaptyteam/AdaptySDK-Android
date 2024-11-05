@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.ui.CircleShape
import com.adapty.ui.internal.ui.RectWithArcShape
import com.adapty.ui.internal.utils.getBitmap
import kotlin.math.roundToInt

@InternalAdaptyApi
public class Shape internal constructor(
    internal val fill: Fill?,
    internal val type: Type,
    internal val border: Border?,
) {
    internal class Fill(val assetId: String)

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
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
    ) {
        val topLeft = topLeft * MULT
        val topRight = topRight * MULT
        val bottomRight = bottomRight * MULT
        val bottomLeft = bottomLeft * MULT

        constructor(value: Float): this(value, value, value, value)

        private companion object {
            const val MULT = 2
        }
    }

    internal class Border(
        val color: String,
        val shapeType: Type,
        val thickness: Float,
    )

    internal companion object {
        fun plain(assetId: String) =
            Shape(fill = Fill(assetId), type = Type.Rectangle(null), border = null)
    }
}

internal sealed class ComposeFill {
    class Color(val color: androidx.compose.ui.graphics.Color): ComposeFill()
    class Gradient(val shader: Brush): ComposeFill()
    class Image(val image: Bitmap, val matrix: Matrix, val paint: Paint): ComposeFill()
}

internal fun Asset.Color.toComposeFill(): ComposeFill.Color {
    return ComposeFill.Color(Color(this.value))
}

internal fun Asset.Gradient.toComposeFill(): ComposeFill.Gradient {
    val colorStops = this.values.map { (point, color) -> point to Color(color.value) }.toTypedArray()
    val (x0, y0, x1, y1) = this.points
    val shader = when (this.type) {
        Asset.Gradient.Type.LINEAR -> Brush.linearGradient(
            colorStops = *colorStops,
            start = Offset(x = x0, y = y0),
            end = Offset(x = x1, y = y1),
        )
        Asset.Gradient.Type.RADIAL -> Brush.radialGradient(*colorStops)
        Asset.Gradient.Type.CONIC -> Brush.sweepGradient(*colorStops)
    }
    return ComposeFill.Gradient(shader)
}

internal fun Asset.Image.toComposeFill(size: Size): ComposeFill.Image? {
    if (!(size.width > 0 && size.height > 0))
        return null

    val image = getBitmap(this, size.width.roundToInt(), size.height.roundToInt(), Asset.Image.ScaleType.FIT_MAX)
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
                RoundedCornerShape(radius.topLeft, radius.topRight, radius.bottomRight, radius.bottomLeft)
            else
                RectangleShape
        }
    }
}