package com.adapty.ui.internal.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

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
    BasicText(
        text = text,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
        style = style,
        autoSize = autoSize,
        modifier = modifier,
    )
}

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
    BasicText(
        text = text,
        inlineContent = inlineContent,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
        style = style,
        autoSize = autoSize,
        modifier = modifier,
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
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        textAlign = textAlign,
        textDecoration = textDecoration,
    )
}
