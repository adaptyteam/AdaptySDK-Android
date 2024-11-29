@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.CommonAttributeMapper
import com.adapty.ui.internal.mapping.attributes.PagerAttributeMapper
import com.adapty.ui.internal.ui.element.PagerElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal class PagerElementMapper(
    private val pagerAttributeMapper: PagerAttributeMapper,
    commonAttributeMapper: CommonAttributeMapper,
) : BaseUIComplexElementMapper("pager", commonAttributeMapper), UIComplexElementMapper {

    override fun map(
        config: Map<*, *>,
        assets: Assets,
        refBundles: ReferenceBundles,
        stateMap: MutableMap<String, Any>,
        inheritShrink: Int,
        childMapper: ChildMapper,
    ): UIElement {
        val referenceIds = mutableSetOf<String>()
        val baseProps = config.extractBaseProps()
        return PagerElement(
            pagerAttributeMapper.mapPageSize(config["page_width"]),
            pagerAttributeMapper.mapPageSize(config["page_height"]),
            config["page_padding"]?.let(commonAttributeMapper::mapEdgeEntities),
            config.extractSpacingOrNull(),
            (config["content"] as? List<*>)?.mapNotNull { item ->
                processContentItem(
                    (item as? Map<*, *>)?.let { content -> childMapper(content) },
                    referenceIds,
                    refBundles.targetElements,
                )
            }.let { content ->
                if (shouldSkipContainer(content, baseProps))
                    return SkippedElement
                content.orEmpty()
            },
            (config["page_control"] as? Map<*, *>)?.let(pagerAttributeMapper::mapPagerIndicator),
            (config["animation"] as? Map<*, *>)?.let(pagerAttributeMapper::mapPagerAnimation),
            pagerAttributeMapper.mapInteractionBehavior(config["interaction"]),
            baseProps,
        )
            .also { container ->
                addToAwaitingReferencesIfNeeded(referenceIds, container, refBundles.awaitingElements)
                addToReferenceTargetsIfNeeded(config, container, refBundles)
            }
    }
}