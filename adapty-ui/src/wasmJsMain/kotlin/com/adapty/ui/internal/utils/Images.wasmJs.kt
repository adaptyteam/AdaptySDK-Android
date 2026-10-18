@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image.Dimension
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image.ScaleType
import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.log
import com.adapty.ui.internal.platform.webStub
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.roundToInt

// Image seams on Skia; mirrors the Android bitmap.kt dispatch for the sources web can serve.

internal actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    if (bytes.isEmpty()) null
    else runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
private val lenientBase64 = Base64.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

/** Lenient like `android.util.Base64.DEFAULT`: whitespace is skipped, padding is optional. */
@OptIn(ExperimentalEncodingApi::class)
internal actual fun decodeBase64(value: String): ByteArray? {
    val compact = buildString(value.length) { value.forEach { if (!it.isWhitespace()) append(it) } }
    return runCatching { lenientBase64.decode(compact) }.getOrNull()
}

internal actual fun loadImageBitmap(
    ctx: PlatformContextHandle,
    image: Asset.Composite<Asset.Image>,
    boundsW: Int,
    boundsH: Int,
    scaleType: ScaleType,
): ImageBitmap? {
    // Same requested-dimension choice as Android getBitmap(context, image, boundsW, boundsH, scaleType)
    val coef = when (scaleType) {
        ScaleType.FIT_MAX -> 1
        ScaleType.FIT_MIN -> -1
    }
    return if ((boundsW - boundsH) * coef > 0) {
        loadComposite(image, boundsW, Dimension.WIDTH)
    } else {
        loadComposite(image, boundsH, Dimension.HEIGHT)
    }
}

internal actual fun loadImageBitmap(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): ImageBitmap? =
    loadComposite(image, 0, Dimension.WIDTH)

internal actual fun imageNaturalSize(ctx: PlatformContextHandle, image: Asset.Composite<Asset.Image>): Pair<Int, Int>? =
    naturalSize(image.main) ?: image.fallback?.let(::naturalSize)

/** Like Android getBitmap: the fallback is used only when loading the main image fails (throws), not when it yields null. */
private fun loadComposite(image: Asset.Composite<Asset.Image>, reqDim: Int, dim: Dimension): ImageBitmap? =
    runCatching { loadSingle(image.main, reqDim, dim) }
        .getOrElse { e ->
            log(ERROR) { "$LOG_PREFIX_ERROR main image error: ${e.message}" }
            val fallback = image.fallback ?: return@getOrElse null
            runCatching { loadSingle(fallback, reqDim, dim) }
                .getOrElse { e ->
                    log(ERROR) { "$LOG_PREFIX_ERROR fallback image error: ${e.message}" }
                    null
                }
        }

private fun loadSingle(image: Asset.Image, reqDim: Int, dim: Dimension): ImageBitmap? {
    return when (val source = image.source) {
        is Asset.Image.Source.Base64Str -> {
            if (source.imageBase64 == null) return null
            val bytes = decodeBase64(source.imageBase64) ?: run {
                log(ERROR) { "$LOG_PREFIX base64 decoding error: bad base-64" }
                return null
            }
            decodeSampled(bytes, reqDim, dim)
        }
        // The web Bitmap shim carries no pixels
        is Asset.Image.Source.Bitmap -> null
        // No packaged assets / content resolver on web: fail like a missing stream on Android so the fallback is tried
        is Asset.Image.Source.AndroidAsset -> throw IllegalStateException("android assets are not available on web")
        is Asset.Image.Source.Uri -> {
            webStub("loadImageBitmap.Uri")
            throw IllegalStateException("uri images are not supported on web: ${source.uri}")
        }
    }
}

private fun naturalSize(image: Asset.Image): Pair<Int, Int>? {
    val source = image.source as? Asset.Image.Source.Base64Str ?: return null
    val bytes = source.imageBase64?.let(::decodeBase64) ?: return null
    return runCatching {
        withCodec(bytes) { codec -> if (codec.width > 0 && codec.height > 0) codec.width to codec.height else null }
    }.getOrNull()
}

/**
 * Decodes [bytes], downsampling like BitmapFactory's inSampleSize for [reqDim] along [dim].
 * WebP scales natively (rounded size, Skia SkWebpCodec); other formats use integer division (SkSampledCodec).
 */
private fun decodeSampled(bytes: ByteArray, reqDim: Int, dim: Dimension): ImageBitmap? {
    if (bytes.isEmpty()) return null
    val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return null
    return runCatching {
        val sample = if (reqDim <= 0) 1 else calculateInSampleSize(
            when (dim) {
                Dimension.WIDTH -> image.width
                Dimension.HEIGHT -> image.height
            },
            reqDim,
        )
        if (sample <= 1) return@runCatching image.toComposeImageBitmap()

        val isWebp = runCatching {
            withCodec(bytes) { it.encodedImageFormat == EncodedImageFormat.WEBP }
        }.getOrDefault(false)
        val w = scaledDim(image.width, sample, isWebp)
        val h = scaledDim(image.height, sample, isWebp)
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.drawImageRect(
            image,
            Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
            Rect.makeWH(w.toFloat(), h.toFloat()),
            SamplingMode.LINEAR,
            Paint(),
            true,
        )
        surface.makeImageSnapshot().toComposeImageBitmap()
    }.getOrNull()
}

private inline fun <R> withCodec(bytes: ByteArray, block: (Codec) -> R): R {
    val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
    try {
        return block(codec)
    } finally {
        codec.close()
    }
}

private fun scaledDim(src: Int, sample: Int, isWebp: Boolean): Int =
    if (isWebp) max(1, (src.toFloat() / sample).roundToInt()) else max(1, src / sample)

// Same as Android bitmap.kt calculateInSampleSize
private fun calculateInSampleSize(initialDimValue: Int, reqDimValue: Int): Int {
    var inSampleSize = 1
    if (initialDimValue > reqDimValue) {
        val half: Int = initialDimValue / 2

        while (half / inSampleSize >= reqDimValue) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
