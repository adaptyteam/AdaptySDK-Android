@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toAlign
import com.adapty.ui.internal.ui.element.BoxElement
import com.adapty.ui.internal.ui.element.BoxWithoutContentElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toBoxElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val (baseProps, nextInheritShrink) = this.extractBasePropsWithShrinkInheritance(inheritShrink)
    val content = (this["content"] as? Map<*, *>)?.let { content -> childMapper(content, nextInheritShrink) }
    val align = this.toAlign()
    if (content == null)
        return BoxWithoutContentElement(align, baseProps)
    return BoxElement(content, align, baseProps)
}
