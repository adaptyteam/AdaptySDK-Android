@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import android.content.res.Resources
import android.graphics.Rect
import android.text.TextPaint
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformTypeface

// Font / text-metrics seams (Task 6c turns them into expect/actual); TypefaceHolder stays Android.

internal fun resolvePlatformTypeface(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): PlatformTypeface =
    TypefaceHolder.getOrPut(ctx.context, asset)

internal fun resolveFontFamily(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): FontFamily =
    FontFamily(TypefaceHolder.getOrPut(ctx.context, asset))

internal fun platformTextStyle(includeFontPadding: Boolean): PlatformTextStyle? =
    PlatformTextStyle(includeFontPadding = includeFontPadding)

/** Natural line height (descent - ascent + leading) in sp for [fontSize] sp. */
internal fun naturalLineHeightSp(fontSize: Float, typeface: PlatformTypeface?): Float {
    val density = Resources.getSystem().displayMetrics.density
    val paint = TextPaint()
    paint.textSize = fontSize * density
    if (typeface != null) paint.typeface = typeface
    val metrics = paint.fontMetrics
    val naturalPx = metrics.descent - metrics.ascent + metrics.leading
    return naturalPx / density
}

/** Height of the "H" glyph bounds in px at [textSizePx]. */
internal fun capHeightPx(textSizePx: Float, typeface: PlatformTypeface?): Float {
    val paint = TextPaint()
    paint.textSize = textSizePx
    typeface?.let { paint.typeface = it }
    val bounds = Rect()
    paint.getTextBounds("H", 0, 1, bounds)
    return bounds.height().toFloat()
}
