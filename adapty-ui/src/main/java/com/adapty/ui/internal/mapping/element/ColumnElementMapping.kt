@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.element.ColumnElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toColumnElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = this.extractBaseProps()
    return ColumnElement(
        content = (this["items"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.toGridItem(DimSpec.Axis.Y) { childConfig -> childMapper(childConfig, inheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        spacing = this.extractSpacingOrNull(),
        height = MainAxisBehaviour.fromValueOrNull(this["height"] as? String),
        baseProps = baseProps,
    )
}
