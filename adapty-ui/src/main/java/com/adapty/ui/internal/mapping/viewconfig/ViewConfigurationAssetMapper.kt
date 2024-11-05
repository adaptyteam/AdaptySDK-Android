@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.viewconfig

import android.graphics.Color
import androidx.annotation.ColorInt
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.utils.getAs

internal class ViewConfigurationAssetMapper {

    private companion object {
        const val ASSETS = "assets"
        const val LOCALIZATIONS = "localizations"
        const val ID = "id"
        const val TYPE = "type"
        const val VALUE = "value"
        const val VALUES = "values"
        const val POINTS = "points"
        const val URL = "url"
        const val PREVIEW_VALUE = "preview_value"
        const val FAMILY_NAME = "family_name"
        const val RESOURCES = "resources"
        const val WEIGHT = "weight"
        const val IS_ITALIC = "italic"
        const val SIZE = "size"
        const val COLOR = "color"
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

        return rawAssets.mapNotNull { (id, rawAsset) ->
            mapVisualAsset(rawAsset)?.let { asset -> id to asset }
        }.toMap()
    }

    private fun mapVisualAsset(asset: JsonObject): Asset? {
        val type = asset.getAs<String>(TYPE)
        val value = asset.getAs<String>(VALUE)

        if (type == null) return null

        return (when (type) {
            "image" -> {
                val url = asset.getAs<String>(URL)
                if (url != null)
                    Asset.RemoteImage(
                        url,
                        asset.getAs<String>(PREVIEW_VALUE)?.let { preview ->
                            Asset.Image(source = Asset.Image.Source.Base64Str(preview))
                        }
                    )
                else
                    Asset.Image(source = Asset.Image.Source.Base64Str(value))
            }
            "color" -> Asset.Color(
                value?.let(::mapVisualAssetColorString)
                    ?: throw adaptyError(
                        message = "color value should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
            )
            "linear-gradient", "radial-gradient", "conic-gradient" -> Asset.Gradient(
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
                                ?: return@mapNotNull null
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
            )
            "font" -> Asset.Font(
                asset.getAs(FAMILY_NAME) ?: "adapty_system",
                asset.getAs<Iterable<String>>(RESOURCES)?.toList() ?: emptyList(),
                asset.getAs<Number>(WEIGHT)?.toInt() ?: 400,
                asset.getAs(IS_ITALIC) ?: false,
                asset.getAs<Number>(SIZE)?.toFloat() ?: 15f,
                asset.getAs<String?>(COLOR)?.let(::mapVisualAssetColorString),
            )
            else -> null
        })
    }

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