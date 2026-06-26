@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.element.LegacyRowElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toLegacyRowElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = this.extractBaseProps()
    return LegacyRowElement(
        content = (this["items"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.toGridItem(DimSpec.Axis.X) { childConfig -> childMapper(childConfig, inheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        spacing = this.extractSpacingOrNull(),
        baseProps = baseProps,
    )
}
