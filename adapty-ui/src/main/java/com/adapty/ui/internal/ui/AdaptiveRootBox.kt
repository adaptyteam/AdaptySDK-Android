package com.adapty.ui.internal.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import com.adapty.ui.internal.ui.attributes.Condition
import com.adapty.ui.internal.ui.attributes.ConditionScope
import com.adapty.ui.internal.utils.LocalScreenDimensions

@Composable
internal fun AdaptiveRootBox(
    modifier: Modifier,
    content: @Composable (ConditionScope) -> Unit,
) {
    val natural = remember { NaturalExtent() }
    BoxWithConstraints(modifier = modifier.then(RecordedIntrinsicsElement(natural))) {
        val configuration = LocalConfiguration.current
        val screenDimensions = LocalScreenDimensions.current
        val screenWidthDp = screenDimensions.widthDp
        val screenHeightDp = screenDimensions.heightDp
        val scope = ConditionScope(
            availableWidthDp = maxWidth.value.takeIf { it.isFinite() } ?: screenWidthDp,
            availableHeightDp = maxHeight.value.takeIf { it.isFinite() } ?: screenHeightDp,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            orientation = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                Condition.Orientation.Landscape
            else
                Condition.Orientation.Portrait,
        )
        Box(Modifier.then(RecordNaturalExtentElement(natural)), propagateMinConstraints = true) {
            content(scope)
        }
    }
}

internal class NaturalExtent {
    val minWidth = mutableIntStateOf(0)
    val maxWidth = mutableIntStateOf(0)
    val height = mutableIntStateOf(0)

    var queried = false
}

private data class RecordedIntrinsicsElement(
    val natural: NaturalExtent,
) : ModifierNodeElement<RecordedIntrinsicsNode>() {
    override fun create() = RecordedIntrinsicsNode(natural)
    override fun update(node: RecordedIntrinsicsNode) {
        node.natural = natural
    }
}

private class RecordedIntrinsicsNode(var natural: NaturalExtent) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        natural.queried = true
        return natural.minWidth.intValue
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        natural.queried = true
        return natural.maxWidth.intValue
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        natural.queried = true
        return natural.height.intValue
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        natural.queried = true
        return natural.height.intValue
    }
}

private data class RecordNaturalExtentElement(
    val natural: NaturalExtent,
) : ModifierNodeElement<RecordNaturalExtentNode>() {
    override fun create() = RecordNaturalExtentNode(natural)
    override fun update(node: RecordNaturalExtentNode) {
        node.natural = natural
    }
}

private class RecordNaturalExtentNode(var natural: NaturalExtent) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        if (natural.queried) {
            try {
                natural.minWidth.intValue = measurable.minIntrinsicWidth(Constraints.Infinity)
                natural.maxWidth.intValue = measurable.maxIntrinsicWidth(Constraints.Infinity)
                natural.height.intValue = measurable.minIntrinsicHeight(constraints.maxWidth)
            } catch (e: IllegalStateException) {
            }
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
