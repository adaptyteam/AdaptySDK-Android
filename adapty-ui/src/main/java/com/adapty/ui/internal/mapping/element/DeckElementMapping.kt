@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toDimUnit
import com.adapty.ui.internal.mapping.attributes.toHorizontalAlign
import com.adapty.ui.internal.mapping.attributes.toPoint
import com.adapty.ui.internal.mapping.attributes.toVerticalAlign
import com.adapty.ui.internal.ui.attributes.DeckItemLength
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.attributes.Point
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.element.DeckElement
import com.adapty.ui.internal.ui.element.DeckItem
import com.adapty.ui.internal.ui.element.UIElement

private const val DECK_ITEM_TYPE = "deck_item"

internal fun Map<*, *>.toDeckElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val (baseProps, nextInheritShrink) = this.extractBasePropsWithShrinkInheritance(inheritShrink)
    val itemConfigs = this["items"] as? List<*>
    if (itemConfigs.isNullOrEmpty())
        throw deckError("Deck must contain at least one item")
    return DeckElement(
        width = this["width"].toDeckSizeMode("width"),
        height = this["height"].toDeckSizeMode("height"),
        items = itemConfigs.mapNotNull { item ->
            val itemConfig = item as? Map<*, *> ?: throw deckError("DeckItem must be an object")
            itemConfig.toDeckItem { content -> childMapper(content, nextInheritShrink) }
        },
        baseProps = baseProps,
    )
}

private fun Any?.toDeckSizeMode(key: String): MainAxisBehaviour {
    if (this == null) return MainAxisBehaviour.FILL
    return MainAxisBehaviour.fromValueOrNull(this as? String)
        ?: throw deckError("Invalid deck $key: $this")
}

private fun Map<*, *>.toDeckItem(contentMapper: (Map<*, *>) -> UIElement?): DeckItem? {
    if (this["type"] != DECK_ITEM_TYPE)
        throw deckError("Invalid DeckItem type: ${this["type"]}")

    val anchor: Point
    val position: Point
    val mirrorInRtl: Boolean
    if (this["anchor"] != null || this["position"] != null) {
        if (this["h_align"] != null || this["v_align"] != null)
            throw deckError("DeckItem must use either anchor and position or h_align and v_align")
        anchor = this["anchor"]?.toNormalizedPoint()
            ?: throw deckError("DeckItem position requires anchor")
        position = this["position"]?.toNormalizedPoint()
            ?: throw deckError("DeckItem anchor requires position")
        mirrorInRtl = this["rtl"].toMirrorInRtl()
    } else {
        if (this["rtl"] != null)
            throw deckError("DeckItem rtl requires anchor and position")
        val horizontal = this["h_align"].toHorizontalAlign()
        val vertical = this["v_align"].toVerticalAlign()
        val x = when (horizontal) {
            HorizontalAlign.START, HorizontalAlign.LEFT -> 0f
            HorizontalAlign.END, HorizontalAlign.RIGHT -> 1f
            HorizontalAlign.CENTER -> 0.5f
        }
        val y = when (vertical) {
            VerticalAlign.TOP -> 0f
            VerticalAlign.BOTTOM -> 1f
            VerticalAlign.CENTER -> 0.5f
        }
        anchor = Point(y = y, x = x)
        position = anchor
        mirrorInRtl = horizontal == HorizontalAlign.START || horizontal == HorizontalAlign.END
    }

    val contentConfig = this["content"] as? Map<*, *>
        ?: throw deckError("DeckItem must have content")
    val content = contentMapper(contentConfig) ?: return null

    return DeckItem(
        width = this["width"].toDeckItemLength("width"),
        height = this["height"].toDeckItemLength("height"),
        anchor = anchor,
        position = position,
        mirrorInRtl = mirrorInRtl,
        content = content,
    )
}

private fun Any.toNormalizedPoint(): Point = toPoint() ?: Point.Zero

private fun Any?.toMirrorInRtl(): Boolean {
    if (this == null) return false
    return when ((this as? String)?.lowercase()) {
        "preserve", "keep" -> false
        "mirror" -> true
        else -> throw deckError("Unexpected DeckItem rtl value: $this")
    }
}

private fun Any?.toDeckItemLength(key: String): DeckItemLength {
    if (this == null) return DeckItemLength.Content
    if (this is Map<*, *> && this.containsKey("parent")) {
        val fraction = (this["parent"] as? Number)?.toFloat()
            ?: throw deckError("DeckItem $key parent must be a number")
        if (fraction < 0f) throw deckError("DeckItem $key parent length must be nonnegative")
        return DeckItemLength.Parent(fraction)
    }
    val unit = this.toDimUnit()
    val negative = when (unit) {
        is DimUnit.Exact -> unit.value < 0f
        is DimUnit.ScreenFraction -> unit.fraction < 0f
        is DimUnit.SafeArea -> false
    }
    if (negative) throw deckError("DeckItem $key fixed length must be nonnegative")
    return DeckItemLength.Fixed(unit)
}

private fun deckError(message: String) =
    adaptyError(message = message, adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)
