package com.adapty.ui.internal.text

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Where to place the web paragraph ([top], px) and its height in px so its line box matches Android. */
internal class LineBoxAdjustment(val top: Int, val height: Int)

/**
 * Android line box of a paragraph with a line height whose spans change the font size.
 *
 * Android (Compose LineHeightStyleSpan) derives one target ascent/descent from the first line's font metrics, gives
 * every line exactly that box, and TextLayout pads the paragraph by what the first line's natural ascent/descent
 * stick out of it. Skia sizes every run to the line height around its own font and unions the runs, so a large span
 * yields a different (shorter) box. Lines keep their web positions; the paragraph is shifted and resized.
 * Returns null when the web layout already matches or the case is not modeled (truncated text, unequal line pitch).
 *
 * [fontMetrics] gives the system font's (ascent, descent) in px for a font size in px.
 */
internal fun androidLineBox(result: TextLayoutResult, fontMetrics: (Float) -> Pair<Float, Float>?): LineBoxAdjustment? {
    val input = result.layoutInput
    val style = input.style
    val lineHeightStyle = style.lineHeightStyle ?: return null
    if (!style.lineHeight.isSpecified || !style.fontSize.isSp) return null
    val text = input.text
    val lines = result.lineCount
    if (lines == 0 || result.didOverflowHeight || result.getLineEnd(lines - 1) < text.length) return null

    val density = input.density
    val basePx = with(density) { style.fontSize.toPx() }
    fun px(unit: TextUnit): Float = when {
        unit.isSp -> with(density) { unit.toPx() }
        unit.isEm -> unit.value * basePx
        else -> basePx
    }
    val sized = text.spanStyles.filter { it.item.fontSize.isSpecified && it.start < it.end }
    if (sized.none { abs(px(it.item.fontSize) - basePx) > 0.01f }) return null

    // First line font metrics as Android's FontMetricsInt: max extent over the sizes used on the line
    val sizes = HashSet<Float>()
    for (i in result.getLineStart(0) until result.getLineEnd(0)) {
        if (text[i] == '\n') continue
        sizes += sized.lastOrNull { i >= it.start && i < it.end }?.let { px(it.item.fontSize) } ?: basePx
    }
    var ascent0 = 0
    var descent0 = 0
    for (size in sizes) {
        val (a, d) = fontMetrics(size) ?: return null
        ascent0 = minOf(ascent0, floor(a).toInt())
        descent0 = maxOf(descent0, ceil(d).toInt())
    }
    val height0 = descent0 - ascent0
    if (height0 <= 0) return null

    val topRatio = when (lineHeightStyle.alignment) {
        LineHeightStyle.Alignment.Top -> 0f
        LineHeightStyle.Alignment.Center -> 0.5f
        LineHeightStyle.Alignment.Bottom -> 1f
        LineHeightStyle.Alignment.Proportional -> -1f
        else -> return null
    }
    val trim = lineHeightStyle.trim
    val trimTop = trim == LineHeightStyle.Trim.FirstLineTop || trim == LineHeightStyle.Trim.Both
    val trimBottom = trim == LineHeightStyle.Trim.LastLineBottom || trim == LineHeightStyle.Trim.Both
    val lineHeight = ceil(px(style.lineHeight)).toInt()

    // LineHeightStyleSpan.calculateTargetMetrics (Compose 1.8)
    val ascent: Int
    val descent: Int
    val firstAscent: Int
    val lastDescent: Int
    val diff = lineHeight - height0
    if (lines == 1 && trimTop && trimBottom) {
        ascent = ascent0; descent = descent0; firstAscent = ascent0; lastDescent = descent0
    } else if (lineHeightStyle.mode == LineHeightStyle.Mode.Minimum && diff <= 0) {
        ascent = ascent0; descent = descent0; firstAscent = ascent0; lastDescent = descent0
    } else {
        val ratio = if (topRatio == -1f) abs(ascent0).toFloat() / height0 else topRatio
        val descentDiff = if (diff <= 0) ceil(diff * ratio).toInt() else ceil(diff * (1f - ratio)).toInt()
        descent = descent0 + descentDiff
        ascent = descent - lineHeight
        firstAscent = if (trimTop) ascent0 else ascent
        lastDescent = if (trimBottom) descent0 else descent
    }
    // TextLayout.getLineHeightPaddings
    val topPadding = maxOf(0, firstAscent - ascent0)
    val bottomPadding = maxOf(0, descent0 - lastDescent)

    val pitch = descent - ascent
    for (i in 0 until lines - 1) {
        if (abs(result.getLineBaseline(i + 1) - result.getLineBaseline(i) - pitch) > 1f) return null
    }
    val baseline0 = topPadding - firstAscent
    val height = if (lines == 1) {
        topPadding + lastDescent - firstAscent + bottomPadding
    } else {
        topPadding + (descent - firstAscent) + (lines - 2) * pitch + (lastDescent - ascent) + bottomPadding
    }
    val top = (baseline0 - result.getLineBaseline(0)).roundToInt()
    if (top == 0 && height == result.size.height) return null
    return LineBoxAdjustment(top, height)
}
