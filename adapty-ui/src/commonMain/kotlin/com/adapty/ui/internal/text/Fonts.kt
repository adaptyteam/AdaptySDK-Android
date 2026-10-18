@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformTypeface
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

// Font / text-metrics seams.

internal expect fun resolvePlatformTypeface(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): PlatformTypeface

internal expect fun resolveFontFamily(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): FontFamily

internal expect fun platformTextStyle(includeFontPadding: Boolean): PlatformTextStyle?

/** Natural line height (descent - ascent + leading) in sp for [fontSize] sp. */
internal expect fun naturalLineHeightSp(fontSize: Float, typeface: PlatformTypeface?): Float

/** Height of the "H" glyph bounds in px at [textSizePx]. */
internal expect fun capHeightPx(textSizePx: Float, typeface: PlatformTypeface?): Float
