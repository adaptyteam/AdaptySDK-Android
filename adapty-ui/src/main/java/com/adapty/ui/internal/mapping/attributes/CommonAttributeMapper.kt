@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.attributes

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.plus

@InternalAdaptyApi
public class CommonAttributeMapper {

    internal fun mapTransition(item: Map<*, *>): Transition? {
        val startDelay = (item["start_delay"] as? Number)?.toInt() ?: 0
        val duration = (item["duration"] as? Number)?.toInt() ?: 300
        val interpolatorName = (item["interpolator"] as? String) ?: "ease_in_out"

        return when(item["type"]) {
            "slide" -> Transition.Slide(duration, startDelay, interpolatorName)
            "fade" -> Transition.Fade(duration, startDelay, interpolatorName)
            else -> null
        }
    }

    internal fun mapAlign(item: Map<*, *>): Align =
        mapHorizontalAlign(item["h_align"]) + mapVerticalAlign(item["v_align"])

    internal fun mapVerticalAlign(item: Any?, default: VerticalAlign = VerticalAlign.CENTER): VerticalAlign {
        return when (item) {
            "top" -> VerticalAlign.TOP
            "bottom" -> VerticalAlign.BOTTOM
            "center" -> VerticalAlign.CENTER
            else -> default
        }
    }

    internal fun mapHorizontalAlign(item: Any?, default: HorizontalAlign = HorizontalAlign.CENTER): HorizontalAlign {
        return when (item) {
            "leading" -> HorizontalAlign.START
            "left" -> HorizontalAlign.LEFT
            "trailing" -> HorizontalAlign.END
            "right" -> HorizontalAlign.RIGHT
            "center" -> HorizontalAlign.CENTER
            else -> default
        }
    }

    internal fun mapDimUnit(item: Any): DimUnit {
        when (item) {
            is Number -> return DimUnit.Exact(item.toFloat())
            is Map<*, *> -> {
                if (item["safe_area"] == "start")
                    return DimUnit.SafeArea(DimUnit.SafeArea.Side.START)
                if (item["safe_area"] == "end")
                    return DimUnit.SafeArea(DimUnit.SafeArea.Side.END)
                val point = item["point"] as? Number
                if (point != null)
                    return DimUnit.Exact(point.toFloat())
                val screen = item["screen"] as? Number
                if (screen != null)
                    return DimUnit.ScreenFraction(screen.toFloat())
                val value = item["value"] as? Number ?: throw adaptyError(
                    message = "Unknown dimension format (${item["value"]?.javaClass})",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
                val unit = item["unit"]
                if (unit == "screen")
                    return DimUnit.ScreenFraction(value.toFloat())
                return DimUnit.Exact(value.toFloat())
            }
            else -> throw adaptyError(
                message = "Unknown dimension format (${item.javaClass})",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }

    internal fun mapDimSpec(item: Any, dimAxis: DimSpec.Axis): DimSpec {
        if (item !is Map<*, *>)
            return DimSpec.Specified(item.asDimUnit(), dimAxis)
        val min = item["min"]
        val max = item["max"]
        val shrink = item["shrink"]
        return when {
            item["fill_max"] == true -> DimSpec.FillMax(dimAxis)
            min != null -> DimSpec.Min(min.asDimUnit(), max?.asDimUnit(), dimAxis)
            shrink != null -> DimSpec.Shrink(shrink.asDimUnit(), max?.asDimUnit(), dimAxis)
            else -> DimSpec.Specified(item.asDimUnit(), dimAxis)
        }
    }

    private fun Any.asDimUnit() = mapDimUnit(this)

    internal fun mapEdgeEntities(item: Any): EdgeEntities? {
        when (item) {
            is Number -> return EdgeEntities(item.toFloat())
            is Map<*, *> -> {
                val start = item["leading"]?.asDimUnit() ?: DimUnit.Exact(0f)
                val top = item["top"]?.asDimUnit() ?: DimUnit.Exact(0f)
                val end = item["trailing"]?.asDimUnit() ?: DimUnit.Exact(0f)
                val bottom = item["bottom"]?.asDimUnit() ?: DimUnit.Exact(0f)

                return EdgeEntities(start, top, end, bottom)
            }
            is Collection<*> -> {
                val values = item.mapIndexedNotNull { i, value ->
                    value?.asDimUnit()
                }
                    .takeIf { it.any { value -> (value as? DimUnit.Exact)?.value != 0f } }
                    ?: return null
                if (values.size == 2) return EdgeEntities(values[0], values[1])
                return EdgeEntities(
                    values.getOrNull(0) ?: DimUnit.Exact(0f),
                    values.getOrNull(1) ?: DimUnit.Exact(0f),
                    values.getOrNull(2) ?: DimUnit.Exact(0f),
                    values.getOrNull(3) ?: DimUnit.Exact(0f),
                )
            }
            else -> throw adaptyError(
                message = "Unknown padding format (${item.javaClass})",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }

    public fun mapAspectRatio(item: Any?): AspectRatio {
        return when (item) {
            "fill" -> AspectRatio.FILL
            "stretch" -> AspectRatio.STRETCH
            else -> AspectRatio.FIT
        }
    }

    internal fun mapOffset(item: Any): Offset? {
        when (item) {
            is Number -> return Offset(item.toFloat(), 0f)
            is Map<*, *> -> {
                val y = (item["y"] as? Number)?.toFloat() ?: 0f
                val x = (item["x"] as? Number)?.toFloat() ?: 0f
                return Offset(y, x)
            }
            is Collection<*> -> {
                val numbers = item.mapNotNull {
                    (it as? Number)?.toFloat()
                }
                    .takeIf { it.any { value -> value != 0f } }
                    ?: return null
                return when (numbers.size) {
                    0 -> null
                    1 -> Offset(numbers[0])
                    2 -> Offset(numbers[0], numbers[1])
                    else -> throw adaptyError(
                        message = "Offset array size (${numbers.size}) exceeds 2!",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }
            }
            else -> throw adaptyError(
                message = "Unknown offset format (${item.javaClass})",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }

    internal fun mapShape(item: Any): Shape? {
        return when(item) {
            is Map<*, *> -> mapShape(item)
            is String -> mapShape(mapOf("background" to item, "type" to "rect"))
            else -> null
        }
    }

    private fun mapShape(item: Map<*, *>): Shape {
        val shapeType = (item["type"] as? String).let { type ->
            when (type) {
                "circle" -> Shape.Type.Circle
                "curve_up" -> Shape.Type.RectWithArc(Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
                "curve_down" -> Shape.Type.RectWithArc(-Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
                else -> {
                    val cornerRadius = item["rect_corner_radius"]?.let(::mapCornerRadius)
                    Shape.Type.Rectangle(cornerRadius)
                }
            }
        }
        val filling = (item["background"] as? String)?.let { assetId -> Shape.Fill(assetId) }
        val border = (item["border"] as? String)?.let { borderColor ->
            val thickness = (item["thickness"] as? Number)?.toFloat() ?: 1f
            if (thickness == 0f) return@let null
            Shape.Border(
                borderColor,
                shapeType,
                thickness,
            )
        }

        return Shape(filling, shapeType, border)
    }

    internal fun mapCornerRadius(item: Any): Shape.CornerRadius {
        when (item) {
            is Number -> return Shape.CornerRadius(item.toFloat())
            is Map<*, *> -> {
                val topStart = (item["top_leading"] as? Number)?.toFloat() ?: 0f
                val topEnd = (item["top_trailing"] as? Number)?.toFloat() ?: 0f
                val bottomEnd = (item["bottom_trailing"] as? Number)?.toFloat() ?: 0f
                val bottomStart = (item["bottom_leading"] as? Number)?.toFloat() ?: 0f

                return Shape.CornerRadius(topStart, topEnd, bottomEnd, bottomStart)
            }
            is Collection<*> -> {
                val numbers = item.mapNotNull {
                    (it as? Number)?.toFloat()
                }
                if (numbers.size == 1) return Shape.CornerRadius(numbers[0])
                return Shape.CornerRadius(
                    numbers.getOrNull(0) ?: 0f,
                    numbers.getOrNull(1) ?: 0f,
                    numbers.getOrNull(2) ?: 0f,
                    numbers.getOrNull(3) ?: 0f,
                )
            }
            else -> throw adaptyError(
                message = "Unknown corner radius format (${item.javaClass})",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }
}