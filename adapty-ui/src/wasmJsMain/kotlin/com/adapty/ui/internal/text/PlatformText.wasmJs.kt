package com.adapty.ui.internal.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface as SkTypeface

@Composable
internal actual fun PlatformBasicText(
    text: String,
    modifier: Modifier,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    maxLines: Int,
    autoSize: TextAutoSize?,
) {
    PlatformBasicText(remember(text) { AnnotatedString(text) }, modifier, style, onTextLayout, overflow, maxLines, emptyMap(), autoSize)
}

/** BasicText with Android's emoji presentation, line breaking, hard-break ellipsis and mixed-size line box. */
@Composable
internal actual fun PlatformBasicText(
    text: AnnotatedString,
    modifier: Modifier,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    maxLines: Int,
    inlineContent: Map<String, InlineTextContent>,
    autoSize: TextAutoSize?,
) {
    val emoji = WebFonts.emojiFamily()
    val presented = remember(text, emoji) { text.withEmojiPresentation(emoji) }
    // Reflow edits depend on the font size, which auto-size changes per layout: not applied with auto-size.
    // Not keyed on the text: the previous edits are shown for a changed text (e.g. a timer tick) until checked.
    val reflow = remember(overflow, autoSize == null) { if (autoSize == null) ReflowState() else null }
    val lineBox = remember { LineBoxState() }
    val userOnTextLayout by rememberUpdatedState(onTextLayout)
    val layoutCallback = remember(presented, reflow) {
        { result: TextLayoutResult ->
            lineBox.result = result
            // A layout about to be replaced by the reflowed text is not reported (e.g. the height fit would count its lines)
            if (reflow?.onLayout(presented, result) != true) userOnTextLayout?.invoke(result)
            Unit
        }
    }
    BasicText(
        text = reflow?.display(presented) ?: presented,
        inlineContent = inlineContent,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = layoutCallback,
        style = style,
        autoSize = autoSize,
        modifier = modifier.then(lineBox.modifier),
    )
}

@Composable
internal actual fun PlatformLabelText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily?,
    textAlign: TextAlign,
    textDecoration: TextDecoration?,
) {
    val emoji = WebFonts.emojiFamily()
    Text(
        text = remember(text, emoji) { AnnotatedString(text).withEmojiPresentation(emoji) },
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        textAlign = textAlign,
        textDecoration = textDecoration,
    )
}

private class ReflowState {
    private var reflow by mutableStateOf<TextReflow?>(null)

    fun display(original: AnnotatedString): AnnotatedString = reflow?.applyTo(original) ?: original

    /** Updates the edits for [result]; true when the displayed text changes (a new layout follows). */
    fun onLayout(original: AnnotatedString, result: TextLayoutResult): Boolean {
        val input = result.layoutInput
        if (reflow?.holdsFor(original, input) != true) reflow = textReflow(original, result)
        return display(original) != input.text
    }
}

private class LineBoxState {
    // Set by BasicText's onTextLayout, which runs inside the measure of the text node this modifier wraps
    var result: TextLayoutResult? = null

    val modifier: Modifier = Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val adjustment = result?.let { androidLineBox(it, ::systemFontMetrics) }
        if (adjustment == null) {
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            layout(placeable.width, adjustment.height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                placeable.place(0, adjustment.top)
            }
        }
    }
}

private var metricsTypeface: SkTypeface? = null
private val metricsBySize = HashMap<Float, Pair<Float, Float>>()

private fun systemFontMetrics(sizePx: Float): Pair<Float, Float>? {
    val typeface = WebFonts.defaultTypeface()?.skia ?: return null
    if (typeface !== metricsTypeface) {
        metricsTypeface = typeface
        metricsBySize.clear()
    }
    return metricsBySize.getOrPut(sizePx) {
        val font = Font(typeface, sizePx)
        try {
            font.metrics.let { it.ascent to it.descent }
        } finally {
            font.close()
        }
    }
}
