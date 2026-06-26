@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.mapping.viewconfig.JsonObject
import com.adapty.ui.internal.mapping.viewconfig.Templates
import com.adapty.ui.internal.ui.element.OverlayContainerElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.UnknownElement
import com.adapty.ui.internal.utils.NO_SHRINK

public typealias Assets = Map<String, AdaptyUI.FlowConfiguration.Asset>
internal typealias Texts = Map<String, AdaptyUI.FlowConfiguration.TextItem>
internal typealias Products = Map<String, AdaptyPaywallProduct>
internal typealias StateMap = MutableMap<String, Any>
internal typealias ChildMapper = (item: Map<*, *>) -> UIElement?
internal typealias ChildMapperShrinkable = (item: Map<*, *>, nextInheritShrink: Int) -> UIElement?
internal typealias ChildSlots = Map<String, JsonObject>

private const val TEMPLATE_REF_PREFIX = "#"
private const val CHILD_TYPE = "child"
private const val TYPE_KEY = "type"
private const val NAME_KEY = "name"

internal sealed interface ElementMapper {
    data class Leaf(val fn: (Map<*, *>, Assets) -> UIElement) : ElementMapper
    data class Container(val fn: (Map<*, *>, Assets, StateMap, Int, ChildMapperShrinkable) -> UIElement) : ElementMapper
}

internal fun leaf(fn: (Map<*, *>, Assets) -> UIElement) = ElementMapper.Leaf(fn)
internal fun container(fn: (Map<*, *>, Assets, StateMap, Int, ChildMapperShrinkable) -> UIElement) = ElementMapper.Container(fn)

internal class UIElementFactory(private val mappers: Map<String, ElementMapper>) {
    fun createElementTree(
        config: Map<*, *>,
        assets: Assets,
        stateMap: StateMap,
        templates: Templates,
    ): UIElement {
        return createElement(config, assets, stateMap, templates, NO_SHRINK, emptyMap())
    }

    private fun createElement(
        config: Map<*, *>,
        assets: Assets,
        stateMap: StateMap,
        templates: Templates,
        inheritShrink: Int,
        childSlots: ChildSlots,
    ): UIElement {
        val type = config[TYPE_KEY] as? String

        if (type == CHILD_TYPE) {
            val slotName = config[NAME_KEY] as? String
                ?: throw adaptyError(
                    message = "'child' element must have a 'name' property",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )

            val slotContent = childSlots[slotName]
                ?: throw adaptyError(
                    message = "Missing required child slot: '$slotName'",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )

            return createElement(slotContent, assets, stateMap, templates, inheritShrink, childSlots)
        }

        if (type != null && type.startsWith(TEMPLATE_REF_PREFIX)) {
            val templateKey = type.removePrefix(TEMPLATE_REF_PREFIX)
            val templateContent = templates[templateKey]
            if (templateContent != null) {
                val newChildSlots = extractChildSlots(config)
                val mergedSlots = childSlots + newChildSlots
                return createElement(templateContent, assets, stateMap, templates, inheritShrink, mergedSlots)
            }
        }

        val element = when (val mapper = type?.let { mappers[it] }) {
            is ElementMapper.Leaf -> mapper.fn(config, assets)
            is ElementMapper.Container -> mapper.fn(config, assets, stateMap, inheritShrink) { childConfig, nextShrink ->
                createElement(childConfig, assets, stateMap, templates, nextShrink, childSlots)
            }
            null -> UnknownElement
        }

        val overlayConfigs = config["overlay"] as? List<*>
        val overlays = overlayConfigs?.mapNotNull { item ->
            (item as? Map<*, *>)?.toOverlayItem { childConfig, nextShrink ->
                createElement(childConfig, assets, stateMap, templates, nextShrink, childSlots)
            }
        }

        val backgroundConfigs = config["background"] as? List<*>
        val backgrounds = backgroundConfigs?.mapNotNull { item ->
            (item as? Map<*, *>)?.toOverlayItem { childConfig, nextShrink ->
                createElement(childConfig, assets, stateMap, templates, nextShrink, childSlots)
            }
        }

        return if (!overlays.isNullOrEmpty() || !backgrounds.isNullOrEmpty()) {
            OverlayContainerElement(
                main = element,
                overlays = overlays ?: emptyList(),
                backgrounds = backgrounds ?: emptyList(),
                baseProps = element.baseProps.copy(shape = null),
            )
        } else {
            element
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractChildSlots(config: Map<*, *>): ChildSlots {
        return config
            .filterKeys { it != TYPE_KEY && it is String }
            .mapNotNull { (key, value) ->
                if (value is Map<*, *>) {
                    (key as String) to (value as JsonObject)
                } else null
            }
            .toMap()
    }
}
