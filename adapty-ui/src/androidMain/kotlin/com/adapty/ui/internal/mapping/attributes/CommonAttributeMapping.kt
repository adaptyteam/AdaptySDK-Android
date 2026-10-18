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
import com.adapty.ui.internal.utils.OneWayBinding
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.TwoWayBinding
import kotlin.collections.get

internal fun Map<*, *>.toTransition(): Transition {
    val startDelay = (this["start_delay"] as? Number)?.toFloat() ?: 0f
    val duration = (this["duration"] as? Number)?.toFloat() ?: 300f
    val interpolator = this["interpolator"].toInterpolator()

    return Transition(duration, startDelay, interpolator)
}

internal fun Any?.toInterpolator(): Interpolator =
    when (this) {
        is String -> Interpolator.Named(this)
        is Collection<*> -> {
            val values = this.mapIndexedNotNull { i, value ->
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

internal fun Map<*, *>.toAnimation(): Animation<*>? {
    val startDelay = (this["start_delay"] as? Number)?.toInt() ?: 0
    val loopDelay = (this["loop_delay"] as? Number)?.toInt() ?: 0
    val pingPongDelay = (this["ping_pong_delay"] as? Number)?.toInt() ?: 0
    val duration = (this["duration"] as? Number)?.toInt() ?: 300
    val interpolator = this["interpolator"].toInterpolator()

    val repeatMode = (this["loop"] as? String)?.let { loop ->
        when (loop) {
            "normal" -> Animation.RepeatMode.Normal
            "ping_pong" -> Animation.RepeatMode.PingPong
            else -> null
        }
    }

    val repeatMaxCount = if (repeatMode == null) {
        1
    } else {
        (this["loop_count"] as? Number)?.toInt() ?: Int.MAX_VALUE
    }

    return when (this["type"]) {
        "opacity" -> {
            var start: Float = 0f
            var end: Float = 1f
            (this["opacity"] as? Map<*, *>)?.let { spec ->
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
            (this["offset"] as? Map<*, *>)?.let { spec ->
                val start = (spec["start"])?.toOffset() ?: throw adaptyError(
                    message = "Couldn't find 'start' for 'offset' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
                val end = (spec["end"])?.toOffset() ?: throw adaptyError(
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
            val anchor = this["anchor"]?.toPoint() ?: Point.NormalizedCenter
            (this["scale"] as? Map<*, *>)?.let { spec ->
                val start = (spec["start"])?.toScalePoint() ?: throw adaptyError(
                    message = "Couldn't find 'start' for 'scale' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
                val end = (spec["end"])?.toScalePoint() ?: throw adaptyError(
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
            val anchor = this["anchor"]?.toPoint() ?: Point.NormalizedCenter
            (this["angle"] as? Map<*, *>)?.let { spec ->
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
            (this["color"] as? Map<*, *>)?.let { spec ->
                val start = spec["start"]?.toVisualValue() ?: throw adaptyError(
                    message = "Couldn't find 'start' for 'background' animation",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
                val end = spec["end"]?.toVisualValue() ?: throw adaptyError(
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
            val color = this["color"] as? Map<*, *>
            val thickness = this["thickness"] as? Map<*, *>
            val start = Border(
                color?.get("start")?.toVisualValue(),
                (thickness?.get("start") as? Number)?.toFloat(),
            )
            val end = Border(
                color?.get("end")?.toVisualValue(),
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
            val widthSpec = this["width"] as? Map<*, *>
            val heightSpec = this["height"] as? Map<*, *>
            val start = Box(
                widthSpec?.get("start")?.toDimUnit(),
                heightSpec?.get("start")?.toDimUnit(),
            )
            val end = Box(
                widthSpec?.get("end")?.toDimUnit(),
                heightSpec?.get("end")?.toDimUnit(),
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
        "blur" -> {
            var start: Float = 0f
            var end: Float = 0f
            (this["blur_radius"] as? Map<*, *>)?.let { spec ->
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
                Animation.Role.Blur,
            )
        }
        "shadow" -> {
            val color = this["color"] as? Map<*, *>
            val blurRadius = this["blur_radius"] as? Map<*, *>
            val offset = this["offset"] as? Map<*, *>
            val start = Shadow(
                color?.let {
                    it["start"]?.toVisualValue() ?: throw adaptyError(
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
                    it["start"]?.toOffset() ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'shadow offset' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                },
            )
            val end = Shadow(
                color?.let {
                    it["end"]?.toVisualValue() ?: throw adaptyError(
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
                    it["end"]?.toOffset() ?: throw adaptyError(
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
        "inner_shadow" -> {
            val color = this["color"] as? Map<*, *>
            val blurRadius = this["blur_radius"] as? Map<*, *>
            val offset = this["offset"] as? Map<*, *>
            val start = Shadow(
                color?.let {
                    it["start"]?.toVisualValue() ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'inner_shadow color' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                },
                blurRadius?.let {
                    (it["start"] as? Number)?.toFloat() ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'inner_shadow blur radius' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                },
                offset?.let {
                    it["start"]?.toOffset() ?: throw adaptyError(
                        message = "Couldn't find 'start' for 'inner_shadow offset' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                },
            )
            val end = Shadow(
                color?.let {
                    it["end"]?.toVisualValue() ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'inner_shadow color' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                },
                blurRadius?.let {
                    (it["end"] as? Number)?.toFloat() ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'inner_shadow blur radius' animation",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                },
                offset?.let {
                    it["end"]?.toOffset() ?: throw adaptyError(
                        message = "Couldn't find 'end' for 'inner_shadow offset' animation",
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
                Animation.Role.InnerShadow,
            )
        }
        else -> null
    }
}

internal fun Map<*, *>.toAlign(): Align =
    this["h_align"].toHorizontalAlign() + this["v_align"].toVerticalAlign()

internal fun Any?.toVerticalAlign(default: VerticalAlign = VerticalAlign.CENTER): VerticalAlign {
    return when (this) {
        "top" -> VerticalAlign.TOP
        "bottom" -> VerticalAlign.BOTTOM
        "center" -> VerticalAlign.CENTER
        else -> default
    }
}

internal fun Any?.toHorizontalAlign(default: HorizontalAlign = HorizontalAlign.CENTER): HorizontalAlign {
    return when (this) {
        "leading" -> HorizontalAlign.START
        "left" -> HorizontalAlign.LEFT
        "trailing" -> HorizontalAlign.END
        "right" -> HorizontalAlign.RIGHT
        "center" -> HorizontalAlign.CENTER
        else -> default
    }
}

internal fun Any.toDimUnit(): DimUnit {
    when (this) {
        is Number -> return DimUnit.Exact(this.toFloat())
        is Map<*, *> -> {
            if (this["safe_area"] == "start")
                return DimUnit.SafeArea(DimUnit.SafeArea.Side.START)
            if (this["safe_area"] == "end")
                return DimUnit.SafeArea(DimUnit.SafeArea.Side.END)
            val point = this["point"] as? Number
            if (point != null)
                return DimUnit.Exact(point.toFloat())
            val screen = this["screen"] as? Number
            if (screen != null)
                return DimUnit.ScreenFraction(screen.toFloat())
            val unit = this["unit"]
            if (unit == "safe_area") {
                return when (this["value"]) {
                    "start" -> DimUnit.SafeArea(DimUnit.SafeArea.Side.START)
                    "end" -> DimUnit.SafeArea(DimUnit.SafeArea.Side.END)
                    else -> throw adaptyError(
                        message = "Unknown safe_area value (${this["value"]})",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }
            }
            val value = this["value"] as? Number ?: throw adaptyError(
                message = "Unknown dimension format (${this["value"]?.javaClass})",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            if (unit == "screen")
                return DimUnit.ScreenFraction(value.toFloat())
            return DimUnit.Exact(value.toFloat())
        }
        else -> throw adaptyError(
            message = "Unknown dimension format (${this.javaClass})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }
}

internal fun Any.toDimSpec(dimAxis: DimSpec.Axis): DimSpec {
    if (this !is Map<*, *>)
        return DimSpec.Specified(this.toDimUnit(), dimAxis)
    val min = this["min"]
    val max = this["max"]
    val shrink = this["shrink"]
    return when {
        this["fill_max"] == true -> DimSpec.FillMax(dimAxis)
        min != null -> DimSpec.Min(min.toDimUnit(), max?.toDimUnit(), dimAxis)
        shrink != null -> DimSpec.Shrink(shrink.toDimUnit(), max?.toDimUnit(), dimAxis)
        else -> DimSpec.Specified(this.toDimUnit(), dimAxis)
    }
}

internal fun Any.toEdgeEntities(): EdgeEntities? {
    when (this) {
        is Number -> return EdgeEntities(DimUnit.Exact(this.toFloat()))
        is Map<*, *> -> {
            val hasEdgeKeys = this.containsKey("leading") || this.containsKey("top") ||
                    this.containsKey("trailing") || this.containsKey("bottom")
            if (!hasEdgeKeys) {
                return EdgeEntities(this.toDimUnit())
            }
            val start = this["leading"]?.toDimUnit() ?: DimUnit.Exact(0f)
            val top = this["top"]?.toDimUnit() ?: DimUnit.Exact(0f)
            val end = this["trailing"]?.toDimUnit() ?: DimUnit.Exact(0f)
            val bottom = this["bottom"]?.toDimUnit() ?: DimUnit.Exact(0f)

            return EdgeEntities(start, top, end, bottom)
        }
        is Collection<*> -> {
            val values = this.mapNotNull { value -> value?.toDimUnit() }
                .takeIf { it.any { value -> (value as? DimUnit.Exact)?.value != 0f } }
                ?: return null
            if (values.size == 1) return EdgeEntities(values[0])
            if (values.size == 2) return EdgeEntities(values[0], values[1])
            return EdgeEntities(
                values.getOrNull(0) ?: DimUnit.Exact(0f),
                values.getOrNull(1) ?: DimUnit.Exact(0f),
                values.getOrNull(2) ?: DimUnit.Exact(0f),
                values.getOrNull(3) ?: DimUnit.Exact(0f),
            )
        }
        else -> throw adaptyError(
            message = "Unknown padding format (${this.javaClass})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }
}

@InternalAdaptyApi
public fun Any?.toAspectRatio(): AspectRatio {
    return when (this) {
        "fill" -> AspectRatio.FILL
        "stretch" -> AspectRatio.STRETCH
        else -> AspectRatio.FIT
    }
}

internal fun Any.toOffset(): Offset? {
    when (this) {
        is Number -> return Offset(this.toDimUnit())
        is Map<*, *> -> {
            if (this.containsKey("y") || this.containsKey("x")) {
                val y = this["y"]?.toDimUnit() ?: DimUnit.Exact(0f)
                val x = this["x"]?.toDimUnit() ?: DimUnit.Exact(0f)
                return Offset(y, x)
            }
            if (this.isEmpty()) return Offset(DimUnit.Exact(0f))
            return Offset(this.toDimUnit())
        }
        is Collection<*> -> {
            val numbers = this.mapIndexedNotNull { i, value ->
                value?.toDimUnit()
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
            message = "Unknown offset format (${this.javaClass})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }
}

private fun Any.toPoint(): Point? {
    when (this) {
        is Number -> return Point(this.toFloat())
        is Map<*, *> -> {
            val y = (this["y"] as? Number)?.toFloat() ?: 0f
            val x = (this["x"] as? Number)?.toFloat() ?: 0f
            return Point(y, x)
        }
        is Collection<*> -> {
            val numbers = this.mapNotNull {
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
            message = "Unknown point format (${this.javaClass})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }
}

private fun Any.toScalePoint(): Point {
    when (this) {
        is Number -> return Point(this.toFloat(), this.toFloat())
        is Map<*, *> -> {
            val y = (this["y"] as? Number)?.toFloat() ?: 0f
            val x = (this["x"] as? Number)?.toFloat() ?: 0f
            return Point(y, x)
        }
        is Collection<*> -> {
            val numbers = this.mapNotNull {
                (it as? Number)?.toFloat()
            }
            return when (numbers.size) {
                0 -> Point(0f, 0f)
                1 -> Point(numbers[0], numbers[0])
                2 -> Point(numbers[0], numbers[1])
                else -> throw adaptyError(
                    message = "Scale point array size (${numbers.size}) exceeds 2!",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
        }
        else -> throw adaptyError(
            message = "Unknown scale point format (${this.javaClass})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }
}

internal fun Any.toStaticRotation(): Rotation? {
    return when (this) {
        is Map<*, *> -> {
            val angle = (this["angle"] as? Number)?.toFloat() ?: return null
            val anchor = this["anchor"]?.toPoint() ?: Point.NormalizedCenter
            Rotation(angle, anchor)
        }
        is Number -> Rotation(this.toFloat(), Point.NormalizedCenter)
        else -> null
    }
}

internal fun Any.toStaticScale(): Scale? {
    return when (this) {
        is Map<*, *> -> {
            val scalePoint = this["scale"]?.toScalePoint() ?: return null
            val anchor = this["anchor"]?.toPoint() ?: Point.NormalizedCenter
            Scale(scalePoint, anchor)
        }
        is Number -> Scale(Point(this.toFloat(), this.toFloat()), Point.NormalizedCenter)
        else -> null
    }
}

internal fun Any.toShape(): Shape? {
    return when(this) {
        is Map<*, *> -> (this as Map<*, *>).toShape()
        is String -> mapOf("background" to this, "type" to "rect").toShape()
        else -> null
    }
}

private fun Map<*, *>.toShape(): Shape {
    val shapeType = (this["type"] as? String).let { type ->
        when (type) {
            "circle" -> Shape.Type.Circle
            "curve_up" -> Shape.Type.RectWithArc(Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
            "curve_down" -> Shape.Type.RectWithArc(-Shape.Type.RectWithArc.ABS_ARC_HEIGHT)
            else -> {
                val cornerRadius = this["rect_corner_radius"]?.toCornerRadius()
                Shape.Type.Rectangle(cornerRadius)
            }
        }
    }
    val filling = this["background"]?.toVisualValue()
    val border = this["border"]?.let { borderVisual ->
        val thickness = (this["thickness"] as? Number)?.toFloat() ?: 1f
        if (thickness == 0f) return@let null
        val visualValue = borderVisual.toVisualValue() ?: return@let null
        Shape.Border(
            visualValue,
            shapeType,
            thickness,
        )
    }
    val shadow = (this["shadow"] as? Map<*, *>)?.let { shadow ->
        Shadow(
            shadow["color"]?.toVisualValue() ?: return@let null,
            (shadow["blur_radius"] as? Number)?.toFloat() ?: 0f,
            shadow["offset"]?.toOffset() ?: Offset.Default,
        )
    }
    val innerShadow = (this["inner_shadow"] as? Map<*, *>)?.let { shadow ->
        Shadow(
            shadow["color"]?.toVisualValue() ?: return@let null,
            (shadow["blur_radius"] as? Number)?.toFloat() ?: 0f,
            shadow["offset"]?.toOffset() ?: Offset.Default,
        )
    }
    val blurRadius = (this["blur_radius"] as? Number)?.toFloat()?.takeIf { it > 0f }
    return Shape(filling, shapeType, border, shadow, innerShadow, blurRadius)
}

internal fun Any.toVisualValue(): VisualValue? {
    return this.toStringSource()?.let { VisualValue.any(it) }
}

@InternalAdaptyApi
public fun Any.toAssetVisualValue(): VisualValue? {
    return this.toStringSource()?.let { VisualValue.assetId(it) }
}

private fun Any.toStringSource(): StringSource? {
    return when {
        this is String && this.isNotEmpty() -> StringSource.Value(this)
        this is Map<*, *> -> {
            val variable = (this["var"] as? String) ?: throw adaptyError(
                message = "Couldn't find 'var' for data binding",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            val scope = (this["scope"]).toScope(Scope.Screen)
            val converter = this["converter"] as? String
            val converterParams = this["converter_params"]
            StringSource.Binding(variable, scope, converter, converterParams)
        }
        else -> null
    }
}

internal fun Any.toOneWayBinding(): OneWayBinding? {
    return when {
        this is Map<*, *> -> {
            val variable = (this["var"] as? String) ?: throw adaptyError(
                message = "Couldn't find 'var' for data binding",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            val scope = (this["scope"]).toScope(Scope.Screen)
            val converter = this["converter"] as? String
            val converterParams = this["converter_params"]
            OneWayBinding(variable, scope, converter, converterParams)
        }
        else -> null
    }
}

internal fun Any.toTwoWayBinding(): TwoWayBinding? {
    return when {
        this is Map<*, *> -> {
            val variable = (this["var"] as? String) ?: throw adaptyError(
                message = "Couldn't find 'var' for data binding",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            val scope = (this["scope"]).toScope(Scope.Screen)
            val setter = this["setter"] as? String
            val converter = this["converter"] as? String
            val converterParams = this["converter_params"]
            TwoWayBinding(variable, scope, setter, converter, converterParams)
        }
        else -> null
    }
}

internal fun Any?.toScope(default: Scope): Scope {
    return when(this as? String) {
        "global" -> Scope.Global
        "screen" -> Scope.Screen
        else -> default
    }
}

internal fun Any.toCornerRadius(): Shape.CornerRadius {
    when (this) {
        is Number -> return Shape.CornerRadius(this.toFloat())
        is Map<*, *> -> {
            val topStart = (this["top_leading"] as? Number)?.toFloat() ?: 0f
            val topEnd = (this["top_trailing"] as? Number)?.toFloat() ?: 0f
            val bottomEnd = (this["bottom_trailing"] as? Number)?.toFloat() ?: 0f
            val bottomStart = (this["bottom_leading"] as? Number)?.toFloat() ?: 0f

            return Shape.CornerRadius(topStart, topEnd, bottomEnd, bottomStart)
        }
        is Collection<*> -> {
            val numbers = this.mapNotNull {
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
            message = "Unknown corner radius format (${this.javaClass})",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }
}
