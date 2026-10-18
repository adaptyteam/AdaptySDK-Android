@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.ui.internal.mapping.attributes.toAlign
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.ZStackElement

internal fun Map<*, *>.toZStackElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val (baseProps, nextInheritShrink) = this.extractBasePropsWithShrinkInheritance(inheritShrink)
    return ZStackElement(
        content = (this["content"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        align = this.toAlign(),
        baseProps = baseProps,
    )
}
