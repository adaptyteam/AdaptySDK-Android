@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.platform.AdaptyErrorCode
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.adaptyError
import com.adapty.ui.internal.ui.element.ToggleElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toToggleElement(assets: Assets): UIElement {
    return ToggleElement(
        this["value"]?.toTwoWayBinding() ?: throw adaptyError(
            message = "value in Toggle must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        this["color"]?.toVisualValue(),
        this.extractBaseProps(),
    )
}
