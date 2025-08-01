@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.attributes

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.ui.attributes.Align
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.Interpolator
import com.adapty.ui.internal.ui.attributes.Offset
import com.adapty.ui.internal.ui.attributes.Point
import com.adapty.ui.internal.ui.attributes.Rotation
import com.adapty.ui.internal.ui.attributes.Scale
import com.adapty.ui.internal.ui.attributes.Shape
import com.adapty.ui.internal.ui.attributes.Transition
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.attributes.plus
import com.adapty.ui.internal.ui.attributes.Border
import com.adapty.ui.internal.ui.attributes.Box
import com.adapty.ui.internal.ui.attributes.Shadow

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

    internal fun mapFadeTransitionToAnimation(item: Map<*, *>): Animation<Float>? {
        if (item["type"] != "fade") return null

        val startDelay = (item["start_delay"] as? Number)?.toInt() ?: 0
        val duration = (item["duration"] as? Number)?.toInt() ?: 300
        val interpolator = mapInterpolator(item["interpolator"])

        return Animation(
            0f,
            1f,
            duration,
            startDelay,
            0,
            0,
            interpolator,
            null,
            1,
            Animation.Role.Opacity,
        )
    }

    private fun mapInterpolator(item: Any?): Interpolator =
        when (item) {
            is String -> Interpolator.Named(item)
            is Collection<*> -> {
                val values = item.mapIndexedNotNull { i, value ->
                    (value as? Number)?.toFloat()
                }
                if (values.size < 4) throw adaptyError(
                    message = "Cubic Bezier values less than 4",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
                Interpolator.CubicBezier(values[0], values[1], values[2], values[3])
            }
            else -> Interpolator.Named("ease_in_out")
        }

    internal fun mapAnimation(item: Map<*, *>): Animation<*>? {
        val startDelay = (item["start_delay"] as? Number)?.toInt() ?: 0
        val loopDelay = (item["loop_delay"] as? Number)?.toInt() ?: 0
        val pingPongDelay = (item["ping_pong_delay"] as? Number)?.toInt() ?: 0
        val duration = (item["duration"] as? Number)?.toInt() ?: 300
        val interpolator = mapInterpolator(item["interpolator"])

        val repeatMode = (item["loop"] as? String)?.let { loop ->
            when (loop) {
                "normal" -> Animation.RepeatMode.Normal
                "ping_pong" -> Animation.RepeatMode.PingPong
                else -> null
            }
        }

        val repeatMaxCount = if (repeatMode == null) {
            1
        } else {
            (item["loop_count"] as? Number)?.toInt() ?: Int.MAX_VALUE
        }

        return when (item["type"]) {
            "opacity" -> {
                var start: Float = 0f
                var end: Float = 1f
                (item["opacity"] as? Map<*, *>)?.let { spec ->
                    (spec["start"] as? Number)?.toFloat()?.let { start = it }
                    (spec["end"] as? Number)?.toFloat()?.let { end = it }
                }
                Animation(
                    start,
                    end,
                    duration,
                    startDelay,
                    loopDelay,
                    pingPongDelay,
                    interpolator,
                    repeatMode,
                    repeatMaxCount,
                    Animation.Role.Opacity,
                )
            }
            "offset" -> {
                (item["offset"] as? Map<*, *>)?.let { spec ->
                    val start = (spec["start"])?.let(::mapOffset) ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'offset' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                    val end = (spec["end"])?.let(::mapOffset) ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'offset' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )

                    Animation(
                        start,
                        end,
                        duration,
                        startDelay,
                        loopDelay,
                        pingPongDelay,
                        interpolator,
                        repeatMode,
                        repeatMaxCount,
                        Animation.Role.Offset,
                    )
                } ?: throw adaptyError(
                    message = "Couldn't find 'offset' spec for 'offset' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
            "scale" -> {
                val anchor =item["anchor"]?.let(::mapPoint) ?: Point.NormalizedCenter
                (item["scale"] as? Map<*, *>)?.let { spec ->
                    val start = (spec["start"])?.let(::mapScalePoint) ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'scale' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                    val end = (spec["end"])?.let(::mapScalePoint) ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'scale' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )

                    Animation(
                        Scale(start, anchor),
                        Scale(end, anchor),
                        duration,
                        startDelay,
                        loopDelay,
                        pingPongDelay,
                        interpolator,
                        repeatMode,
                        repeatMaxCount,
                        Animation.Role.Scale,
                    )
                } ?: throw adaptyError(
                    message = "Couldn't find 'scale' spec for 'scale' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
            "rotation" -> {
                val anchor =item["anchor"]?.let(::mapPoint) ?: Point.NormalizedCenter
                (item["angle"] as? Map<*, *>)?.let { spec ->
                    val start = (spec["start"] as? Number)?.toFloat() ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'rotation' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                    val end = (spec["end"] as? Number)?.toFloat() ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'rotation' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )

                    Animation(
                        Rotation(start, anchor),
                        Rotation(end, anchor),
                        duration,
                        startDelay,
                        loopDelay,
                        pingPongDelay,
                        interpolator,
                        repeatMode,
                        repeatMaxCount,
                        Animation.Role.Rotation,
                    )
                } ?: throw adaptyError(
                    message = "Couldn't find 'angle' spec for 'rotation' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
            "background" -> {
                (item["color"] as? Map<*, *>)?.let { spec ->
                    val start = spec["start"] as? String ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'background' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                    val end = spec["end"] as? String ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'background' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )

                    Animation(
                        start,
                        end,
                        duration,
                        startDelay,
                        loopDelay,
                        pingPongDelay,
                        interpolator,
                        repeatMode,
                        repeatMaxCount,
                        Animation.Role.Background,
                    )
                } ?: throw adaptyError(
                    message = "Couldn't find 'color' spec for 'background' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
            "border" -> {
                val color = item["color"] as? Map<*, *>
                val thickness = item["thickness"] as? Map<*, *>
                val start = Border(
                    color?.get("start") as? String,
                    (thickness?.get("start") as? Number)?.toFloat(),
                )
                val end = Border(
                    color?.get("end") as? String,
                    (thickness?.get("end") as? Number)?.toFloat(),
                )
                Animation(
                    start,
                    end,
                    duration,
                    startDelay,
                    loopDelay,
                    pingPongDelay,
                    interpolator,
                    repeatMode,
                    repeatMaxCount,
                    Animation.Role.Border,
                )
            }
            "box" -> {
                val widthSpec = item["width"] as? Map<*, *>
                val heightSpec = item["height"] as? Map<*, *>
                val start = Box(
                    widthSpec?.get("start")?.let(::mapDimUnit),
                    heightSpec?.get("start")?.let(::mapDimUnit),
                )
                val end = Box(
                    widthSpec?.get("end")?.let(::mapDimUnit),
                    heightSpec?.get("end")?.let(::mapDimUnit),
                )
                Animation(
                    start,
                    end,
                    duration,
                    startDelay,
                    loopDelay,
                    pingPongDelay,
                    interpolator,
                    repeatMode,
                    repeatMaxCount,
                    Animation.Role.Box,
                )
            }
            "shadow" -> {
                val color = item["color"] as? Map<*, *>
                val blurRadius = item["blur_radius"] as? Map<*, *>
                val offset = item["offset"] as? Map<*, *>
                val start = Shadow(
                    color?.let {
                        it["start"] as? String ?: throw adaptyError(
                            message = "Couldn't find 'start' for 'shadow color' animation",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    },
                    blurRadius?.let {
                        (it["start"] as? Number)?.toFloat() ?: throw adaptyError(
                            message = "Couldn't find 'start' for 'shadow blur radius' animation",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    },
                    offset?.let {
                        it["start"]?.let(::mapOffset) ?: throw adaptyError(
                            message = "Couldn't find 'start' for 'shadow offset' animation",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    },
                )
                val end = Shadow(
                    color?.let {
                        it["end"] as? String ?: throw adaptyError(
                            message = "Couldn't find 'end' for 'shadow color' animation",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    },
                    blurRadius?.let {
                        (it["end"] as? Number)?.toFloat() ?: throw adaptyError(
                            message = "Couldn't find 'end' for 'shadow blur radius' animation",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    },
                    offset?.let {
                        it["end"]?.let(::mapOffset) ?: throw adaptyError(
                            message = "Couldn't find 'end' for 'shadow offset' animation",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    },
                )
                Animation(
                    start,
                    end,
                    duration,
                    startDelay,
                    loopDelay,
                    pingPongDelay,
                    interpolator,
                    repeatMode,
                    repeatMaxCount,
                    Animation.Role.Shadow,
                )
            }
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
            is Number -> return Offset(item.asDimUnit())
            is Map<*, *> -> {
                val y = item["y"]?.asDimUnit() ?: DimUnit.Exact(0f)
                val x = item["x"]?.asDimUnit() ?: DimUnit.Exact(0f)
                return Offset(y, x)
            }
            is Collection<*> -> {
                val numbers = item.mapIndexedNotNull { i, value ->
                    value?.asDimUnit()
                }
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

    private fun mapPoint(item: Any): Point? {
        when (item) {
            is Number -> return Point(item.toFloat(), 0f)
            is Map<*, *> -> {
                val y = (item["y"] as? Number)?.toFloat() ?: 0f
                val x = (item["x"] as? Number)?.toFloat() ?: 0f
                return Point(y, x)
            }
            is Collection<*> -> {
                val numbers = item.mapNotNull {
                    (it as? Number)?.toFloat()
                }
                return when (numbers.size) {
                    0 -> null
                    1 -> Point(numbers[0])
                    2 -> Point(numbers[0], numbers[1])
                    else -> throw adaptyError(
                        message = "Point array size (${numbers.size}) exceeds 2!",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }
            }
            else -> throw adaptyError(
                message = "Unknown point format (${item.javaClass})",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }

    private fun mapScalePoint(item: Any): Point {
        when (item) {
            is Number -> return Point(item.toFloat(), item.toFloat())
            is Map<*, *> -> {
                val y = (item["y"] as? Number)?.toFloat() ?: 1f
                val x = (item["x"] as? Number)?.toFloat() ?: 1f
                return Point(y, x)
            }
            is Collection<*> -> {
                val numbers = item.mapNotNull {
                    (it as? Number)?.toFloat()
                }
                return when (numbers.size) {
                    0 -> Point.One
                    1 -> Point(numbers[0], numbers[0])
                    2 -> Point(numbers[0], numbers[1])
                    else -> throw adaptyError(
                        message = "Scale point array size (${numbers.size}) exceeds 2!",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }
            }
            else -> throw adaptyError(
                message = "Unknown scale point format (${item.javaClass})",
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
        val shadow = (item["shadow"] as? Map<*, *>)?.let { shadow ->
            Shadow(
                shadow["color"] as? String ?: return@let null,
                (shadow["blur_radius"] as? Number)?.toFloat() ?: 0f,
                shadow["offset"]?.let(::mapOffset) ?: Offset.Default,
            )
        }
        return Shape(filling, shapeType, border, shadow)
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