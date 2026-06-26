@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toCornerRadius
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.WheelPickerDataSource
import com.adapty.ui.internal.ui.element.WheelPickerElement

internal fun Map<*, *>.toWheelPickerElement(assets: Assets): UIElement {
    val binding = this["value"]?.toTwoWayBinding() ?: throw adaptyError(
        message = "value in WheelPicker must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )

    val itemsList = (this["items"] as? List<*>)?.mapNotNull { it as? String }
    val min = (this["min"] as? Number)?.toDouble()
    val max = (this["max"] as? Number)?.toDouble()
    val step = (this["step"] as? Number)?.toDouble() ?: 1.0

    val dataSource = when {
        itemsList != null -> WheelPickerDataSource.StringList(itemsList)
        min != null && max != null -> WheelPickerDataSource.Range(min, max, step)
        else -> throw adaptyError(
            message = "WheelPicker requires either 'items' or 'min'/'max'",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    }

    val itemHeight = (this["item_height"] as? Number)?.toFloat() ?: 40f
    val visibleItems = ((this["visible_items"] as? Number)?.toInt() ?: 5).let { v ->
        if (v % 2 == 0) v + 1 else v
    }

    val indicator = this["indicator"] as? Map<*, *>

    return WheelPickerElement(
        value = binding,
        dataSource = dataSource,
        itemHeightDp = itemHeight,
        visibleItems = visibleItems,
        textAttributes = this.toTextAttributes(),
        selectedColor = this["selected_color"]?.toVisualValue(),
        indicatorColor = indicator?.get("color")?.toVisualValue(),
        indicatorCornerRadius = indicator?.get("rect_corner_radius")?.toCornerRadius(),
        stringId = this["string_id"]?.toStringId(),
        baseProps = this.extractBaseProps(),
    )
}
