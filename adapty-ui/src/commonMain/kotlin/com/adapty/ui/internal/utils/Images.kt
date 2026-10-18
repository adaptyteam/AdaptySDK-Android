@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.InternalAdaptyUiApi

// Image seams.

internal expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** Lenient like `android.util.Base64.DEFAULT`; null when the value cannot be decoded. */
internal expect fun decodeBase64(value: String): ByteArray?

internal expect fun loadImageBitmap(
    ctx: PlatformContextHandle,
    image: Asset.Composite<Asset.Image>,
    boundsW: Int,
    boundsH: Int,
    scaleType: Asset.Image.ScaleType,
): ImageBitmap?

internal expect fun loadImageBitmap(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): ImageBitmap?

internal expect fun imageNaturalSize(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): Pair<Int, Int>?
