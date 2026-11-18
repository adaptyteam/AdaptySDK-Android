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
import com.adapty.ui.internal.ui.attributes.BlendMode
import com.adapty.ui.internal.ui.attributes.Decorator
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
                    targetId = this["target_id"].toAnimationTargetId(),
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
                targetId = this["target_id"].toAnimationTargetId(),
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
                targetId = this["target_id"].toAnimationTargetId(),
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
                targetId = this["target_id"].toAnimationTargetId(),
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
            if (this.isEmpty()) return EdgeEntities(DimUnit.Exact(0f))
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

internal fun Any.toPoint(): Point? {
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

internal fun Any.toDecorator(): Decorator? {
    return when(this) {
        is Map<*, *> -> (this as Map<*, *>).toDecorator()
        is String -> mapOf("background" to this, "type" to "rect").toDecorator()
        else -> null
    }
}

private fun Map<*, *>.toDecorator(): Decorator {
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
    val underlays = mutableListOf<Decorator.Layer>()
    val overlays = mutableListOf<Decorator.Layer>()
    val shadow = (this["shadow"] as? Map<*, *>)?.toLegacyEffect(Decorator.Effect.Kind.DROP)
    val background = this["background"]?.toVisualValue()
    if (background != null) {
        val innerShadow = (this["inner_shadow"] as? Map<*, *>)?.toLegacyEffect(Decorator.Effect.Kind.INNER)
        underlays += Decorator.Layer(
            id = Decorator.LEGACY_ID,
            kind = Decorator.Layer.Kind.Image(background, AspectRatio.FILL),
            effects = listOfNotNull(shadow, innerShadow),
        )
    } else if (shadow != null) {
        underlays += Decorator.Layer(
            id = Decorator.LEGACY_ID,
            kind = Decorator.Layer.Kind.Effects,
            effects = listOf(shadow),
        )
    }
    this["border"]?.let { borderVisual ->
        val thickness = (this["thickness"] as? Number)?.toFloat() ?: 1f
        val visualValue = borderVisual.toVisualValue() ?: return@let
        overlays += Decorator.Layer(
            id = Decorator.LEGACY_ID,
            kind = Decorator.Layer.Kind.Stroke(visualValue, thickness),
        )
    }
    underlays += this["underlays"].toDecoratorLayers("underlays")
    overlays += this["overlays"].toDecoratorLayers("overlays")
    val blurRadius = (this["blur_radius"] as? Number)?.toFloat()?.takeIf { it > 0f }
    return Decorator(
        shapeType,
        underlays,
        overlays,
        blurRadius,
        this["clip"].toDecoratorClip(),
        this["blend"].toBlendMode(),
    )
}

private fun Any?.toDecoratorClip(): Decorator.Clip = when (this) {
    null, "content" -> Decorator.Clip.CONTENT
    "none" -> Decorator.Clip.NONE
    else -> throw decoratorError("Unknown decorator clip ($this)")
}

private fun Any?.toAnimationTargetId(): Int {
    if (this == null) return 0
    val number = this as? Number
        ?: throw decoratorError("Animation 'target_id' must be a number (${this.javaClass})")
    val value = number.toDouble()
    if (value % 1.0 != 0.0 || value < Int.MIN_VALUE || value > Int.MAX_VALUE)
        throw decoratorError("Animation 'target_id' must be an integer ($number)")
    return value.toInt()
}

private fun decoratorError(message: String): Throwable =
    adaptyError(message = message, adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)

internal fun Any?.toBlendMode(): BlendMode = (this as? String)?.let(BlendMode::fromId) ?: BlendMode.NORMAL

private fun Any?.toDecoratorLayers(key: String): List<Decorator.Layer> {
    val items = when (this) {
        null -> return emptyList()
        is List<*> -> this
        else -> throw decoratorError("Decorator '$key' must be an array (${this.javaClass})")
    }
    return items.mapNotNull { item ->
        val layer = item as? Map<*, *>
            ?: throw decoratorError("Decorator layer must be an object (${item?.javaClass})")
        layer.toDecoratorLayer()
    }
}

private fun Map<*, *>.toDecoratorLayer(): Decorator.Layer? {
    val kind = when (val type = this["type"]) {
        null, "effects" -> Decorator.Layer.Kind.Effects
        "fill" -> Decorator.Layer.Kind.Fill(requireLayerAsset("fill"))
        "stroke" -> Decorator.Layer.Kind.Stroke(
            requireLayerAsset("stroke"),
            (this["thickness"] as? Number)?.toFloat() ?: 1f,
        )
        "image" -> Decorator.Layer.Kind.Image(
            requireLayerAsset("image"),
            this["aspect"]?.toAspectRatio() ?: AspectRatio.FILL,
        )
        is String -> return null
        else -> throw decoratorError("Decorator layer type must be a string (${type.javaClass})")
    }
    return Decorator.Layer(
        id = this["id"].toDecoratorComponentId(),
        kind = kind,
        blend = this["blend"].toBlendMode(),
        effects = this["effects"].toDecoratorEffects(),
    )
}

private fun Map<*, *>.requireLayerAsset(type: String): VisualValue =
    this["asset_id"]?.toVisualValue()
        ?: throw decoratorError("Decorator '$type' layer requires 'asset_id'")

private fun Any?.toDecoratorEffects(): List<Decorator.Effect> {
    val items = when (this) {
        null -> return emptyList()
        is List<*> -> this
        else -> throw decoratorError("Decorator layer 'effects' must be an array (${this.javaClass})")
    }
    return items.mapNotNull { item ->
        val effect = item as? Map<*, *>
            ?: throw decoratorError("Decorator effect must be an object (${item?.javaClass})")
        effect.toDecoratorEffect()
    }
}

private fun Map<*, *>.toDecoratorEffect(): Decorator.Effect? {
    val kind = when (val type = this["type"]) {
        null, "drop_shadow" -> Decorator.Effect.Kind.DROP
        "inner_shadow" -> Decorator.Effect.Kind.INNER
        is String -> return null
        else -> throw decoratorError("Decorator effect type must be a string (${type.javaClass})")
    }
    return Decorator.Effect(
        id = this["id"].toDecoratorComponentId(),
        kind = kind,
        color = this["color"]?.toVisualValue()
            ?: throw decoratorError("Decorator effect requires 'color'"),
        blurRadius = (this["blur_radius"] as? Number)?.toFloat() ?: 0f,
        offset = this["offset"]?.toOffset() ?: Offset.Default,
    )
}

private fun Any?.toDecoratorComponentId(): Int? {
    if (this == null) return null
    val number = this as? Number
        ?: throw decoratorError("Decorator component id must be a number (${this.javaClass})")
    val value = number.toDouble()
    if (value % 1.0 != 0.0 || value < 1.0 || value > Int.MAX_VALUE)
        throw decoratorError("Decorator component id must be a positive integer ($number)")
    return value.toInt()
}

private fun Map<*, *>.toLegacyEffect(kind: Decorator.Effect.Kind): Decorator.Effect? {
    return Decorator.Effect(
        id = Decorator.LEGACY_ID,
        kind = kind,
        color = this["color"]?.toVisualValue() ?: return null,
        blurRadius = (this["blur_radius"] as? Number)?.toFloat() ?: 0f,
        offset = this["offset"]?.toOffset() ?: Offset.Default,
    )
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
