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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.adapty.ui.internal.ui.HStackMinWidthQuery
import com.adapty.ui.internal.ui.VStackMinHeightQuery
import java.text.BreakIterator
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class TightWidthController {
    private var last: TextLayoutResult? = null

    val onTextLayout: (TextLayoutResult) -> Unit = { last = it }

    var proposedWidth: Int = -1
        private set

    fun modifier(pushOut: PushOutInput?): Modifier = TightWidthModifier(pushOut)

    @Suppress("DEPRECATION")
    private inner class TightWidthModifier(private val pushOut: PushOutInput?) : LayoutModifier {
        override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
            proposedWidth = constraints.maxWidth
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
            if (HStackMinWidthQuery.active) 0 else measurable.minIntrinsicWidth(height)

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int =
            measurable.maxIntrinsicWidth(height)

        override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int {
            val wrapped = measurable.minIntrinsicHeight(width)
            return if (VStackMinHeightQuery.active) {
                measurable.minIntrinsicHeight(Constraints.Infinity).coerceAtMost(wrapped)
            } else {
                wrapped
            }
        }

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
        val greedy = layout(proposal, scope)
        if (greedy.truncated()) return null
        val paragraphs = greedy.paragraphLines()
        val candidates = paragraphs.indices.mapNotNull { p -> pushOutCap(greedy, paragraphs[p])?.let { p to it } }
        if (candidates.isEmpty()) return null
        val space by lazy(LazyThreadSafetyMode.NONE) { spaceAdvance(greedy) }
        for ((p, start) in candidates.sortedBy { it.second }) {
            var cap = start
            var capped = layout(cap, scope)
            for (round in 1 until MAX_CAP_ROUNDS) {
                val lines = capped.paragraphLines().getOrNull(p)
                if (lines == null || lines.count() != 2 ||
                    capped.getLineStart(lines.last) < greedy.getLineStart(paragraphs[p].last)
                ) {
                    break
                }
                val next = ceil(capped.getLineRight(lines.first) - capped.getLineLeft(lines.first)).toInt() - 1
                cap = next.takeIf { it in 1 until cap } ?: break
                capped = layout(cap, scope)
            }
            if (pushedOut(greedy, paragraphs, capped, space)) return cap
        }
        return null
    }

    private fun pushedOut(
        greedy: TextLayoutResult,
        paragraphs: List<IntRange>,
        capped: TextLayoutResult,
        space: Float,
    ): Boolean {
        if (capped.truncated() || capped.brokenWords() > greedy.brokenWords()) return false
        val cappedParagraphs = capped.paragraphLines()
        if (cappedParagraphs.size != paragraphs.size) return false
        var moved = 0
        for (p in paragraphs.indices) {
            val old = paragraphs[p]
            val new = cappedParagraphs[p]
            if (old.count() != new.count()) return false
            if (old.zip(new).all { (o, n) -> greedy.getLineStart(o) == capped.getLineStart(n) }) continue
            if (old.count() != 2 || pushOutCap(greedy, old) == null) return false
            val oldBreak = greedy.getLineStart(old.last)
            val newBreak = capped.getLineStart(new.last)
            if (newBreak >= oldBreak || greedy.layoutInput.text.text.substring(newBreak, oldBreak).trimEnd().any { it.isWhitespace() }) {
                return false
            }
            val balanced = pushOutBalanced(
                firstLineWidth = greedy.lineWidth(old.first, space),
                orphanWidth = greedy.lineWidth(old.last, space),
                pushedFirstLineWidth = capped.lineWidth(new.first, space),
                pushedLastLineWidth = capped.lineWidth(new.last, space),
            )
            if (!balanced) return false
            moved++
        }
        return moved > 0
    }

    fun heightAt(width: Int, lines: Int, scope: IntrinsicMeasureScope): Int =
        layout(width, scope, lines).size.height

    private fun layout(width: Int, scope: IntrinsicMeasureScope, lines: Int = maxLines): TextLayoutResult =
        layout(Constraints(maxWidth = width), lines, scope, scope.layoutDirection)

    fun layout(constraints: Constraints, lines: Int, density: Density, layoutDirection: LayoutDirection): TextLayoutResult =
        measurer.measure(
            text = text,
            style = style,
            overflow = TextOverflow.Ellipsis,
            maxLines = lines,
            constraints = constraints,
            layoutDirection = layoutDirection,
            density = density,
        )
}

private fun pushOutCap(r: TextLayoutResult, lines: IntRange): Int? {
    if (lines.count() != 2) return null
    val lastLine = r.layoutInput.text.text.substring(r.getLineStart(lines.last), r.getLineEnd(lines.last, visibleEnd = true))
    return pushOutCap(lastLine, r.getLineRight(lines.first) - r.getLineLeft(lines.first))
}

private fun TextLayoutResult.paragraphLines(): List<IntRange> {
    val text = layoutInput.text.text
    val paragraphs = mutableListOf<IntRange>()
    var first = 0
    for (line in 1 until lineCount) {
        val start = getLineStart(line)
        if (start > 0 && text[start - 1] == '\n') {
            paragraphs += first until line
            first = line
        }
    }
    if (lineCount > 0) paragraphs += first until lineCount
    return paragraphs
}

private fun TextLayoutResult.truncated(): Boolean =
    hasVisualOverflow || (0 until lineCount).any { isLineEllipsized(it) }

private fun TextLayoutResult.brokenWords(): Int {
    val text = layoutInput.text.text
    return (1 until lineCount).count { line ->
        val start = getLineStart(line)
        start in 1 until text.length && text[start - 1].isLetterOrDigit() && text[start].isLetterOrDigit()
    }
}

private fun TextLayoutResult.lineWidth(line: Int, spaceWidth: Float): Float {
    val swallowed = swallowedWhitespace(layoutInput.text.text, getLineEnd(line, visibleEnd = true), getLineEnd(line))
    return getLineRight(line) - getLineLeft(line) + spaceWidth * swallowed
}

internal fun pushOutBalanced(
    firstLineWidth: Float,
    orphanWidth: Float,
    pushedFirstLineWidth: Float,
    pushedLastLineWidth: Float,
): Boolean {
    if (orphanWidth <= 0f || pushedLastLineWidth <= 0f) return false
    val before = firstLineWidth / orphanWidth
    val after = pushedFirstLineWidth / pushedLastLineWidth
    return after >= PUSH_OUT_MIN_RAGGEDNESS && abs(1f - after) <= abs(1f - before)
}

internal fun pushOutCap(lastLine: CharSequence, firstLineWidth: Float): Int? {
    if (!isPushOutOrphan(lastLine)) return null
    return (ceil(firstLineWidth).toInt() - 1).takeIf { it > 0 }
}

internal fun isPushOutOrphan(lastLine: CharSequence): Boolean {
    val orphan = lastLine.trimEnd().toString()
    if (orphan.isEmpty() || orphan.length > PUSH_OUT_MAX_ORPHAN_LENGTH) return false
    if (!Character.isLetterOrDigit(orphan.codePointAt(0))) return false
    var end = 0
    var needsDictionary = false
    while (end < orphan.length) {
        val c = orphan.codePointAt(end)
        if (Character.isLetterOrDigit(c)) {
            needsDictionary = needsDictionary || writtenWithoutSpaces(c)
            end += Character.charCount(c)
        } else if (c in WORD_JOINERS && end + 1 < orphan.length && Character.isLetterOrDigit(orphan.codePointAt(end + 1))) {
            end++
        } else {
            break
        }
    }
    if (hasLetterOrDigit(orphan, end, orphan.length)) return false
    return !needsDictionary || dictionaryWords(orphan.substring(0, end)) == 1
}

private fun dictionaryWords(word: String): Int {
    val words = BreakIterator.getWordInstance()
    words.setText(word)
    var count = 0
    var start = words.first()
    var end = words.next()
    while (end != BreakIterator.DONE) {
        if (hasLetterOrDigit(word, start, end)) count++
        start = end
        end = words.next()
    }
    return count
}

private fun hasLetterOrDigit(text: String, start: Int, end: Int): Boolean {
    var i = start
    while (i < end) {
        val c = text.codePointAt(i)
        if (Character.isLetterOrDigit(c)) return true
        i += Character.charCount(c)
    }
    return false
}

private fun writtenWithoutSpaces(codePoint: Int): Boolean =
    codePoint in 0x0E00..0x0EFF || codePoint in 0x1000..0x109F || codePoint in 0x1780..0x17FF ||
        codePoint in 0x2E80..0x9FFF || codePoint in 0xF900..0xFAFF || codePoint in 0xFF66..0xFF9F ||
        codePoint >= 0x20000

private val WORD_JOINERS = intArrayOf('.'.code, ','.code, '\''.code, '\u2019'.code)

private const val MAX_CAP_ROUNDS = 3
private const val PUSH_OUT_MAX_ORPHAN_LENGTH = 10
private const val PUSH_OUT_MIN_RAGGEDNESS = 0.6f

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
