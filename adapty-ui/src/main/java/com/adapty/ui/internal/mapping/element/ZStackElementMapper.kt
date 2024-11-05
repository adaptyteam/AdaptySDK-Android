@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.ZStackElement

internal class ZStackElementMapper(
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("z_stack", commonAttributeMapper),
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
        return ZStackElement(
                content = (config["content"] as? List<*>)?.mapNotNull { item ->
                    processContentItem(
                        (item as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) },
                        referenceIds,
                        refBundles.targetElements,
                    )
                }?.takeIf { it.isNotEmpty() }
                    ?: return SkippedElement,
                align = commonAttributeMapper.mapAlign(config),
                baseProps = baseProps,
            )
                .also { container ->
                    addToAwaitingReferencesIfNeeded(referenceIds, container, refBundles.awaitingElements)
                    addToReferenceTargetsIfNeeded(config, container, refBundles)
                }
    }
}