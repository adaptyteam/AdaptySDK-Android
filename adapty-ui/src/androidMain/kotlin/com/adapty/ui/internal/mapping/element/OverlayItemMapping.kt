@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.mapping.attributes.toAlign
import com.adapty.ui.internal.ui.element.OverlayItem
import com.adapty.ui.internal.utils.NO_SHRINK

internal fun Map<*, *>.toOverlayItem(
    childMapper: ChildMapperShrinkable,
): OverlayItem? {
    val contentConfig = this["content"] as? Map<*, *> ?: return null
    val content = childMapper(contentConfig, NO_SHRINK) ?: return null
    return OverlayItem(
        align = this.toAlign(),
        content = content,
    )
}
