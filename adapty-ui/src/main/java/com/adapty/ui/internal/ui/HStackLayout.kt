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

internal enum class HStackCellKind { DEFAULT, TEXT_LIKE, SIZE_DRIVEN_UNSIZED, SIZE_DRIVEN_SIZED, SPACER, FILL_WIDTH, GREEDY_ROOT }

internal val STACK_SPACER_MIN_LENGTH: Dp = 8.dp

internal data class HStackCellSpec(
    val kind: HStackCellKind,
    val minWidth: Dp = 0.dp,
    val weight: Float = 1f,
)

@Composable
internal fun HStackLayout(
    spacing: Dp,
    verticalAlignment: Alignment.Vertical,
    cells: List<HStackCellSpec>,
    modifier: Modifier,
    slots: List<SectionPresenceSlot?>? = null,
    content: @Composable () -> Unit,
) {
    val measurePolicy = remember(spacing, verticalAlignment, cells, slots) {
        HStackMeasurePolicy(spacing, verticalAlignment, cells, slots)
    }
    Layout(content = content, modifier = modifier, measurePolicy = measurePolicy)
}

private class HStackMeasurePolicy(
    private val spacing: Dp,
    private val verticalAlignment: Alignment.Vertical,
    private val cells: List<HStackCellSpec>,
    private val slots: List<SectionPresenceSlot?>? = null,
) : MeasurePolicy {

    private fun kindOf(index: Int, count: Int): HStackCellSpec =
        if (cells.size == count) cells[index] else DEFAULT_CELL

    private fun weightOf(index: Int, count: Int): Float = kindOf(index, count).weight.coerceAtLeast(0.01f)

    private fun Density.spacerMinOf(index: Int, count: Int): Int =
        kindOf(index, count).let { if (it.kind == HStackCellKind.SPACER) it.minWidth.roundToPx() else 0 }

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
        val intrinsicHeight =
            if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity
        val placeables = arrayOfNulls<Placeable>(count)
        distribute(
            measurables,
            available = availableWidth(constraints.maxWidth, constraints.hasBoundedWidth, totalSpacing),
            bound = constraints.maxWidth,
            intrinsicHeight = intrinsicHeight,
        ) { index, proposal ->
            val isSpacer = kindOf(index, count).kind == HStackCellKind.SPACER
            val fixedSpacerWidth = if (isSpacer && proposal != Constraints.Infinity) proposal else 0
            val placeable = measurables[index].measure(
                Constraints(
                    minWidth = fixedSpacerWidth,
                    maxWidth = proposal,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                )
            )
            placeables[index] = placeable
            placeable.width
        }

        var contentWidth = totalSpacing
        var contentHeight = 0
        for (placeable in placeables) {
            contentWidth += placeable!!.width
            contentHeight = maxOf(contentHeight, placeable.height)
        }
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                x += gaps[index]
                placeable!!.placeRelative(x, verticalAlignment.align(placeable.height, height))
                x += placeable.width
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.indices.sumOf { i -> spacerMinOf(i, measurables.size).takeIf { it > 0 } ?: measurables[i].minIntrinsicWidth(height) } +
            stackGapsPx(spacing.roundToPx(), measurables.size, slots).sum()

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.indices.sumOf { i -> spacerMinOf(i, measurables.size).takeIf { it > 0 } ?: measurables[i].maxIntrinsicWidth(height) } +
            stackGapsPx(spacing.roundToPx(), measurables.size, slots).sum()

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        intrinsicHeight(measurables, width) { measurable, w -> measurable.minIntrinsicHeight(w) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        intrinsicHeight(measurables, width) { measurable, w -> measurable.maxIntrinsicHeight(w) }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        heightOf: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val count = measurables.size
        if (count == 0) return 0
        val totalSpacing = stackGapsPx(spacing.roundToPx(), count, slots).sum()
        val bounded = width != Constraints.Infinity
        var height = 0
        distribute(
            measurables,
            available = availableWidth(width, bounded, totalSpacing),
            bound = width,
            intrinsicHeight = Constraints.Infinity,
        ) { index, proposal ->
            val measurable = measurables[index]
            val maxWidth = measurable.maxIntrinsicWidth(Constraints.Infinity)
            val placedWidth = when {
                proposal == Constraints.Infinity -> maxWidth
                maxWidth == 0 -> proposal
                kindOf(index, count).kind == HStackCellKind.SPACER ||
                    kindOf(index, count).kind == HStackCellKind.FILL_WIDTH ||
                    kindOf(index, count).kind == HStackCellKind.GREEDY_ROOT -> proposal
                else -> minOf(maxWidth, proposal)
            }
            height = maxOf(height, heightOf(measurable, placedWidth))
            placedWidth
        }
        return height
    }

    private fun availableWidth(maxWidth: Int, bounded: Boolean, totalSpacing: Int): Int =
        if (bounded) (maxWidth - totalSpacing).coerceAtLeast(0) else Constraints.Infinity

    private inline fun Density.distribute(
        measurables: List<IntrinsicMeasurable>,
        available: Int,
        bound: Int,
        intrinsicHeight: Int,
        place: (index: Int, proposal: Int) -> Int,
    ) {
        val count = measurables.size
        val minWidths = IntArray(count)
        val flexibility = LongArray(count)
        val needsOrdering = count > 1 && available != Constraints.Infinity
        if (needsOrdering) {
            for (i in 0 until count) {
                val measurable = measurables[i]
                val cell = kindOf(i, count)
                try {
                    when (cell.kind) {
                        HStackCellKind.TEXT_LIKE -> {
                            minWidths[i] = 0
                            flexibility[i] = measurable.maxIntrinsicWidth(intrinsicHeight).toLong()
                        }
                        HStackCellKind.SIZE_DRIVEN_UNSIZED -> {
                            minWidths[i] = 0
                            flexibility[i] = Long.MAX_VALUE
                        }
                        HStackCellKind.SIZE_DRIVEN_SIZED -> {
                            minWidths[i] = cell.minWidth.roundToPx()
                            flexibility[i] = 0
                        }
                        HStackCellKind.SPACER, HStackCellKind.FILL_WIDTH, HStackCellKind.GREEDY_ROOT -> {
                            minWidths[i] = 0
                            flexibility[i] = Long.MAX_VALUE
                        }
                        HStackCellKind.DEFAULT -> {
                            val min = measurable.minIntrinsicWidth(intrinsicHeight)
                            val max = measurable.maxIntrinsicWidth(intrinsicHeight)
                            minWidths[i] = min
                            flexibility[i] = (max - min).coerceAtLeast(0).toLong()
                        }
                    }
                } catch (e: IllegalStateException) {
                    minWidths[i] = 0
                    flexibility[i] = 0
                }
            }
        }
        val order = if (needsOrdering) (0 until count).sortedBy { flexibility[it] } else (0 until count).toList()

        val (spacers, contentCells) = order.partition { kindOf(it, count).kind == HStackCellKind.SPACER }
        var reserved = 0
        for (index in spacers) reserved += spacerMinOf(index, count)
        var remaining = if (available == Constraints.Infinity) available else (available - reserved).coerceAtLeast(0)
        var unplaced = contentCells.size
        for (index in contentCells) {
            val proposal = when {
                remaining == Constraints.Infinity -> Constraints.Infinity
                else -> maxOf(remaining / unplaced, minWidths[index]).coerceAtMost(bound)
            }
            val placedWidth = place(index, proposal)
            if (remaining != Constraints.Infinity) {
                remaining = (remaining - placedWidth).coerceAtLeast(0)
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
            val placedWidth = place(index, proposal)
            if (remaining != Constraints.Infinity) {
                remaining = (remaining - placedWidth).coerceAtLeast(0)
            }
            spacerWeight -= weight
        }
    }
}

private val DEFAULT_CELL = HStackCellSpec(HStackCellKind.DEFAULT)
