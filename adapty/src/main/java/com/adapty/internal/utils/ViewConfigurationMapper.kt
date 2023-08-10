package com.adapty.internal.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.ViewConfigurationConfig
import com.adapty.internal.data.models.ViewConfigurationDto
import com.adapty.models.AdaptyViewConfiguration
import com.adapty.models.AdaptyViewConfiguration.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ViewConfigurationMapper {

    @JvmSynthetic
    fun map(viewConfig: ViewConfigurationDto): AdaptyViewConfiguration {
        val config = viewConfig.config

        return AdaptyViewConfiguration(
            id = viewConfig.id ?: throw AdaptyError(
                message = "id in ViewConfiguration should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            isHard = config?.isHard ?: false,
            templateId = config?.templateId,
            defaultLocalization = config?.defaultLocalization,
            privacyUrlId = config?.privacy?.url,
            termsUrlId = config?.terms?.url,
            assets = mapVisualAssets(config?.assets),
            localizations = config?.localizations?.associate { localization ->
                if (localization.id == null) {
                    throw AdaptyError(
                        message = "id in Localization should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }

                val strings = localization.strings?.associate { str ->
                    if (str.id == null || str.value == null) {
                        throw AdaptyError(
                            message = "id and value in strings in Localization should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        )
                    }

                    str.id to str.value
                }.orEmpty()

                val assets = mapVisualAssets(localization.assets)

                localization.id to Localization(strings, assets)
            }.orEmpty(),
            styles = config?.styles?.mapValues { (_, styleValue) ->
                (styleValue as? Map<*, *>)?.let { map ->
                    map.keys.filterIsInstance<String>().mapNotNull { key ->
                        mapVisualStyleComponents(map, key)?.let { value -> key to value }
                    }.toMap()
                }.orEmpty()
            }.orEmpty()
        )
    }

    private fun mapVisualStyleComponents(map: Map<*, *>, key: String): Any? =
        when (val value = map[key]) {
            is String -> Component.Reference(value)
            is Map<*, *> -> {
                when {
                    key == "custom_properties" -> {
                        value.keys.filterIsInstance<String>().mapNotNull { customPropKey ->
                            mapVisualStyleComponents(
                                value,
                                customPropKey
                            )?.let { value -> customPropKey to value }
                        }.toMap()
                    }
                    value["type"] == "text" -> mapTextComponent(value)
                    value["type"] == "text-rows" -> mapTextCollectionComponent(value)
                    else -> null
                }
            }
            else -> null
        }

    private fun mapTextComponent(value: Map<*, *>) =
        Component.Text(
            stringId = (value["string_id"] as? String) ?: throw AdaptyError(
                message = "stringId in Text should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            fontId = (value["font"] as? String) ?: throw AdaptyError(
                message = "fontId in Text should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            size = value["size"] as? Float,
            textColorId = value["color"] as? String,
        )

    private fun mapTextCollectionComponent(value: Map<*, *>): Component.TextCollection {
        val texts = (value["rows"] as? Collection<*>)?.mapNotNull { row ->
            (row as? Map<*, *>)?.let { rowMap ->
                Component.Text(
                    stringId = (rowMap["string_id"] as? String) ?: throw AdaptyError(
                        message = "stringId in TextCollection should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    fontId = (value["font"] as? String) ?: throw AdaptyError(
                        message = "fontId in TextCollection should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    size = (rowMap["size"] as? Float) ?: (value["size"] as? Float),
                    textColorId = (rowMap["color"] as? String) ?: (value["color"] as? String),
                )
            }
        }?.takeIf { it.isNotEmpty() } ?: throw AdaptyError(
            message = "rows in TextCollection should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        return Component.TextCollection(texts)
    }

    private fun mapVisualAssets(assets: List<ViewConfigurationConfig.Asset>?): Map<String, Asset> {
        return assets?.mapNotNull { asset ->
            if (asset.id != null && asset.type != null) {
                (when (asset.type) {
                    "image" -> Asset.Image(asset.value as? String)
                    "color" -> Asset.Color(
                        (asset.value as? String)?.let(::mapVisualAssetColorString)
                            ?: throw AdaptyError(
                                message = "color value should not be null",
                                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                            )
                    )
                    "font" -> Asset.Font(
                        (asset.value as? String) ?: throw AdaptyError(
                            message = "font value should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        asset.style ?: throw AdaptyError(
                            message = "font style should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        asset.size,
                        asset.color?.let(::mapVisualAssetColorString),
                    )
                    else -> null
                })?.let { asset.id to it }
            } else
                null
        }?.toMap().orEmpty()
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
            throw AdaptyError(
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