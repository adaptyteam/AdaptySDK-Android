@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.BoxElement
import com.adapty.ui.internal.ui.element.BoxWithoutContentElement
import com.adapty.ui.internal.ui.element.UIElement

internal class BoxElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("box", commonAttributeMapper), UIComplexShrinkableElementMapper {
    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapperShrinkable,
    ): UIElement {
        val referenceIds = mutableSetOf<String>()
        val (baseProps, nextInheritShrink) = extractBasePropsWithShrinkInheritance(config, inheritShrink)
        val content = processContentItem(
            (config["content"] as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) },
            referenceIds,
            refBundles.targetElements
        )
        val align = commonAttributeMapper.mapAlign(config)
        if (content == null)
            return BoxWithoutContentElement(align, baseProps)
        return BoxElement(content, align, baseProps)
            .also { container ->
                addToAwaitingReferencesIfNeeded(referenceIds, container, refBundles.awaitingElements)
                addToReferenceTargetsIfNeeded(config, container, refBundles)
            }
    }
}