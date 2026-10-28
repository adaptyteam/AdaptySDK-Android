@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toVerticalAlign
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.element.HStackElement
import com.adapty.ui.internal.ui.element.RowElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toRowElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = this.extractBaseProps()
    val content = (this["items"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.toGridItem(DimSpec.Axis.X) { childConfig -> childMapper(childConfig, inheritShrink) }
    }.let { content ->
        if (shouldSkipContainer(content, baseProps))
            return SkippedElement
        content.orEmpty()
    }
    (this["draw_as_stack"] as? Map<*, *>)?.let { stackParams ->
        return HStackElement(
            content = content.map { it.content },
            align = stackParams["v_align"].toVerticalAlign(),
            spacing = this.extractSpacingOrNull(),
            baseProps = baseProps,
        )
    }
    return RowElement(
        content = content,
        spacing = this.extractSpacingOrNull(),
        width = MainAxisBehaviour.fromValueOrNull(this["width"] as? String),
        baseProps = baseProps,
    )
}
