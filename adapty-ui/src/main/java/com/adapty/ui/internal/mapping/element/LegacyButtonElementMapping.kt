@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.ui.element.LegacyButtonElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toLegacyButtonElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    return LegacyButtonElement(
        (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
            ?: (this["action"] as? Map<*, *>)?.toAction()?.let { action -> listOf(action) }.orEmpty(),
        (this["normal"] as? Map<*, *>)?.let { content -> childMapper(content, inheritShrink) }
            ?: throw adaptyError(
                message = "normal in legacy Button must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
        (this["selected"] as? Map<*, *>)?.let { content -> childMapper(content, inheritShrink) },
        this["is_selected"]?.toOneWayBinding(),
        this.extractBaseProps(),
    )
}
