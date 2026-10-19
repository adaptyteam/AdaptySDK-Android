package com.adapty.ui.internal.text

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
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

// BasicText seam: Android calls BasicText as is; web adds the text-layout behavior Android has and Skia lacks.

@Composable
internal expect fun PlatformBasicText(
    text: String,
    modifier: Modifier,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    maxLines: Int,
    autoSize: TextAutoSize?,
)

@Composable
internal expect fun PlatformBasicText(
    text: AnnotatedString,
    modifier: Modifier,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    maxLines: Int,
    inlineContent: Map<String, InlineTextContent>,
    autoSize: TextAutoSize?,
)

/** Material3 Text seam for plain labels (wheel pickers). */
@Composable
internal expect fun PlatformLabelText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily?,
    textAlign: TextAlign,
    textDecoration: TextDecoration?,
)
