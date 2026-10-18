@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toVerticalAlign
import com.adapty.ui.internal.ui.element.HStackElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toHStackElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val (baseProps, nextInheritShrink) = this.extractBasePropsWithShrinkInheritance(inheritShrink)
    return HStackElement(
        content = (this["content"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        align = this["v_align"].toVerticalAlign(),
        spacing = this.extractSpacingOrNull(),
        baseProps = baseProps,
    )
}
