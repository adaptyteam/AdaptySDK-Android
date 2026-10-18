package com.adapty.ui.internal.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.webStub

internal actual fun decodeImage(bytes: ByteArray): ImageBitmap? {
    webStub("decodeImage")
    return null
}

internal actual fun decodeBase64(value: String): ByteArray? {
    webStub("decodeBase64")
    return null
}

internal actual fun loadImageBitmap(
    ctx: PlatformContextHandle,
    image: Asset.Composite<Asset.Image>,
    boundsW: Int,
    boundsH: Int,
    scaleType: Asset.Image.ScaleType,
): ImageBitmap? {
    webStub("loadImageBitmap")
    return null
}

internal actual fun loadImageBitmap(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): ImageBitmap? {
    webStub("loadImageBitmap")
    return null
}

internal actual fun imageNaturalSize(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): Pair<Int, Int>? {
    webStub("imageNaturalSize")
    return null
}
