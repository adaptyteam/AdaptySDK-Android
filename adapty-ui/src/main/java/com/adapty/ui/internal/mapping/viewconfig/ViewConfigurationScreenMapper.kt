@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.viewconfig

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Screen
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.ScreenBundle
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.ReferenceBundles
import com.adapty.ui.internal.mapping.element.StateMap
import com.adapty.ui.internal.mapping.element.UIElementFactory
import com.adapty.ui.internal.ui.attributes.plus
import com.adapty.ui.internal.ui.element.BoxElement
import com.adapty.ui.internal.utils.ContentWrapper
import com.adapty.ui.internal.utils.getAs

internal class ViewConfigurationScreenMapper(
    private val uiElementFactory: UIElementFactory,
    private val commonAttributeMapper: CommonAttributeMapper,
) {

    private companion object {
        const val DEFAULT = "default"
        const val BACKGROUND = "background"
        const val CONTENT = "content"
        const val COVER = "cover"
        const val FOOTER = "footer"
        const val OVERLAY = "overlay"
        const val V_ALIGN = "v_align"
        const val H_ALIGN = "h_align"
        const val DEFAULT_CONTENT_V_ALIGN = "top"
        const val DECORATOR = "decorator"
        const val OFFSET = "offset"
    }

    fun map(
        screens: JsonObject,
        template: Template,
        assets: Assets,
        stateMap: StateMap,
    ): ScreenBundle {
        val refBundles = ReferenceBundles.create()

        val defaultScreen = screens.getAs<JsonObject>(DEFAULT)?.let { default ->
            mapDefaultScreen(default, template, assets, stateMap, refBundles)
        } ?: throw adaptyError(
            message = "default in styles in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

        val bottomSheets = mutableMapOf<String, Screen.BottomSheet>()
        screens
            .forEach { (k, v) ->
                if (k == DEFAULT || v !is Map<*, *>) return@forEach
                val bottomSheet = mapBottomSheet(v, assets, stateMap, refBundles) ?: return@forEach
                bottomSheets[k] = bottomSheet
            }

        return ScreenBundle(defaultScreen, bottomSheets, stateMap)
    }

    private fun mapDefaultScreen(
        rawScreen: JsonObject,
        template: Template,
        assets: Assets,
        stateMap: StateMap,
        refBundles: ReferenceBundles,
    ): Screen.Default {
        val background = rawScreen.getAs<String>(BACKGROUND) ?: throw adaptyError(
            message = "background in 'default' screen in ViewConfiguration should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        val cover = rawScreen.getAs<JsonObject>(COVER)?.toElementTree(assets, stateMap, refBundles) as? BoxElement
        val footer = rawScreen.getAs<JsonObject>(FOOTER)?.toElementTree(assets, stateMap, refBundles)
        val overlay = rawScreen.getAs<JsonObject>(OVERLAY)?.toElementTree(assets, stateMap, refBundles)
        return when (template) {
            Template.BASIC -> {
                if (cover == null)
                    throw adaptyError(
                        message = "cover in 'basic' template in ViewConfiguration should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                val contentWrapper = rawScreen.getAs<JsonObject>(CONTENT)
                    ?.toMutableMap()
                    ?.let { content -> putContentIntoWrapper(content, assets, stateMap, refBundles) }
                    ?: throw adaptyError(
                        message = "content in 'default' screen in ViewConfiguration should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Screen.Default.Basic(background, cover, contentWrapper, footer, overlay)
            }
            Template.TRANSPARENT -> {
                val content = rawScreen.getAs<JsonObject>(CONTENT)
                    ?.toElementTree(assets, stateMap, refBundles)
                    ?: throw adaptyError(
                        message = "content in 'default' screen in ViewConfiguration should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Screen.Default.Transparent(background, cover, content, footer, overlay)
            }
            Template.FLAT -> {
                val contentWrapper = rawScreen.getAs<JsonObject>(CONTENT)
                    ?.toMutableMap()
                    ?.let { content -> putContentIntoWrapper(content, assets, stateMap, refBundles) }
                    ?: throw adaptyError(
                        message = "content in 'default' screen in ViewConfiguration should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                Screen.Default.Flat(background, cover, contentWrapper, footer, overlay)
            }
        }
    }

    private fun putContentIntoWrapper(
        rawContent: MutableMap<String, Any?>,
        assets: Assets,
        stateMap: StateMap,
        refBundles: ReferenceBundles,
    ): ContentWrapper {
        val vAlign = rawContent.remove(V_ALIGN) ?: DEFAULT_CONTENT_V_ALIGN
        val decorator = rawContent.remove(DECORATOR)
        val offset = rawContent.remove(OFFSET)
        return ContentWrapper(
            rawContent.toElementTree(assets, stateMap, refBundles),
            commonAttributeMapper.mapVerticalAlign(vAlign) + commonAttributeMapper.mapHorizontalAlign(rawContent.remove(H_ALIGN)),
            decorator?.let(commonAttributeMapper::mapShape),
            offset?.let(commonAttributeMapper::mapOffset),
        )
    }

    private fun mapBottomSheet(
        rawBottomSheet: Map<*, *>,
        assets: Assets,
        stateMap: StateMap,
        refBundles: ReferenceBundles
    ): Screen.BottomSheet? {
        val content = rawBottomSheet.getAs<JsonObject>(CONTENT)?.toElementTree(assets, stateMap, refBundles) ?: return null
        return Screen.BottomSheet(content)
    }

    private fun Map<*, *>.toElementTree(
        assets: Assets,
        stateMap: StateMap,
        refBundles: ReferenceBundles,
    ) = uiElementFactory.createElementTree(this, assets, stateMap, refBundles)
}