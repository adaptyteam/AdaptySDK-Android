@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.element.LegacyColumnElement
import com.adapty.ui.internal.ui.element.SkippedElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toLegacyColumnElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = this.extractBaseProps()
    return LegacyColumnElement(
        content = (this["items"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.toGridItem(DimSpec.Axis.Y) { childConfig -> childMapper(childConfig, inheritShrink) }
        }.let { content ->
            if (shouldSkipContainer(content, baseProps))
                return SkippedElement
            content.orEmpty()
        },
        spacing = this.extractSpacingOrNull(),
        baseProps = baseProps,
    )
}
