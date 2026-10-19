package com.adapty.ui.internal.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density

/**
 * Text edits that make Skia's line breaking match Android (Minikin, greedy):
 * - [breaks]: spaces turned into hard breaks before words too long for a line. Minikin starts such a word on a new
 *   line and only then breaks it by graphemes; Skia keeps it on the current line and breaks it there.
 * - [cut]: index of a hard break that ends the last visible line while text follows. Android ellipsizes that line
 *   (StaticLayout forces the ellipsis), Skia does not ellipsize at a hard break; "…" is inserted before the break.
 *   The hidden lines stay, so the paragraph keeps Android's width (the widest line of the whole text).
 * Indices refer to [source] ([breaks] replace a char in place, so they stay valid). The edits hold for [layout] only.
 */
internal data class TextReflow(val source: AnnotatedString, val layout: ReflowLayout, val breaks: List<Int>, val cut: Int?) {
    /** Whether the edits still hold for [source] laid out with [input]. */
    fun holdsFor(source: AnnotatedString, input: TextLayoutInput): Boolean =
        source == this.source && ReflowLayout.of(input) == layout

    /** The edits applied to [text] when they fit it structurally (spaces and the break at the same indices). */
    fun applyTo(text: AnnotatedString): AnnotatedString? {
        if (text != source) {
            val t = text.text
            if (breaks.any { it >= t.length || t[it] != ' ' }) return null
            if (cut != null && (cut + 1 >= t.length || t[cut] != '\n')) return null
        }
        return text.withReflow(breaks, cut)
    }
}

/** Layout parameters the edits depend on. */
internal data class ReflowLayout(
    val width: Int,
    val maxLines: Int,
    val style: TextStyle,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver,
) {
    companion object {
        fun of(input: TextLayoutInput) =
            ReflowLayout(input.constraints.maxWidth, input.maxLines, input.style, input.density, input.fontFamilyResolver)
    }
}

/** Edits for [original] laid out as [layout] (of [original] or of an edited copy); null when none are needed. */
internal fun textReflow(original: AnnotatedString, layout: TextLayoutResult): TextReflow? {
    val input = layout.layoutInput
    // Paragraph styles would be split by the edits; the flow text uses none
    if (original.paragraphStyles.isNotEmpty()) return null
    val measurer by lazy { TextMeasurer(input.fontFamilyResolver, input.density, input.layoutDirection, 0) }
    fun measure(text: AnnotatedString) = measurer.measure(
        text, input.style, input.overflow, input.softWrap, input.maxLines, input.placeholders, input.constraints,
        input.layoutDirection, input.density, input.fontFamilyResolver,
    )

    var current = if (input.text == original) layout else measure(original)
    val breaks = ArrayList<Int>()
    if (input.softWrap && input.constraints.hasBoundedWidth) {
        for (pass in 0 until MAX_BREAK_PASSES) {
            val space = tooLongWordSpace(original.text, current) ?: break
            breaks += space
            current = measure(original.withReflow(breaks, null))
        }
    }
    val cut = hardBreakEllipsisCut(original.text, current)
    if (breaks.isEmpty() && cut == null) return null
    return TextReflow(original, ReflowLayout.of(input), breaks, cut)
}

internal fun AnnotatedString.withReflow(breaks: List<Int>, cut: Int?): AnnotatedString {
    if (breaks.isEmpty() && cut == null) return this
    val source = this
    return buildAnnotatedString {
        var pos = 0
        for (b in breaks.sorted()) {
            if (cut != null && b >= cut) break
            append(source, pos, b)
            append(source.styledAs(b, "\n"))
            pos = b + 1
        }
        if (cut != null) {
            append(source, pos, cut)
            append(if (cut > 0) source.styledAs(cut - 1, ELLIPSIS) else AnnotatedString(ELLIPSIS))
            pos = cut
        }
        append(source, pos, source.length)
    }
}

/**
 * The space before the first word Skia started mid-line and broke inside although it does not fit a line of its
 * own; Minikin would move it to the next line first. Only letter/digit words after a plain space are handled.
 */
internal fun tooLongWordSpace(text: String, layout: TextLayoutResult): Int? {
    for (line in 1 until layout.lineCount) {
        val start = layout.getLineStart(line)
        if (start <= 0 || start >= text.length || !isWordChar(text[start - 1]) || !isWordChar(text[start])) continue
        var wordStart = start
        while (wordStart > 0 && isWordChar(text[wordStart - 1])) wordStart--
        if (wordStart > layout.getLineStart(line - 1) && text[wordStart - 1] == ' ') return wordStart - 1
    }
    return null
}

/** Index of the hard break that ends the last visible line when more text follows, if Ellipsis applies. */
internal fun hardBreakEllipsisCut(text: String, layout: TextLayoutResult): Int? {
    val input = layout.layoutInput
    if (input.overflow != TextOverflow.Ellipsis || input.maxLines == Int.MAX_VALUE) return null
    if (layout.lineCount < input.maxLines) return null
    val end = layout.getLineEnd(layout.lineCount - 1)
    val cut = when {
        end < text.length && text[end] == '\n' -> end
        end in 1..text.length && text[end - 1] == '\n' -> end - 1
        else -> return null
    }
    return cut.takeIf { it + 1 < text.length }
}

// Letters/digits of space-separated scripts: a break between two of them is a forced (grapheme) break.
// Scripts written without spaces (dictionary line breaking) and CJK break inside "words" by design.
private fun isWordChar(c: Char) = c.isLetterOrDigit() && c.code < 0x2E80 && NO_SPACE_SCRIPTS.none { c.code in it }

// Thai, Lao, Tibetan, Myanmar, Khmer, New Tai Lue, Tai Tham
private val NO_SPACE_SCRIPTS = listOf(0x0E00..0x0FFF, 0x1000..0x109F, 0x1780..0x17FF, 0x1980..0x19DF, 0x1A20..0x1AAF)

private fun AnnotatedString.styledAs(index: Int, replacement: String): AnnotatedString =
    AnnotatedString(replacement, subSequence(index, index + 1).spanStyles.map { it.copy(start = 0, end = replacement.length) })

private const val ELLIPSIS = "…"
private const val MAX_BREAK_PASSES = 32
