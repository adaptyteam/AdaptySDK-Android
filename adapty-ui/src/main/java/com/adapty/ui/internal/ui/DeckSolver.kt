package com.adapty.ui.internal.ui

import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.element.DeckItem

internal object DeckSolver {

    const val FALLBACK_LENGTH_DP: Float = 10f

    sealed interface Length {
        object Content : Length

        data class Px(val value: Float) : Length

        data class Parent(val fraction: Float) : Length
    }

    data class Placement(
        val anchorX: Float,
        val anchorY: Float,
        val positionX: Float,
        val positionY: Float,
    )

    data class Item(
        val width: Length,
        val height: Length,
        val placement: Placement,
    )

    data class Extent(val width: Float, val height: Float)

    data class Frame(val x: Float, val y: Float, val width: Float, val height: Float)

    data class Result(val width: Float, val height: Float, val frames: List<Frame>)

    fun solve(
        width: MainAxisBehaviour,
        height: MainAxisBehaviour,
        items: List<Item>,
        availableWidth: Float?,
        availableHeight: Float?,
        fallbackLength: Float,
        measure: (index: Int, maxWidth: Float?, maxHeight: Float?) -> Extent,
    ): Result {
        val naturalSizes = items.mapIndexed { index, item ->
            if (item.width == Length.Content) measure(index, null, null) else null
        }
        val naturalWidths = naturalSizes.map { it?.width }

        val deckWidth = innerLength(
            mode = width,
            available = availableWidth,
            fallback = fallbackLength,
            bases = items.indices.map { baseLength(items[it].width, naturalWidths[it]) },
        )
        val childWidths = items.indices.map {
            resolvedLength(items[it].width, naturalWidths[it], deckWidth)
        }

        val contentHeights = items.mapIndexed { index, item ->
            if (item.height == Length.Content) {
                (naturalSizes[index] ?: measure(index, childWidths[index], null)).height
            } else {
                null
            }
        }

        val deckHeight = innerLength(
            mode = height,
            available = availableHeight,
            fallback = fallbackLength,
            bases = items.indices.map { baseLength(items[it].height, contentHeights[it]) },
        )

        val frames = items.mapIndexed { index, item ->
            val childWidth = childWidths[index]
            val childHeight = resolvedLength(item.height, contentHeights[index], deckHeight)
            val placement = item.placement
            Frame(
                x = placement.positionX * deckWidth - placement.anchorX * childWidth,
                y = placement.positionY * deckHeight - placement.anchorY * childHeight,
                width = childWidth,
                height = childHeight,
            )
        }

        return Result(deckWidth, deckHeight, frames)
    }

    fun placement(item: DeckItem, isRtl: Boolean): Placement {
        val flipsX = isRtl && item.mirrorInRtl
        return Placement(
            anchorX = if (flipsX) 1f - item.anchor.x else item.anchor.x,
            anchorY = item.anchor.y,
            positionX = if (flipsX) 1f - item.position.x else item.position.x,
            positionY = item.position.y,
        )
    }

    private fun innerLength(
        mode: MainAxisBehaviour,
        available: Float?,
        fallback: Float,
        bases: List<Float?>,
    ): Float = when (mode) {
        MainAxisBehaviour.HUG -> bases.filterNotNull().maxOrNull() ?: fallback
        MainAxisBehaviour.FILL ->
            if (available != null && available.isFinite()) available.coerceAtLeast(0f) else fallback
    }

    private fun baseLength(length: Length, natural: Float?): Float? = when (length) {
        Length.Content -> natural?.coerceAtLeast(0f)
        is Length.Px -> length.value.coerceAtLeast(0f)
        is Length.Parent -> null
    }

    private fun resolvedLength(length: Length, natural: Float?, inner: Float): Float = when (length) {
        Length.Content -> (natural ?: 0f).coerceAtLeast(0f)
        is Length.Px -> length.value.coerceAtLeast(0f)
        is Length.Parent -> (length.fraction * inner).coerceAtLeast(0f)
    }
}
