@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.utils

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle

// Image seams; Android delegates to the bitmap.kt path.

internal actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()

/** Lenient like `android.util.Base64.DEFAULT`; null when the value cannot be decoded. */
internal actual fun decodeBase64(value: String): ByteArray? =
    runCatching { Base64.decode(value, Base64.DEFAULT) }.getOrNull()

internal actual fun loadImageBitmap(
    ctx: PlatformContextHandle,
    image: Asset.Composite<Asset.Image>,
    boundsW: Int,
    boundsH: Int,
    scaleType: Asset.Image.ScaleType,
): ImageBitmap? =
    getBitmap(ctx.context, image, boundsW, boundsH, scaleType)
        ?.asImageBitmap()

internal actual fun loadImageBitmap(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): ImageBitmap? =
    getBitmap(ctx.context, image)
        ?.asImageBitmap()

internal actual fun imageNaturalSize(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): Pair<Int, Int>? =
    getImageNaturalSize(ctx.context, image)
