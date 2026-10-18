@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toHorizontalAlign
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.VStackElement

internal fun Map<*, *>.toVStackElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val (baseProps, nextInheritShrink) = this.extractBasePropsWithShrinkInheritance(inheritShrink)
    return VStackElement(
        content = (this["content"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        align = this["h_align"].toHorizontalAlign(),
        spacing = this.extractSpacingOrNull(),
        baseProps = baseProps,
    )
}
