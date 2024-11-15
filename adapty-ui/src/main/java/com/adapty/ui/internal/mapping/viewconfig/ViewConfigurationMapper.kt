@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywall
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration
import com.adapty.ui.internal.utils.getAs
import com.adapty.ui.internal.utils.getProductGroupKey

internal typealias JsonObject = Map<String, Any?>
internal typealias JsonArray = Iterable<JsonObject>

internal class ViewConfigurationMapper(
    private val assetMapper: ViewConfigurationAssetMapper,
    private val textMapper: ViewConfigurationTextMapper,
    private val screenMapper: ViewConfigurationScreenMapper,
) {

    private companion object {
        const val DATA = "data"
        const val PAYWALL_BUILDER_ID = "paywall_builder_id"
        const val PAYWALL_BUILDER_CONFIG = "paywall_builder_config"
        const val IS_HARD_PAYWALL = "is_hard_paywall"
        const val TEMPLATE_ID = "template_id"
        const val LANG = "lang"
        const val DEFAULT_LOCALIZATION = "default_localization"
        const val ASSETS = "assets"
        const val LOCALIZATIONS = "localizations"
        const val ID = "id"
        const val TYPE = "type"
        const val URL = "url"
        const val STYLES = "styles"
    }

    fun map(data: JsonObject, paywall: AdaptyPaywall): LocalizedViewConfiguration {
        val data = data.getAs<JsonObject>(DATA) ?: data
        val id = data.getAs<String>(PAYWALL_BUILDER_ID) ?: throw adaptyError(
            message = "id in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val config = data.getAs<JsonObject>(PAYWALL_BUILDER_CONFIG) ?: throw adaptyError(
            message = "config in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        val template = Template.from(config.getAs(TEMPLATE_ID))

        val localesOrderedDesc = setOfNotNull<String>(
            config.getAs(DEFAULT_LOCALIZATION),
            data.getAs(LANG),
        )

        val screenStateMap = mutableMapOf<String, Any>()

        config.getAs<JsonObject>("products")?.getAs<JsonObject>("selected")?.forEach { (k, v) ->
            if (v == null)
                throw adaptyError(
                    message = "styles in ViewConfiguration should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            screenStateMap[getProductGroupKey(k)] = v
        }

        val assets = assetMapper.map(config, localesOrderedDesc)

        return LocalizedViewConfiguration(
            id = id,
            paywall = paywall,
            isHard = config.getAs(IS_HARD_PAYWALL) ?: false,
            isRtl = config.getAs<JsonArray>(LOCALIZATIONS)?.let { localizations ->
                localizations
                    .firstOrNull { it.getAs<String>(ID) == localesOrderedDesc.lastOrNull() }
                    ?.getAs<Boolean>("is_right_to_left")
            } ?: false,
            assets = assets,
            texts = textMapper.map(config, localesOrderedDesc),
            screens = config.getAs<JsonObject>(STYLES)?.let {
                screenMapper.map(it, template, assets, screenStateMap)
            } ?: throw adaptyError(
                message = "styles in ViewConfiguration should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
        )
    }

    fun mapToMediaUrls(data: JsonObject): Pair<String, Set<String>> {
        val id = data.getAs<String>(PAYWALL_BUILDER_ID) ?: throw adaptyError(
            message = "id in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val config = data.getAs<JsonObject>(PAYWALL_BUILDER_CONFIG) ?: return id to emptySet()
        val mediaUrls = mutableSetOf<String>()
        config.getAs<JsonArray>(ASSETS)?.let { assets ->
            mediaUrls += findMediaUrls(assets)
        }
        config.getAs<JsonArray>(LOCALIZATIONS)?.forEach { localization ->
            localization.getAs<JsonArray>(ASSETS)?.let { assets ->
                mediaUrls += findMediaUrls(assets)
            }
        }
        return id to mediaUrls
    }

    private fun findMediaUrls(assets: JsonArray): Set<String> {
        val mediaUrls = mutableSetOf<String>()
        assets.forEach { asset ->
            when (asset.getAs<String>(TYPE)) {
                "image" -> asset.getAs<String>(URL)?.let { url -> mediaUrls.add(url) }
                "video" -> asset.getAs<JsonObject>("image")?.getAs<String>(URL)?.let { url -> mediaUrls.add(url) }
            }
        }
        return mediaUrls
    }
}

internal enum class Template {
    BASIC, FLAT, TRANSPARENT;

    companion object {
        fun from(templateId: String?) = when (templateId) {
            "basic" -> BASIC
            "flat" -> FLAT
            "transparent" -> TRANSPARENT
            else -> throw adaptyError(
                message = "Unsupported templateId: $templateId",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }
}