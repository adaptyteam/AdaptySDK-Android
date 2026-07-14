@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toEdgeEntities
import com.adapty.ui.internal.mapping.attributes.toInteractionBehavior
import com.adapty.ui.internal.mapping.attributes.toPageSize
import com.adapty.ui.internal.mapping.attributes.toPagerAnimation
import com.adapty.ui.internal.mapping.attributes.toPagerIndicator
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.ui.element.PagerElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toPagerElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = this.extractBaseProps()
    return PagerElement(
        this["page_width"].toPageSize(),
        this["page_height"].toPageSize(),
        this["page_padding"]?.toEdgeEntities(),
        this.extractSpacingOrNull(),
        (this["content"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let { content -> childMapper(content, inheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        (this["page_control"] as? Map<*, *>)?.toPagerIndicator(),
        (this["animation"] as? Map<*, *>)?.toPagerAnimation(),
        this["interaction"].toInteractionBehavior(),
        this["page_index"]?.toTwoWayBinding(),
        baseProps,
    )
}
