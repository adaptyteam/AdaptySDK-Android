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
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

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

    private var changed = false

    var onRecordedChanged: (() -> Unit)? = null

    fun write(value: Int) {
        if (height.intValue != value) {
            height.intValue = value
            changed = true
        }
    }

    fun reportChange() {
        if (!changed) return
        changed = false
        onRecordedChanged?.invoke()
    }

    fun recordedOrZero(): Int {
        if (!NaturalHeightQuery.isActive) return 0
        queried = true
        return height.intValue
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
    record: NaturalHeightRecord,
) : Modifier.Node(), LayoutModifierNode {
    private val invalidator = RecordChangeInvalidator(this)

    var record: NaturalHeightRecord = record
        set(value) {
            if (field.onRecordedChanged === invalidator.invalidate) field.onRecordedChanged = null
            field = value
            if (isAttached) value.onRecordedChanged = invalidator.invalidate
        }

    override fun onAttach() {
        record.onRecordedChanged = invalidator.invalidate
    }

    override fun onDetach() {
        if (record.onRecordedChanged === invalidator.invalidate) record.onRecordedChanged = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        record.recordedOrZero()

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        record.recordedOrZero()

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
}

internal fun Modifier.contentIntrinsicsOrRecordedHeight(
    record: NaturalHeightRecord,
    onSizeUnknown: () -> Unit,
): Modifier = this.then(ContentIntrinsicsElement(record, onSizeUnknown))

private data class ContentIntrinsicsElement(
    val record: NaturalHeightRecord,
    val onSizeUnknown: () -> Unit,
) : ModifierNodeElement<ContentIntrinsicsNode>() {
    override fun create() = ContentIntrinsicsNode(record, onSizeUnknown)
    override fun update(node: ContentIntrinsicsNode) {
        node.record = record
        node.onSizeUnknown = onSizeUnknown
    }
}

private class ContentIntrinsicsNode(
    var record: NaturalHeightRecord,
    var onSizeUnknown: () -> Unit,
) : Modifier.Node(), LayoutModifierNode {
    private var reported = false

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        contentOr({ measurable.minIntrinsicHeight(width) }) { record.recordedOrZero() }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        contentOr({ measurable.maxIntrinsicHeight(width) }) { record.recordedOrZero() }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        contentOr({ measurable.minIntrinsicWidth(height) }) { 0 }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
        contentOr({ measurable.maxIntrinsicWidth(height) }) { 0 }

    private inline fun contentOr(query: () -> Int, fallback: () -> Int): Int =
        try {
            query()
        } catch (e: IllegalStateException) {
            if (!reported) {
                reported = true
                onSizeUnknown()
            }
            fallback()
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
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
            record.reportChange()
        }
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

internal class RecordChangeInvalidator(private val node: LayoutModifierNode) {
    private var budget = SAME_PASS_BUDGET

    val invalidate: () -> Unit = invalidate@{
        val modifierNode = node.node
        if (!modifierNode.isAttached) return@invalidate
        if (budget > 0) {
            if (budget == SAME_PASS_BUDGET) modifierNode.coroutineScope.launch { budget = SAME_PASS_BUDGET }
            budget--
            node.invalidateMeasurement()
        } else {
            modifierNode.coroutineScope.launch { if (modifierNode.isAttached) node.invalidateMeasurement() }
        }
    }

    private companion object {
        const val SAME_PASS_BUDGET = 8
    }
}
