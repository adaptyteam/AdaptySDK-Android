@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformTypeface
import com.adapty.ui.internal.platform.systemDensity
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.FontHinting
import kotlin.math.ceil
import kotlin.math.floor

// Font / text-metrics seams backed by the fonts registered via AdaptyUIWeb.registerFonts.
// Like Android (no font resources on web), every asset resolves to the system font at its weight/italic.

internal actual fun resolvePlatformTypeface(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): PlatformTypeface =
    WebFonts.typeface(asset.weight, asset.isItalic) ?: PlatformTypeface(null)

internal actual fun resolveFontFamily(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): FontFamily =
    WebFonts.family(asset.weight, asset.isItalic) ?: FontFamily.Default

internal actual fun platformTextStyle(includeFontPadding: Boolean): PlatformTextStyle? = null

private fun skiaFont(typeface: PlatformTypeface?, sizePx: Float): Font? {
    val sk = (typeface ?: WebFonts.defaultTypeface())?.skia ?: return null
    // Android Paint defaults: anti-aliased, normal hinting, no subpixel positioning
    return Font(sk, sizePx).apply {
        edging = FontEdging.ANTI_ALIAS
        hinting = FontHinting.NORMAL
        isSubpixel = false
    }
}

/** Natural line height (descent - ascent + leading) in sp for [fontSize] sp, measured at device px like Android. */
internal actual fun naturalLineHeightSp(fontSize: Float, typeface: PlatformTypeface?): Float {
    val density = systemDensity()
    val font = skiaFont(typeface, fontSize * density) ?: return fontSize * 1.2f
    val m = font.metrics
    return (m.descent - m.ascent + m.leading) / density
}

/** Height of the "H" glyph bounds in px at [textSizePx] (rounded out like Paint.getTextBounds). */
internal actual fun capHeightPx(textSizePx: Float, typeface: PlatformTypeface?): Float {
    val font = skiaFont(typeface, textSizePx) ?: return textSizePx * 0.7f
    val bounds = font.measureText("H")
    return ceil(bounds.bottom) - floor(bounds.top)
}
