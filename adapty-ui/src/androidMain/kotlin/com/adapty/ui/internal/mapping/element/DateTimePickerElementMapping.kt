@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toDateTime
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.ui.element.DateTimePickerElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toCompactDateTimePickerElement(assets: Assets): UIElement =
    toDateTimePickerElement(DateTimePickerElement.Kind.Compact)

internal fun Map<*, *>.toWheelDateTimePickerElement(assets: Assets): UIElement =
    toDateTimePickerElement(DateTimePickerElement.Kind.Wheel)

internal fun Map<*, *>.toGraphicalDateTimePickerElement(assets: Assets): UIElement =
    toDateTimePickerElement(DateTimePickerElement.Kind.Graphical)

private fun Map<*, *>.toDateTimePickerElement(kind: DateTimePickerElement.Kind): UIElement {
    val binding = this["value"]?.toTwoWayBinding() ?: throw adaptyError(
        message = "value in DateTimePicker must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )

    val components = (this["components"] as? Iterable<*>)
        ?.mapNotNull { it as? String }
        ?.toSet()
        .orEmpty()

    var hasDate = false
    var hasHourMinute = false
    components.forEach {
        when (it) {
            "date" -> hasDate = true
            "hour_and_minute" -> hasHourMinute = true
        }
    }
    if (!hasDate && !hasHourMinute) throw adaptyError(
        message = "components in DateTimePicker must contain at least one of 'date'/'hour_and_minute'",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )

    return DateTimePickerElement(
        kind = kind,
        value = binding,
        components = DateTimePickerElement.Components(date = hasDate, hourAndMinute = hasHourMinute),
        minDate = this["min"]?.toDateTime(),
        maxDate = this["max"]?.toDateTime(),
        color = this["color"]?.toVisualValue(),
        baseProps = this.extractBaseProps(),
    )
}
