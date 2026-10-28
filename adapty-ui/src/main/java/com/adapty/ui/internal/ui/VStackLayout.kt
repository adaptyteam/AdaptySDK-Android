package com.adapty.ui.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class VStackCellKind { DEFAULT, TEXT_LIKE, SIZE_DRIVEN_UNSIZED, SIZE_DRIVEN_SIZED, SPACER, FILL_HEIGHT, GREEDY_ROOT }

internal data class VStackCellSpec(
    val kind: VStackCellKind,
    val minHeight: Dp = 0.dp,
    val weight: Float = 1f,
    val minFromContent: Boolean = false,
)

@Composable
internal fun VStackLayout(
    spacing: Dp,
    horizontalAlignment: Alignment.Horizontal,
    overflowAnchor: Alignment.Vertical,
    cells: List<VStackCellSpec>,
    modifier: Modifier,
    slots: List<SectionPresenceSlot?>? = null,
    content: @Composable () -> Unit,
) {
    val measurePolicy = remember(spacing, horizontalAlignment, overflowAnchor, cells, slots) {
        VStackMeasurePolicy(spacing, horizontalAlignment, overflowAnchor, cells, slots)
    }
    Layout(content = content, modifier = modifier, measurePolicy = measurePolicy)
}

private class VStackMeasurePolicy(
    private val spacing: Dp,
    private val horizontalAlignment: Alignment.Horizontal,
    private val overflowAnchor: Alignment.Vertical,
    private val cells: List<VStackCellSpec>,
    private val slots: List<SectionPresenceSlot?>? = null,
) : MeasurePolicy {

    private fun kindOf(index: Int, count: Int): VStackCellSpec =
        if (cells.size == count) cells[index] else DEFAULT_CELL

    private fun weightOf(index: Int, count: Int): Float = kindOf(index, count).weight.coerceAtLeast(0.01f)

    private fun Density.spacerMinOf(index: Int, count: Int): Int =
        kindOf(index, count).let { if (it.kind == VStackCellKind.SPACER) it.minHeight.roundToPx() else 0 }

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val count = measurables.size
        if (count == 0) {
            return layout(constraints.minWidth, constraints.minHeight) {}
        }
        val gaps = stackGapsPx(spacing.roundToPx(), count, slots)
        val totalSpacing = gaps.sum()
        val intrinsicWidth =
            if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val placeables = arrayOfNulls<Placeable>(count)
        distribute(
            measurables,
            available = availableHeight(constraints.maxHeight, constraints.hasBoundedHeight, totalSpacing),
            bound = constraints.maxHeight,
            intrinsicWidth = intrinsicWidth,
        ) { index, proposal ->
            val isSpacer = kindOf(index, count).kind == VStackCellKind.SPACER
            val fixedSpacerHeight = if (isSpacer && proposal != Constraints.Infinity) proposal else 0
            val placeable = measurables[index].measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = fixedSpacerHeight,
                    maxHeight = proposal,
                )
            )
            placeables[index] = placeable
            placeable.height
        }

        var contentHeight = totalSpacing
        var contentWidth = 0
        for (placeable in placeables) {
            contentHeight += placeable!!.height
            contentWidth = maxOf(contentWidth, placeable.width)
        }
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val overflow = contentHeight - height
        val yStart = if (overflow > 0) overflowAnchor.align(contentHeight, height) else 0
        return layout(width, height) {
            var y = yStart
            placeables.forEachIndexed { index, placeable ->
                y += gaps[index]
                placeable!!.place(horizontalAlignment.align(placeable.width, width, layoutDirection), y)
                y += placeable.height
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.indices.sumOf { i -> spacerMinOf(i, measurables.size).takeIf { it > 0 } ?: measurables[i].minIntrinsicHeight(width) } +
            stackGapsPx(spacing.roundToPx(), measurables.size, slots).sum()

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        measurables.indices.sumOf { i -> spacerMinOf(i, measurables.size).takeIf { it > 0 } ?: measurables[i].maxIntrinsicHeight(width) } +
            stackGapsPx(spacing.roundToPx(), measurables.size, slots).sum()

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        intrinsicWidth(measurables, height) { measurable, h -> measurable.minIntrinsicWidth(h) }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        intrinsicWidth(measurables, height) { measurable, h -> measurable.maxIntrinsicWidth(h) }

    private fun IntrinsicMeasureScope.intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        widthOf: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val count = measurables.size
        if (count == 0) return 0
        val totalSpacing = stackGapsPx(spacing.roundToPx(), count, slots).sum()
        val bounded = height != Constraints.Infinity
        var width = 0
        distribute(
            measurables,
            available = availableHeight(height, bounded, totalSpacing),
            bound = height,
            intrinsicWidth = Constraints.Infinity,
        ) { index, proposal ->
            val measurable = measurables[index]
            val maxHeight = measurable.maxIntrinsicHeight(Constraints.Infinity)
            val placedHeight = when {
                proposal == Constraints.Infinity -> maxHeight
                maxHeight == 0 -> proposal
                kindOf(index, count).kind == VStackCellKind.SPACER ||
                    kindOf(index, count).kind == VStackCellKind.FILL_HEIGHT ||
                    kindOf(index, count).kind == VStackCellKind.GREEDY_ROOT -> proposal
                else -> minOf(maxHeight, proposal)
            }
            width = maxOf(width, widthOf(measurable, placedHeight))
            placedHeight
        }
        return width
    }

    private fun availableHeight(maxHeight: Int, bounded: Boolean, totalSpacing: Int): Int =
        if (bounded) (maxHeight - totalSpacing).coerceAtLeast(0) else Constraints.Infinity

    internal inline fun Density.distribute(
        measurables: List<IntrinsicMeasurable>,
        available: Int,
        bound: Int,
        intrinsicWidth: Int,
        place: (index: Int, proposal: Int) -> Int,
    ) {
        val count = measurables.size
        val minHeights = IntArray(count)
        val flexibility = LongArray(count)
        val needsOrdering = count > 1 && available != Constraints.Infinity
        if (needsOrdering) {
            for (i in 0 until count) {
                val measurable = measurables[i]
                val cell = kindOf(i, count)
                try {
                    when (cell.kind) {
                        VStackCellKind.TEXT_LIKE -> {
                            val ideal = measurable.maxIntrinsicHeight(intrinsicWidth)
                            val oneLine = measurable.minIntrinsicHeight(Constraints.Infinity).coerceAtMost(ideal)
                            minHeights[i] = oneLine
                            flexibility[i] = (ideal - oneLine).coerceAtLeast(0).toLong()
                        }
                        VStackCellKind.SIZE_DRIVEN_UNSIZED -> {
                            minHeights[i] = 0
                            flexibility[i] = Long.MAX_VALUE
                        }
                        VStackCellKind.SIZE_DRIVEN_SIZED -> {
                            minHeights[i] = cell.minHeight.roundToPx()
                            flexibility[i] = 0
                        }
                        VStackCellKind.FILL_HEIGHT -> {
                            minHeights[i] = if (cell.minFromContent) measurable.minIntrinsicHeight(intrinsicWidth) else 0
                            flexibility[i] = Long.MAX_VALUE
                        }
                        VStackCellKind.SPACER, VStackCellKind.GREEDY_ROOT -> {
                            minHeights[i] = 0
                            flexibility[i] = Long.MAX_VALUE
                        }
                        VStackCellKind.DEFAULT -> {
                            val min = measurable.minIntrinsicHeight(intrinsicWidth)
                            val max = measurable.maxIntrinsicHeight(intrinsicWidth)
                            minHeights[i] = min
                            flexibility[i] = (max - min).coerceAtLeast(0).toLong()
                        }
                    }
                } catch (e: IllegalStateException) {
                    minHeights[i] = 0
                    flexibility[i] = if (cell.kind == VStackCellKind.FILL_HEIGHT) Long.MAX_VALUE else 0
                }
            }
        }
        val order = if (needsOrdering) (0 until count).sortedBy { flexibility[it] } else (0 until count).toList()

        val (spacers, contentCells) = order.partition { kindOf(it, count).kind == VStackCellKind.SPACER }
        var reserved = 0
        for (index in spacers) reserved += spacerMinOf(index, count)
        var remaining = if (available == Constraints.Infinity) available else (available - reserved).coerceAtLeast(0)
        var unplaced = contentCells.size
        for (index in contentCells) {
            val proposal = when {
                remaining == Constraints.Infinity -> Constraints.Infinity
                else -> maxOf(remaining / unplaced, minHeights[index]).coerceAtMost(bound)
            }
            val placedHeight = place(index, proposal)
            if (remaining != Constraints.Infinity) {
                remaining = (remaining - placedHeight).coerceAtLeast(0)
            }
            unplaced--
        }
        if (remaining != Constraints.Infinity) remaining += reserved
        var spacerWeight = 0f
        for (index in spacers) spacerWeight += weightOf(index, count)
        for (index in spacers) {
            val weight = weightOf(index, count)
            val min = spacerMinOf(index, count)
            val proposal = when {
                remaining == Constraints.Infinity -> min
                else -> maxOf((remaining * weight / spacerWeight).toInt().coerceAtMost(bound), min)
            }
            val placedHeight = place(index, proposal)
            if (remaining != Constraints.Infinity) {
                remaining = (remaining - placedHeight).coerceAtLeast(0)
            }
            spacerWeight -= weight
        }
    }
}

private val DEFAULT_CELL = VStackCellSpec(VStackCellKind.DEFAULT)
