@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.attributes

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.VisualValue

@InternalAdaptyApi
public class Decorator internal constructor(
    internal val type: Shape.Type,
    internal val underlays: List<Layer>,
    internal val overlays: List<Layer>,
    internal val blurRadius: Float? = null,
    internal val clip: Clip = Clip.CONTENT,
    internal val blend: BlendMode = BlendMode.NORMAL,
) {
    internal class Layer(
        val id: Int?,
        val kind: Kind,
        val blend: BlendMode = BlendMode.NORMAL,
        val effects: List<Effect> = emptyList(),
    ) {
        sealed class Kind {
            object Effects : Kind()
            class Fill(val asset: VisualValue) : Kind()
            class Stroke(val asset: VisualValue, val thickness: Float) : Kind()
            class Image(val asset: VisualValue, val aspect: AspectRatio) : Kind()
        }
    }

    internal class Effect(
        val id: Int?,
        val kind: Kind,
        val color: VisualValue,
        val blurRadius: Float = 0f,
        val offset: Offset = Offset.Default,
    ) {
        enum class Kind { DROP, INNER }
    }

    internal enum class Clip { CONTENT, NONE }

    internal val primaryUnderlay: Layer? =
        underlays.firstOrNull { it.kind is Layer.Kind.Fill || it.kind is Layer.Kind.Image }

    internal val fill: VisualValue? = when (val kind = primaryUnderlay?.kind) {
        is Layer.Kind.Fill -> kind.asset
        is Layer.Kind.Image -> kind.asset
        else -> null
    }

    internal companion object {
        fun background(fill: VisualValue?) = Decorator(
            type = Shape.Type.Rectangle(null),
            underlays = listOfNotNull(
                fill?.let { Layer(LEGACY_ID, Layer.Kind.Image(it, AspectRatio.FILL)) }
            ),
            overlays = emptyList(),
        )

        const val LEGACY_ID = 0
    }
}

internal enum class BlendMode(val id: String) {
    NORMAL("normal"),
    MULTIPLY("multiply"),
    SCREEN("screen"),
    OVERLAY("overlay"),
    DARKEN("darken"),
    LIGHTEN("lighten"),
    COLOR_DODGE("color_dodge"),
    COLOR_BURN("color_burn"),
    HARD_LIGHT("hard_light"),
    DIFFERENCE("difference"),
    EXCLUSION("exclusion"),
    HUE("hue"),
    SATURATION("saturation"),
    COLOR("color"),
    LUMINOSITY("luminosity"),
    SOURCE_ATOP("source_atop"),
    DESTINATION_OVER("destination_over"),
    DESTINATION_OUT("destination_out"),
    DESTINATION_IN("destination_in"),
    PLUS_LIGHTER("plus_lighter");

    companion object {
        private val byId = values().associateBy { it.id }

        fun fromId(id: String): BlendMode? = byId[id]
    }
}
