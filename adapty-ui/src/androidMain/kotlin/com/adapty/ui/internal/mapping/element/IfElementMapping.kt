@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.ui.internal.platform.AdaptyErrorCode
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.adaptyError
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toIfElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val key = if (!matchesStaticConditionFields(DeviceKind.current())) {
        "else"
    } else {
        listOf("then", "else").firstOrNull { key ->
            hasVideoSupport || (this[key] as? Map<*, *>)?.get("type") != "video"
        } ?: "then"
    }
    return (this[key] as? Map<*, *>)?.let { item -> childMapper(item, inheritShrink) }
        ?: throw adaptyError(
            message = "$key in If must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
}

internal var hasVideoSupport: Boolean = false
