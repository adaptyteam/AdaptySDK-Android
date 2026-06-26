@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.internal.utils.getAs
import com.adapty.models.AdaptyFlow
import com.adapty.ui.AdaptyUI.FlowConfiguration
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.ElementMapper
import com.adapty.ui.internal.mapping.element.UIElementFactory
import com.adapty.ui.internal.mapping.element.container
import com.adapty.ui.internal.mapping.element.hasVideoSupport
import com.adapty.ui.internal.mapping.element.leaf
import com.adapty.ui.internal.mapping.element.toBoxElement
import com.adapty.ui.internal.mapping.element.toButtonElement
import com.adapty.ui.internal.mapping.element.toColumnElement
import com.adapty.ui.internal.mapping.element.toCompactDateTimePickerElement
import com.adapty.ui.internal.mapping.element.toGraphicalDateTimePickerElement
import com.adapty.ui.internal.mapping.element.toHStackElement
import com.adapty.ui.internal.mapping.element.toIfElement
import com.adapty.ui.internal.mapping.element.toImageElement
import com.adapty.ui.internal.mapping.element.toLegacyButtonElement
import com.adapty.ui.internal.mapping.element.toLegacyColumnElement
import com.adapty.ui.internal.mapping.element.toLegacyRowElement
import com.adapty.ui.internal.mapping.element.toPagerElement
import com.adapty.ui.internal.mapping.element.toRowElement
import com.adapty.ui.internal.mapping.element.toScreenHolderElement
import com.adapty.ui.internal.mapping.element.toSliderElement
import com.adapty.ui.internal.mapping.element.toHorizontalProgressElement
import com.adapty.ui.internal.mapping.element.toVerticalProgressElement
import com.adapty.ui.internal.mapping.element.toRadialProgressElement
import com.adapty.ui.internal.mapping.element.toTextProgressElement
import com.adapty.ui.internal.mapping.element.toWheelDateTimePickerElement
import com.adapty.ui.internal.mapping.element.toWheelItemsPickerElement
import com.adapty.ui.internal.mapping.element.toWheelPickerElement
import com.adapty.ui.internal.mapping.element.toWheelRangePickerElement
import com.adapty.ui.internal.mapping.element.toSectionElement
import com.adapty.ui.internal.mapping.element.toSpaceElement
import com.adapty.ui.internal.mapping.element.toTextElement
import com.adapty.ui.internal.mapping.element.toTimerElement
import com.adapty.ui.internal.mapping.element.toTextFieldElement
import com.adapty.ui.internal.mapping.element.toToggleElement
import com.adapty.ui.internal.mapping.element.toVStackElement
import com.adapty.ui.internal.mapping.element.toZStackElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.FORMAT_VERSION_5_0_0
import com.adapty.ui.internal.utils.FlowMode
import com.adapty.ui.internal.utils.isSameOrNewerVersionThan

internal typealias JsonObject = Map<String, Any?>
internal typealias JsonArray = Iterable<JsonObject>
internal typealias Templates = Map<String, JsonObject>

internal class ViewConfigurationMapper(
    private val uiElementFactory: UIElementFactory,
) {

    internal companion object {
        const val DATA = "data"
        const val PAYWALL_BUILDER_ID = "paywall_builder_id"
        const val PAYWALL_BUILDER_CONFIG = "paywall_builder_config"
        const val IS_HARD_PAYWALL = "is_hard_paywall"
        const val LANG = "lang"
        const val DEFAULT_LOCALIZATION = "default_localization"
        const val ASSETS = "assets"
        const val LOCALIZATIONS = "localizations"
        const val ID = "id"
        const val TYPE = "type"
        const val URL = "url"
        const val ANDROID_LOCALE_ID = "android_locale_id"

        fun createDefault(videoMapperFn: ((Map<*, *>, Assets) -> UIElement)? = null): ViewConfigurationMapper {
            hasVideoSupport = videoMapperFn != null
            val mappers = buildMap<String, ElementMapper> {
                put("image", leaf(Map<*, *>::toImageElement))
                put("text", leaf(Map<*, *>::toTextElement))
                put("space", leaf(Map<*, *>::toSpaceElement))
                put("timer", leaf(Map<*, *>::toTimerElement))
                put("toggle", leaf(Map<*, *>::toToggleElement))
                put("text_field", leaf(Map<*, *>::toTextFieldElement))
                put("text_editor", leaf(Map<*, *>::toTextFieldElement))
                put("slider", leaf(Map<*, *>::toSliderElement))
                put("horizontal_progress", leaf(Map<*, *>::toHorizontalProgressElement))
                put("vertical_progress", leaf(Map<*, *>::toVerticalProgressElement))
                put("radial_progress", leaf(Map<*, *>::toRadialProgressElement))
                put("text_progress", leaf(Map<*, *>::toTextProgressElement))
                put("wheel_picker", leaf(Map<*, *>::toWheelPickerElement))
                put("wheel_items_picker", leaf(Map<*, *>::toWheelItemsPickerElement))
                put("wheel_range_picker", leaf(Map<*, *>::toWheelRangePickerElement))
                put("compact_datetime_picker", leaf(Map<*, *>::toCompactDateTimePickerElement))
                put("wheel_datetime_picker", leaf(Map<*, *>::toWheelDateTimePickerElement))
                put("graphical_datetime_picker", leaf(Map<*, *>::toGraphicalDateTimePickerElement))
                put("screen_holder", leaf(Map<*, *>::toScreenHolderElement))

                put("box", container(Map<*, *>::toBoxElement))
                put("button", container(Map<*, *>::toButtonElement))
                put("legacy_button", container(Map<*, *>::toLegacyButtonElement))
                put("column", container(Map<*, *>::toColumnElement))
                put("legacy_column", container(Map<*, *>::toLegacyColumnElement))
                put("v_stack", container(Map<*, *>::toVStackElement))
                put("h_stack", container(Map<*, *>::toHStackElement))
                put("z_stack", container(Map<*, *>::toZStackElement))
                put("row", container(Map<*, *>::toRowElement))
                put("legacy_row", container(Map<*, *>::toLegacyRowElement))
                put("pager", container(Map<*, *>::toPagerElement))
                put("section", container(Map<*, *>::toSectionElement))
                put("if", container(Map<*, *>::toIfElement))

                videoMapperFn?.let { fn -> put("video", leaf(fn)) }
            }
            return ViewConfigurationMapper(UIElementFactory(mappers))
        }
    }

    fun map(data: JsonObject, flow: AdaptyFlow): FlowConfiguration {
        return map(data, FlowMode.Live(flow))
    }

    fun map(data: JsonObject, mode: FlowMode): FlowConfiguration {
        val data = data.getAs<JsonObject>(DATA) ?: data
        val id = data.getAs<String>(PAYWALL_BUILDER_ID) ?: throw adaptyError(
            message = "id in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val config = data.getAs<JsonObject>(PAYWALL_BUILDER_CONFIG) ?: throw adaptyError(
            message = "config in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        val localesOrderedDesc = setOfNotNull<String>(
            config.getAs(DEFAULT_LOCALIZATION),
            data.getAs(LANG),
        )

        val screenStateMap = mutableMapOf<String, Any>()

        val assets = mapAssets(config, localesOrderedDesc)

        val normalized = config.normalizeViewConfig()

        val localizations = config.getAs<JsonArray>(LOCALIZATIONS)
        val resolvedLocaleId = localesOrderedDesc.lastOrNull()
        val defaultLocaleId = config.getAs<String>(DEFAULT_LOCALIZATION)
        val resolvedLocalization = localizations?.firstOrNull { it.getAs<String>(ID) == resolvedLocaleId }
        val defaultLocalization = localizations?.firstOrNull { it.getAs<String>(ID) == defaultLocaleId }
        val androidLocaleId = resolvedLocalization?.getAs<String>(ANDROID_LOCALE_ID)
            ?: defaultLocalization?.getAs<String>(ANDROID_LOCALE_ID)
        val converterLocale = androidLocaleId
            ?.let { runCatching { java.util.Locale.forLanguageTag(it) }.getOrNull() }
            ?.takeUnless { it.language.isNullOrEmpty() }
            ?: java.util.Locale.getDefault()

        val behavior = config.getAs<JsonObject>("behavior")
        val showPurchaseLoader = behavior?.getAs<Boolean>("show_purchase_loader") ?: true
        val showRestoreLoader = behavior?.getAs<Boolean>("show_restore_loader") ?: true

        val format = config.getAs<String>(FORMAT) ?: FORMAT_VERSION_5_0_0
        val isLegacyFormat = !format.isSameOrNewerVersionThan(FORMAT_VERSION_5_0_0)

        return FlowConfiguration(
            id = id,
            mode = mode,
            isHard = config.getAs(IS_HARD_PAYWALL) ?: false,
            isRtl = (resolvedLocalization ?: defaultLocalization)
                ?.getAs<Boolean>("is_right_to_left") ?: false,
            locale = converterLocale,
            localizationId = resolvedLocaleId,
            assets = assets,
            texts = mapTexts(config, localesOrderedDesc),
            screens = mapScreens(normalized.screensConfig, assets, screenStateMap, uiElementFactory, normalized.templates),
            navigators = mapNavigators(normalized.navigatorsConfig, assets, screenStateMap, uiElementFactory, normalized.templates),
            initialScript = normalized.initialScript,
            showPurchaseLoader = showPurchaseLoader,
            showRestoreLoader = showRestoreLoader,
            isLegacyFormat = isLegacyFormat,
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

internal enum class LayoutBehaviour {
    DEFAULT, HERO, FLAT, TRANSPARENT;

    companion object {
        fun from(value: String?) = when (value) {
            "default" -> DEFAULT
            "hero" -> HERO
            "flat" -> FLAT
            "transparent" -> TRANSPARENT
            else -> throw adaptyError(
                message = "Unsupported layout_behaviour: $value",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
    }
}
