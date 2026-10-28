package com.adapty.ui.internal.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints

@Suppress("DEPRECATION")
internal object ZeroIntrinsicsModifier : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = 0

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = 0
}

internal object NaturalHeightQuery {
    var depth = 0

    val isActive: Boolean get() = depth > 0

    inline fun <T> run(block: () -> T): T {
        depth++
        try {
            return block()
        } finally {
            depth--
        }
    }
}

internal class NaturalHeightRecord {
    val height = mutableIntStateOf(0)
    var queried = false

    fun write(value: Int) {
        if (height.intValue != value) height.intValue = value
    }
}

internal fun Modifier.zeroIntrinsicsOrRecordedHeight(record: NaturalHeightRecord): Modifier =
    this.then(RecordedHeightIntrinsicsElement(record))

internal fun Modifier.recordNaturalHeight(
    record: NaturalHeightRecord,
    extraPx: Int = 0,
    write: (Int) -> Unit = record::write,
): Modifier = this.then(RecordNaturalHeightElement(record, extraPx, write))

private data class RecordedHeightIntrinsicsElement(
    val record: NaturalHeightRecord,
) : ModifierNodeElement<RecordedHeightIntrinsicsNode>() {
    override fun create() = RecordedHeightIntrinsicsNode(record)
    override fun update(node: RecordedHeightIntrinsicsNode) {
        node.record = record
    }
}

private class RecordedHeightIntrinsicsNode(
    var record: NaturalHeightRecord,
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        recordedOrZero()

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        recordedOrZero()

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0

    private fun recordedOrZero(): Int {
        if (!NaturalHeightQuery.isActive) return 0
        record.queried = true
        return record.height.intValue
    }
}

private data class RecordNaturalHeightElement(
    val record: NaturalHeightRecord,
    val extraPx: Int,
    val write: (Int) -> Unit,
) : ModifierNodeElement<RecordNaturalHeightNode>() {
    override fun create() = RecordNaturalHeightNode(record, extraPx, write)
    override fun update(node: RecordNaturalHeightNode) {
        node.record = record
        node.extraPx = extraPx
        node.write = write
    }
}

private class RecordNaturalHeightNode(
    var record: NaturalHeightRecord,
    var extraPx: Int,
    var write: (Int) -> Unit,
) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        if (record.queried) {
            try {
                write(NaturalHeightQuery.run { measurable.maxIntrinsicHeight(constraints.maxWidth) } + extraPx)
            } catch (e: IllegalStateException) {
            }
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
