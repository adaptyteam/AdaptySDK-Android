package com.adapty.ui.internal.text

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.PlatformTypeface
import com.adapty.ui.internal.platform.webStub

internal actual fun resolvePlatformTypeface(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): PlatformTypeface {
    webStub("resolvePlatformTypeface")
    return PlatformTypeface()
}

internal actual fun resolveFontFamily(asset: AdaptyUI.FlowConfiguration.Asset.Font, ctx: PlatformContextHandle): FontFamily {
    webStub("resolveFontFamily")
    return FontFamily.Default
}

internal actual fun platformTextStyle(includeFontPadding: Boolean): PlatformTextStyle? = null

internal actual fun naturalLineHeightSp(fontSize: Float, typeface: PlatformTypeface?): Float {
    webStub("naturalLineHeightSp")
    return fontSize * 1.2f
}

internal actual fun capHeightPx(textSizePx: Float, typeface: PlatformTypeface?): Float {
    webStub("capHeightPx")
    return textSizePx * 0.7f
}
