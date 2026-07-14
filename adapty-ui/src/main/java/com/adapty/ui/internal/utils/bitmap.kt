@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image.Dimension
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image.ScaleType
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import java.io.InputStream

internal fun getBitmap(context: Context, image: Asset.Composite<Asset.Image>, boundsW: Int, boundsH: Int, scaleType: ScaleType): Bitmap? {
    val dim: Dimension
    val reqDim: Int
    val coef = when (scaleType) {
        ScaleType.FIT_MAX -> 1
        ScaleType.FIT_MIN -> -1
    }
    if ((boundsW - boundsH) * coef > 0) {
        dim = Dimension.WIDTH
        reqDim = boundsW
    } else {
        dim = Dimension.HEIGHT
        reqDim = boundsH
    }
    return getBitmap(context, image, reqDim, dim)
}

internal fun getImageNaturalSize(context: Context, image: Asset.Composite<Asset.Image>): Pair<Int, Int>? {
    return runCatching { getImageNaturalSize(context, image.main) }.getOrNull()
        ?: image.fallback?.let { runCatching { getImageNaturalSize(context, it) }.getOrNull() }
}

private fun getImageNaturalSize(context: Context, image: Asset.Image): Pair<Int, Int>? {
    return when (val source = image.source) {
        is Asset.Image.Source.Base64Str -> {
            if (source.imageBase64 == null) return null
            val byteArray = runCatching { Base64.decode(source.imageBase64, Base64.DEFAULT) }.getOrNull()
                ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
            if (options.outWidth > 0 && options.outHeight > 0)
                options.outWidth to options.outHeight
            else null
        }
        is Asset.Image.Source.Bitmap -> {
            val bmp = source.bitmap
            if (bmp.width > 0 && bmp.height > 0) bmp.width to bmp.height else null
        }
        is Asset.Image.Source.AndroidAsset -> {
            runCatching {
                context.assets?.open(source.path)?.use { input ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input, null, options)
                    if (options.outWidth > 0 && options.outHeight > 0)
                        options.outWidth to options.outHeight
                    else null
                }
            }.getOrNull()
        }
        is Asset.Image.Source.Uri -> null
    }
}

internal fun getBitmap(context: Context, image: Asset.Composite<Asset.Image>, reqDim: Int = 0, dim: Dimension = Dimension.WIDTH) : Bitmap? {
    return runCatching { getBitmap(context, image.main, reqDim, dim) }
        .getOrElse { e ->
            log(ERROR) {
                "$LOG_PREFIX_ERROR main image error: ${e.localizedMessage}"
            }

            val fallback = image.fallback ?: return@getOrElse null

            runCatching { getBitmap(context, fallback, reqDim, dim) }
                .getOrElse { e ->
                    log(ERROR) {
                        "$LOG_PREFIX_ERROR fallback image error: ${e.localizedMessage}"
                    }
                    null
                }
        }
}

private fun getBitmap(context: Context, image: Asset.Image, reqDim: Int = 0, dim: Dimension = Dimension.WIDTH) : Bitmap? {
    return when (val source = image.source) {
        is Asset.Image.Source.Base64Str -> getBitmap(source, reqDim, dim)
        is Asset.Image.Source.Bitmap -> source.bitmap
        is Asset.Image.Source.Uri -> getBitmap(context, source, reqDim, dim)
        is Asset.Image.Source.AndroidAsset -> getBitmap(context, source, reqDim, dim)
    }
}

private fun getBitmap(source: Asset.Image.Source.Base64Str, reqDim: Int, dim: Dimension) : Bitmap? {
    if (source.imageBase64 == null) return null
    val byteArray = runCatching { Base64.decode(source.imageBase64, Base64.DEFAULT) }.getOrElse { e ->
        log(ERROR) { "$LOG_PREFIX base64 decoding error: ${e.localizedMessage}" }
        return null
    }
    if (reqDim <= 0) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    options.updateInSampleSize(reqDim, dim)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
}

private fun getBitmap(context: Context, source: Asset.Image.Source.Uri, reqDim: Int, dim: Dimension): Bitmap? {
    return getBitmap({ context.contentResolver?.openInputStream(source.uri) }, reqDim, dim)
}

private fun getBitmap(context: Context, source: Asset.Image.Source.AndroidAsset, reqDim: Int, dim: Dimension) : Bitmap? {
    return getBitmap({ context.assets?.open(source.path) }, reqDim, dim)
}

private fun getBitmap(createInputStream: () -> InputStream?, reqDim: Int, dim: Dimension): Bitmap? {
    if (reqDim <= 0) {
        createInputStream()?.use { input ->
            return BitmapFactory.decodeStream(input)
        }
    }
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    createInputStream()?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
    options.updateInSampleSize(reqDim, dim)
    options.inJustDecodeBounds = false
    return createInputStream()?.use { input ->
        BitmapFactory.decodeStream(input, null, options)
    }
}

private fun BitmapFactory.Options.updateInSampleSize(reqDim: Int, dim: Dimension) {
    inSampleSize = calculateInSampleSize(
        when (dim) {
            Dimension.WIDTH -> this.outWidth
            Dimension.HEIGHT -> this.outHeight
        },
        reqDim,
    )
}

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
