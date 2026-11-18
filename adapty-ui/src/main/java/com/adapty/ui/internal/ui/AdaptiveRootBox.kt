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
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import com.adapty.ui.internal.ui.attributes.Condition
import com.adapty.ui.internal.ui.attributes.ConditionScope
import com.adapty.ui.internal.utils.LocalScreenDimensions
import kotlinx.coroutines.launch

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
        val unspecifiedAxes = LocalUnspecifiedAxes.current
        val scope = ConditionScope(
            availableWidthDp = maxWidth.value.takeIf { it.isFinite() && !unspecifiedAxes.horizontal }
                ?: screenWidthDp,
            availableHeightDp = maxHeight.value.takeIf { it.isFinite() && !unspecifiedAxes.vertical }
                ?: screenHeightDp,
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
    val maxHeight = mutableIntStateOf(0)

    val stackMinWidth = mutableIntStateOf(0)
    val stackMinHeight = mutableIntStateOf(0)

    var queried = false
    var queriedStackMinWidth = false
    var queriedStackMinHeight = false

    fun snapshot(): IntArray = intArrayOf(
        minWidth.intValue, maxWidth.intValue, height.intValue, maxHeight.intValue,
        stackMinWidth.intValue, stackMinHeight.intValue,
    )

    var onRecordedChanged: (() -> Unit)? = null
}

private data class RecordedIntrinsicsElement(
    val natural: NaturalExtent,
) : ModifierNodeElement<RecordedIntrinsicsNode>() {
    override fun create() = RecordedIntrinsicsNode(natural)
    override fun update(node: RecordedIntrinsicsNode) {
        node.natural = natural
        natural.onRecordedChanged = node.invalidateAfterMeasure
    }
}

private class RecordedIntrinsicsNode(var natural: NaturalExtent) : Modifier.Node(), LayoutModifierNode {
    val invalidateAfterMeasure: () -> Unit = {
        if (isAttached) coroutineScope.launch { if (isAttached) invalidateMeasurement() }
    }

    override fun onAttach() {
        natural.onRecordedChanged = invalidateAfterMeasure
    }

    override fun onDetach() {
        if (natural.onRecordedChanged === invalidateAfterMeasure) natural.onRecordedChanged = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        natural.queried = true
        if (HStackMinWidthQuery.active) {
            natural.queriedStackMinWidth = true
            return natural.stackMinWidth.intValue
        }
        return natural.minWidth.intValue
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int {
        natural.queried = true
        return natural.maxWidth.intValue
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        natural.queried = true
        if (VStackMinHeightQuery.active) {
            natural.queriedStackMinHeight = true
            return natural.stackMinHeight.intValue
        }
        return natural.height.intValue
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
        natural.queried = true
        return natural.maxHeight.intValue
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
            val before = natural.snapshot()
            try {
                natural.minWidth.intValue = measurable.minIntrinsicWidth(Constraints.Infinity)
                natural.maxWidth.intValue = measurable.maxIntrinsicWidth(Constraints.Infinity)
                natural.height.intValue = NaturalHeightQuery.run { measurable.minIntrinsicHeight(constraints.maxWidth) }
                natural.maxHeight.intValue = NaturalHeightQuery.run { measurable.maxIntrinsicHeight(constraints.maxWidth) }
                if (natural.queriedStackMinWidth) {
                    natural.stackMinWidth.intValue = HStackMinWidthQuery.minIntrinsicWidth(measurable, Constraints.Infinity)
                }
                if (natural.queriedStackMinHeight) {
                    natural.stackMinHeight.intValue =
                        NaturalHeightQuery.run { VStackMinHeightQuery.minIntrinsicHeight(measurable, constraints.maxWidth) }
                }
            } catch (e: IllegalStateException) {
            }
            if (!before.contentEquals(natural.snapshot())) natural.onRecordedChanged?.invoke()
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
