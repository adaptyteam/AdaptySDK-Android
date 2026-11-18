package com.adapty.ui.internal.text

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isSpecified
import kotlin.math.roundToInt

internal fun AnnotatedString.toSpanned(
    density: Density,
    resolveTypeface: (FontFamily) -> Typeface?,
): Spanned {
    val spannable = SpannableString(text)
    spanStyles.forEach { range ->
        val style = range.item
        fun set(span: Any) = spannable.setSpan(span, range.start, range.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (style.color.isSpecified) set(ForegroundColorSpan(style.color.toArgb()))
        if (style.background.isSpecified) set(BackgroundColorSpan(style.background.toArgb()))
        if (style.fontSize.isSpecified) set(AbsoluteSizeSpan(with(density) { style.fontSize.toPx() }.roundToInt()))
        style.textDecoration?.let { decoration ->
            if (TextDecoration.Underline in decoration) set(UnderlineSpan())
            if (TextDecoration.LineThrough in decoration) set(StrikethroughSpan())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            style.fontFamily?.let(resolveTypeface)?.let { set(TypefaceSpan(it)) }
        }
    }
    return spannable
}
