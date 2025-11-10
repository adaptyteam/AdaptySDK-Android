@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.viewconfig

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.net.toUri
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.getAs
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.utils.DARK_THEME_ASSET_SUFFIX

internal class ViewConfigurationAssetMapper {

    private companion object {
        const val ASSETS = "assets"
        const val LOCALIZATIONS = "localizations"
        const val ID = "id"
        const val CUSTOM_ID = "custom_id"
        const val TYPE = "type"
        const val VALUE = "value"
        const val VALUES = "values"
        const val POINTS = "points"
        const val URL = "url"
        const val PREVIEW_VALUE = "preview_value"
        const val IMAGE = "image"
        const val FAMILY_NAME = "family_name"
        const val RESOURCES = "resources"
        const val WEIGHT = "weight"
        const val IS_ITALIC = "italic"
        const val SIZE = "size"
        const val COLOR = "color"
        const val VIDEO_PREVIEW_ASSET_SUFFIX = "\$\$preview"
    }

    fun map(config: JsonObject, localesOrderedDesc: Set<String>): Map<String, Asset> {
        val rawAssets = config.getAs<JsonArray>(ASSETS)
            ?.mapNotNull { rawAsset ->
                rawAsset.getAs<String>(ID)?.let { id -> id to rawAsset }
            }
            ?.toMap().orEmpty().toMutableMap()

        localesOrderedDesc.forEach { locale ->
            config.getAs<JsonArray>(LOCALIZATIONS)
                ?.firstOrNull { it.getAs<String>(ID) == locale }
                ?.getAs<JsonArray>(ASSETS)
                ?.mapNotNull { rawAsset ->
                    rawAsset.getAs<String>(ID)?.let { id -> id to rawAsset }
                }
                ?.toMap()
                ?.let { localizedRawAssets ->
                    rawAssets.putAll(localizedRawAssets)
                }
        }

        val assets = mutableMapOf<String, Asset>()
        rawAssets.forEach { (id, rawAsset) ->
            val type = rawAsset.getAs<String>(TYPE) ?: return@forEach
            when (type) {
                "image" -> assets[id] = mapImageAsset(rawAsset)
                "video" -> {
                    val (videoAsset, imageAsset) = mapVideoAsset(rawAsset)
                    assets[id] = videoAsset
                    val previewAssetId =
                        if (id.endsWith(DARK_THEME_ASSET_SUFFIX))
                            "${id.substringBeforeLast(DARK_THEME_ASSET_SUFFIX)}${VIDEO_PREVIEW_ASSET_SUFFIX}${DARK_THEME_ASSET_SUFFIX}"
                        else
                            "${id}${VIDEO_PREVIEW_ASSET_SUFFIX}"
                    assets[previewAssetId] = imageAsset
                }
                "color" -> assets[id] = mapColorAsset(rawAsset)
                "linear-gradient", "radial-gradient", "conic-gradient" -> assets[id] = mapGradientAsset(rawAsset, type)
                "font" -> assets[id] = mapFontAsset(rawAsset)
                else -> Unit
            }
        }
        return assets
    }

    private fun mapImageAsset(asset: JsonObject): Asset {
        val url = asset.getAs<String>(URL)
        val customId = asset.getAs<String>(CUSTOM_ID)
        return if (url != null)
            Asset.RemoteImage(
                url,
                asset.getAs<String>(PREVIEW_VALUE)?.let { preview ->
                    Asset.Image(source = Asset.Image.Source.Base64Str(preview))
                },
                customId,
            )
        else
            Asset.Image(source = Asset.Image.Source.Base64Str(asset.getAs<String>(VALUE)), customId)
    }

    private fun mapVideoAsset(asset: JsonObject): Pair<Asset.Video, Asset> {
        val url = asset.getAs<String>(URL)
            ?: throw adaptyError(
                message = "url value for video should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        val image = asset.getAs<JsonObject>(IMAGE)
            ?: throw adaptyError(
                message = "image value for video should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        val customId = asset.getAs<String>(CUSTOM_ID)
        return Asset.Video(Asset.Video.Source.Uri(url.toUri()), customId) to mapImageAsset(image)
    }

    private fun mapColorAsset(asset: JsonObject): Asset.Color =
        Asset.Color(
            asset.getAs<String>(VALUE)
                ?.let(::mapVisualAssetColorString)
                ?: throw adaptyError(
                    message = "color value should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
            asset.getAs<String>(CUSTOM_ID),
        )

    private fun mapGradientAsset(asset: JsonObject, type: String): Asset.Gradient =
        Asset.Gradient(
            when (type) {
                "radial-gradient" -> Asset.Gradient.Type.RADIAL
                "conic-gradient" -> Asset.Gradient.Type.CONIC
                else -> Asset.Gradient.Type.LINEAR
            },
            (asset.getAs<List<*>>(VALUES))?.mapNotNull {
                (it as? Map<*, *>)?.let { value ->
                    val p = (value["p"] as? Number)?.toFloat() ?: return@mapNotNull null
                    val color = Asset.Color(
                        (value["color"] as? String)?.let(::mapVisualAssetColorString)
                            ?: return@mapNotNull null,
                    )
                    Asset.Gradient.Value(p, color)
                }
            } ?: throw adaptyError(
                message = "gradient values should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            asset.getAs<Map<*, *>>(POINTS)?.let {
                val x0 = (it["x0"] as? Number)?.toFloat() ?: return@let null
                val y0 = (it["y0"] as? Number)?.toFloat() ?: return@let null
                val x1 = (it["x1"] as? Number)?.toFloat() ?: return@let null
                val y1 = (it["y1"] as? Number)?.toFloat() ?: return@let null
                Asset.Gradient.Points(x0, y0, x1, y1)
            } ?: throw adaptyError(
                message = "gradient points should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            asset.getAs<String>(CUSTOM_ID),
        )

    private fun mapFontAsset(asset: JsonObject): Asset.Font =
        Asset.Font(
            asset.getAs(FAMILY_NAME) ?: "adapty_system",
            asset.getAs<Iterable<String>>(RESOURCES)?.toList() ?: emptyList(),
            asset.getAs<Number>(WEIGHT)?.toInt() ?: 400,
            asset.getAs(IS_ITALIC) ?: false,
            asset.getAs<Number>(SIZE)?.toFloat() ?: 15f,
            asset.getAs<String?>(COLOR)?.let(::mapVisualAssetColorString),
            asset.getAs<String>(CUSTOM_ID),
        )

    @ColorInt
    private fun mapVisualAssetColorString(colorString: String): Int {
        return try {
            Color.parseColor(
                when (colorString.length) {
                    9 -> rgbaToArgbStr(colorString)
                    else -> colorString
                }
            )
        } catch (e: Exception) {
            throw adaptyError(
                message = "color value should be a valid #RRGGBB or #RRGGBBAA",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                originalError = e
            )
        }
    }

    private fun rgbaToArgbStr(rgbaColorString: String): String {
        return rgbaColorString.toCharArray().let { chars ->
            val a1 = chars[7]
            val a2 = chars[8]
            for (i in 8 downTo 3) {
                chars[i] = chars[i - 2]
            }
            chars[1] = a1
            chars[2] = a2
            String(chars)
        }
    }
}