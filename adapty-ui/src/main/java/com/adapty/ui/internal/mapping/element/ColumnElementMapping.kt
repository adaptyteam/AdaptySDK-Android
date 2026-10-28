@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toHorizontalAlign
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.element.ColumnElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.VStackElement

internal fun Map<*, *>.toColumnElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = this.extractBaseProps()
    val content = (this["items"] as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.toGridItem(DimSpec.Axis.Y) { childConfig -> childMapper(childConfig, inheritShrink) }
    }.let { content ->
        if (shouldSkipContainer(content, baseProps))
            return SkippedElement
        content.orEmpty()
    }
    (this["draw_as_stack"] as? Map<*, *>)?.let { stackParams ->
        return VStackElement(
            content = content.map { it.content },
            align = stackParams["h_align"].toHorizontalAlign(),
            spacing = this.extractSpacingOrNull(),
            baseProps = baseProps,
        )
    }
    return ColumnElement(
        content = content,
        spacing = this.extractSpacingOrNull(),
        height = MainAxisBehaviour.fromValueOrNull(this["height"] as? String),
        baseProps = baseProps,
    )
}
