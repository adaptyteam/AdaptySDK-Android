@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
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
    return RowElement(
        content = (this["items"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.toGridItem(DimSpec.Axis.X) { childConfig -> childMapper(childConfig, inheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        spacing = this.extractSpacingOrNull(),
        width = MainAxisBehaviour.fromValueOrNull(this["width"] as? String),
        baseProps = baseProps,
    )
}
