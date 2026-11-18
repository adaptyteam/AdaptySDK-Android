package com.adapty.ui.internal.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints

internal class ScrollViewport {
    var height: Int by mutableIntStateOf(Constraints.Infinity)
}

internal val LocalScrollViewport = compositionLocalOf<ScrollViewport?> { null }

internal fun Modifier.recordScrollViewport(viewport: ScrollViewport?): Modifier =
    if (viewport == null) this
    else layout { measurable, constraints ->
        viewport.height = constraints.maxHeight
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

internal fun Modifier.boundedByScrollViewport(viewport: ScrollViewport?): Modifier =
    if (viewport == null) this else this.then(BoundedByScrollViewportElement(viewport))

private data class BoundedByScrollViewportElement(
    val viewport: ScrollViewport,
) : ModifierNodeElement<BoundedByScrollViewportNode>() {
    override fun create() = BoundedByScrollViewportNode(viewport)
    override fun update(node: BoundedByScrollViewportNode) {
        node.viewport = viewport
    }
}

private class BoundedByScrollViewportNode(
    var viewport: ScrollViewport,
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val viewportHeight = viewport.height
        val bounded =
            if (constraints.hasBoundedHeight || viewportHeight == Constraints.Infinity) {
                constraints
            } else {
                val natural = try {
                    measurable.maxIntrinsicHeight(constraints.maxWidth)
                } catch (e: IllegalStateException) {
                    null
                }
                constraints.copy(maxHeight = maxOf(natural ?: viewportHeight, constraints.minHeight))
            }
        val placeable = measurable.measure(bounded)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.minIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measurable.maxIntrinsicHeight(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.minIntrinsicWidth(height)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        measurable.maxIntrinsicWidth(height)
}
