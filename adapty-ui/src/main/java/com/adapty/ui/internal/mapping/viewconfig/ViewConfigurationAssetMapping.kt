@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.viewconfig

import androidx.core.net.toUri
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.getAs
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.utils.DARK_THEME_ASSET_SUFFIX
import com.adapty.ui.internal.utils.parseColorInt

private const val ASSETS = "assets"
private const val LOCALIZATIONS = "localizations"
private const val ID = "id"
private const val CUSTOM_ID = "custom_id"
private const val FALLBACK_ASSET_ID = "fallback_asset_id"
private const val TYPE = "type"
private const val VALUE = "value"
private const val VALUES = "values"
private const val POINTS = "points"
private const val URL = "url"
private const val PREVIEW_VALUE = "preview_value"
private const val IMAGE = "image"
private const val V_RES = "v_res"
private const val H_RES = "h_res"
private const val FAMILY_NAME = "family_name"
private const val RESOURCES = "resources"
private const val WEIGHT = "weight"
private const val IS_ITALIC = "italic"
private const val SIZE = "size"
private const val COLOR = "color"
private const val LETTER_SPACING = "letter_spacing"
private const val LINE_HEIGHT = "line_height"
private const val VIDEO_PREVIEW_ASSET_SUFFIX = "\$\$preview"

internal fun mapAssets(config: JsonObject, localesOrderedDesc: Set<String>): Map<String, Asset> {
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
            else -> assets[id] = Asset.Unknown(
                rawAsset.getAs<String>(FALLBACK_ASSET_ID),
                rawAsset.getAs<String>(CUSTOM_ID),
            )
        }
    }
    return assets
}

private fun mapImageAsset(asset: JsonObject): Asset {
    val value = asset.getAs<String>(VALUE)
    val customId = asset.getAs<String>(CUSTOM_ID)
    if (!value.isNullOrEmpty())
        return Asset.Image(source = Asset.Image.Source.Base64Str(value), customId)
    val url = asset.getAs<String>(URL) ?: return Asset.Image(source = Asset.Image.Source.Base64Str(null), customId)
    return Asset.RemoteImage(
        url,
        asset.getAs<String>(PREVIEW_VALUE)?.let { preview ->
            Asset.Image(source = Asset.Image.Source.Base64Str(preview))
        },
        customId,
    )
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
    val vRes = asset.getAs<Number>(V_RES)?.toInt() ?: 0
    val hRes = asset.getAs<Number>(H_RES)?.toInt() ?: 0
    return Asset.Video(Asset.Video.Source.Uri(url.toUri()), vRes, hRes, customId) to mapImageAsset(image)
}

private fun mapColorAsset(asset: JsonObject): Asset.Color =
    Asset.Color(
        asset.getAs<String>(VALUE)
            ?.parseColorInt()
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
                    (value["color"] as? String)?.parseColorInt()
                        ?: return@mapNotNull null,
                )
                Asset.Gradient.Value(p, color)
            }
        }?.sortedBy { it.p } ?: throw adaptyError(
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
        asset.getAs<Iterable<String>>(FAMILY_NAME)?.firstOrNull()
            ?: asset.getAs(FAMILY_NAME)
            ?: "adapty_system",
        asset.getAs<Iterable<String>>(RESOURCES)?.toList() ?: emptyList(),
        asset.getAs<Number>(WEIGHT)?.toInt() ?: 400,
        asset.getAs(IS_ITALIC) ?: false,
        asset.getAs<Number>(SIZE)?.toFloat() ?: 15f,
        asset.getAs<String?>(COLOR)?.parseColorInt() ?: 0xFF000000.toInt(),
        asset.getAs<Number>(LETTER_SPACING)?.toFloat(),
        asset.getAs<Number>(LINE_HEIGHT)?.toFloat(),
        asset.getAs<String>(CUSTOM_ID),
    )
