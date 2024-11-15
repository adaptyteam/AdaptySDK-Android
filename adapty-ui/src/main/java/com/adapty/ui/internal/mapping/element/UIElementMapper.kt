@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.ui.element.Container
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.UnknownElement
import com.adapty.ui.internal.utils.NO_SHRINK

public typealias Assets = Map<String, AdaptyUI.LocalizedViewConfiguration.Asset>
internal typealias Texts = Map<String, AdaptyUI.LocalizedViewConfiguration.TextItem>
internal typealias Products = Map<String, AdaptyPaywallProduct>
internal typealias StateMap = MutableMap<String, Any>
internal typealias ChildMapper = (item: Map<*, *>) -> UIElement?
internal typealias ChildMapperShrinkable = (item: Map<*, *>, nextInheritShrink: Int) -> UIElement?

@InternalAdaptyApi
public interface UIElementMapper {
    public fun canMap(config: Map<*, *>): Boolean
}

@InternalAdaptyApi
public interface UIPlainElementMapper: UIElementMapper {
    public fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles): UIElement
}

internal interface UIComplexElementMapper: UIElementMapper {
    fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles, stateMap: MutableMap<String, Any>, inheritShrink: Int, childMapper: ChildMapper): UIElement
}

internal interface UIComplexShrinkableElementMapper: UIElementMapper {
    fun map(config: Map<*, *>, assets: Assets, refBundles: ReferenceBundles, stateMap: MutableMap<String, Any>, inheritShrink: Int, childMapper: ChildMapperShrinkable): UIElement
}

internal class UIElementFactory(private val mappers: List<UIElementMapper>) {
    fun createElementTree(
        config: Map<*, *>,
        assets: Assets,
        stateMap: StateMap,
        refBundles: ReferenceBundles,
    ): UIElement {
        return createElement(config, assets, stateMap, refBundles, NO_SHRINK)
    }

    private fun createElement(
        config: Map<*, *>,
        assets: Assets,
        stateMap: StateMap,
        refBundles: ReferenceBundles,
        inheritShrink: Int,
    ): UIElement {
        val mapper = mappers.find { it.canMap(config) }
        return when (mapper) {
            is UIPlainElementMapper -> {
                mapper.map(config, assets, refBundles)
            }
            is UIComplexElementMapper -> {
                mapper.map(config, assets, refBundles, stateMap, inheritShrink) { childConfig ->
                    createElement(childConfig, assets, stateMap, refBundles, inheritShrink)
                }
            }
            is UIComplexShrinkableElementMapper -> {
                mapper.map(config, assets, refBundles, stateMap, inheritShrink) { childConfig, nextInheritShrink ->
                    createElement(childConfig, assets, stateMap, refBundles, nextInheritShrink)
                }
            }
            else -> UnknownElement
        }
    }
}

@InternalAdaptyApi
public class ReferenceBundles internal constructor(
    internal val targetElements: MutableMap<String, UIElement>,
    internal val awaitingElements: MutableMap<String, MutableList<Container<*>>>,
) {
    internal companion object {
        fun create() = ReferenceBundles(mutableMapOf(), mutableMapOf())
    }
}