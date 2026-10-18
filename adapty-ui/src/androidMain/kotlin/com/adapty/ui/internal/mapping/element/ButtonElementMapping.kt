@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.ui.element.ButtonElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toButtonElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    return ButtonElement(
        (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
            ?: (this["action"] as? Map<*, *>)?.toAction()?.let { action -> listOf(action) }.orEmpty(),
        (this["content"] as? Map<*, *>)?.let { content -> childMapper(content, inheritShrink) }
            ?: throw adaptyError(
                message = "content in Button must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
        this.extractBaseProps(),
    )
}
