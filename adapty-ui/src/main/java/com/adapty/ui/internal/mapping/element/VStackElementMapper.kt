@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.VStackElement

internal class VStackElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("v_stack", commonAttributeMapper),
    UIComplexShrinkableElementMapper {

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
        return VStackElement(
            content = (config["content"] as? List<*>)?.mapNotNull { item ->
                processContentItem(
                    (item as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) },
                    referenceIds,
                    refBundles.targetElements,
                )
            }.let { content ->
                if (shouldSkipContainer(content, baseProps))
                    return SkippedElement
                content.orEmpty()
            },
            align = commonAttributeMapper.mapHorizontalAlign(config["h_align"]),
            spacing = config.extractSpacingOrNull(),
            baseProps = baseProps,
        )
            .also { container ->
                addToAwaitingReferencesIfNeeded(referenceIds, container, refBundles.awaitingElements)
                addToReferenceTargetsIfNeeded(config, container, refBundles)
            }
    }
}