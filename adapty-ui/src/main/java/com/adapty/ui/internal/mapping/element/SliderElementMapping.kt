@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.ui.element.SliderElement
import com.adapty.ui.internal.ui.element.UIElement
import kotlin.math.roundToInt

internal fun Map<*, *>.toSliderElement(assets: Assets): UIElement {
    val min = (this["min"] as? Number)?.toFloat() ?: throw adaptyError(
        message = "min in Slider must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )
    val max = (this["max"] as? Number)?.toFloat() ?: throw adaptyError(
        message = "max in Slider must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )
    val step = (this["step"] as? Number)?.toFloat() ?: 1f
    val steps = if (step > 0f) {
        (((max - min) / step).roundToInt() - 1).coerceAtLeast(0)
    } else {
        0
    }

    return SliderElement(
        this["value"]?.toTwoWayBinding() ?: throw adaptyError(
            message = "value in Slider must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        min,
        max,
        steps,
        this.extractBaseProps(),
    )
}
