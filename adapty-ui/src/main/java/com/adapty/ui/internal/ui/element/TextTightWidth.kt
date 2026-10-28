package com.adapty.ui.internal.ui.element

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class TightWidthController {
    private var last: TextLayoutResult? = null

    val onTextLayout: (TextLayoutResult) -> Unit = { last = it }

    fun modifier(pushOut: PushOutInput?): Modifier = TightWidthModifier(pushOut)

    @Suppress("DEPRECATION")
    private inner class TightWidthModifier(private val pushOut: PushOutInput?) : LayoutModifier {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            val cap = if (pushOut != null && constraints.hasBoundedWidth) {
                pushOut.capFor(constraints.maxWidth, measurable, this)
            } else {
                null
            }
            val childConstraints = if (cap == null) constraints else
                constraints.copy(minWidth = min(constraints.minWidth, cap), maxWidth = cap)
            val placeable = measurable.measure(childConstraints)
            val r = last
            if (r == null || r.lineCount == 0 || r.size.width != placeable.width) {
                return layout(placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth), placeable.height) {
                    placeable.place(0, 0)
                }
            }
            val box = tightTextBox(lineExtents(r), placeable.width, constraints.minWidth, constraints.maxWidth)
            return layout(box.width, placeable.height) { placeable.place(box.offsetX, 0) }
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
            measurable.minIntrinsicWidth(height)

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
            measurable.maxIntrinsicWidth(height)

        override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
            measurable.minIntrinsicHeight(width)

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
            measurable.maxIntrinsicHeight(width)
    }
}

internal class PushOutInput(
    private val text: AnnotatedString,
    private val style: TextStyle,
    private val maxLines: Int,
    private val measurer: TextMeasurer,
) {
    fun capFor(proposal: Int, measurable: Measurable, scope: MeasureScope): Int? {
        val unwrapped = measurable.maxIntrinsicWidth(Constraints.Infinity)
        if (unwrapped <= proposal || unwrapped > 2 * proposal) return null
        var r = layout(proposal, scope)
        val lines = r.lineCount
        var cap: Int? = null
        for (round in 0 until MAX_ROUNDS) {
            val next = pushOutCap(r) ?: return cap
            if (cap != null && next >= cap) return cap
            val capped = layout(next, scope)
            if (capped.lineCount > lines || capped.hasVisualOverflow || capped.isLineEllipsized(capped.lineCount - 1)) {
                return cap
            }
            cap = next
            r = capped
        }
        return cap
    }

    fun heightAt(width: Int, lines: Int, scope: IntrinsicMeasureScope): Int =
        layout(width, scope, lines).size.height

    private fun layout(width: Int, scope: IntrinsicMeasureScope, lines: Int = maxLines): TextLayoutResult =
        measurer.measure(
            text = text,
            style = style,
            overflow = TextOverflow.Ellipsis,
            maxLines = lines,
            constraints = Constraints(maxWidth = width),
            layoutDirection = scope.layoutDirection,
            density = scope,
        )
}

private fun pushOutCap(r: TextLayoutResult): Int? {
    if (r.lineCount != 2 || r.hasVisualOverflow || r.isLineEllipsized(1)) return null
    val lastLine = r.layoutInput.text.text.substring(r.getLineStart(1), r.getLineEnd(1, visibleEnd = true))
    return pushOutCap(lastLine, r.getLineRight(0) - r.getLineLeft(0))
}

internal fun pushOutCap(lastLine: CharSequence, firstLineWidth: Float): Int? {
    val word = lastLine.trim()
    if (word.isEmpty() || word.any { it.isWhitespace() }) return null
    return (ceil(firstLineWidth).toInt() - 1).takeIf { it > 0 }
}

private const val MAX_ROUNDS = 3

internal class LineExtents(val width: Float, val alignFraction: Float)

internal class TightTextBox(val width: Int, val offsetX: Int)

private fun lineExtents(r: TextLayoutResult): LineExtents {
    val text = r.layoutInput.text.text
    val spaceWidth by lazy(LazyThreadSafetyMode.NONE) { spaceAdvance(r) }
    var width = 0f
    var widestVisible = -1f
    var widestLeft = 0f
    for (i in 0 until r.lineCount) {
        val left = r.getLineLeft(i)
        val visible = r.getLineRight(i) - left
        var lineWidth = visible
        if (i < r.lineCount - 1) {
            val swallowed = swallowedWhitespace(text, r.getLineEnd(i, visibleEnd = true), r.getLineEnd(i))
            if (swallowed > 0) lineWidth += spaceWidth * swallowed
        }
        width = max(width, lineWidth)
        if (visible > widestVisible) {
            widestVisible = visible
            widestLeft = left
        }
    }
    val slack = r.size.width - widestVisible
    val alignFraction = if (slack < 1f) 0f else (widestLeft / slack).coerceIn(0f, 1f)
    return LineExtents(width, alignFraction)
}

internal fun swallowedWhitespace(text: CharSequence, visibleEnd: Int, end: Int): Int {
    var count = 0
    for (k in visibleEnd until min(end, text.length)) {
        val c = text[k]
        if (c != '\n' && c != '\r' && c.isWhitespace()) count++
    }
    return count
}

private fun spaceAdvance(r: TextLayoutResult): Float {
    val text = r.layoutInput.text.text
    for (i in 0 until r.lineCount) {
        for (k in r.getLineStart(i) until r.getLineEnd(i, visibleEnd = true)) {
            if (text[k] != ' ') continue
            val width = runCatching { r.getBoundingBox(k).width }.getOrNull() ?: continue
            if (width > 0f) return width
        }
    }
    val fontSize = r.layoutInput.style.fontSize
    val density = r.layoutInput.density
    return if (fontSize.isSp) with(density) { fontSize.toPx() } * 0.25f else 4f * density.density
}

internal fun tightTextBox(
    extents: LineExtents,
    placeableWidth: Int,
    minWidth: Int,
    maxWidth: Int = placeableWidth,
): TightTextBox {
    val width = min(ceil(extents.width).toInt(), placeableWidth).coerceAtLeast(min(minWidth, maxWidth))
    val offsetX = -(extents.alignFraction * (placeableWidth - width)).roundToInt()
    return TightTextBox(width, offsetX)
}

internal fun combineTextLayout(
    a: ((TextLayoutResult) -> Unit)?,
    b: ((TextLayoutResult) -> Unit)?,
): ((TextLayoutResult) -> Unit)? = when {
    a == null -> b
    b == null -> a
    else -> { r -> a(r); b(r) }
}
