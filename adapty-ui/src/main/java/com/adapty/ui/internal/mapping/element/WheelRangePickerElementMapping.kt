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

internal fun Map<*, *>.toWheelRangePickerElement(assets: Assets): UIElement {
    val binding = this["value"]?.toTwoWayBinding() ?: throw adaptyError(
        message = "value in WheelRangePicker must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )

    val min = (this["min"] as? Number)?.toDouble() ?: throw adaptyError(
        message = "min in WheelRangePicker must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )
    val max = (this["max"] as? Number)?.toDouble() ?: throw adaptyError(
        message = "max in WheelRangePicker must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )
    val step = (this["step"] as? Number)?.toDouble()?.takeIf { it > 0.0 } ?: 1.0

    val formatItems = this["format"]?.toStringId()?.let { stringId ->
        listOf(WheelPickerDataSource.RangeWithFormat.FormatItem(Double.NEGATIVE_INFINITY, stringId))
    }
        ?: (this["format"] as? Iterable<*>)?.mapNotNull { item ->
            if (item !is Map<*, *>) return@mapNotNull null
            val stringId = item["string_id"]?.toStringId() ?: throw adaptyError(
                message = "Missing key \"string_id\"",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            val from = (item["from"] as? Number)?.toDouble() ?: throw adaptyError(
                message = "from in WheelRangePicker format item must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
            WheelPickerDataSource.RangeWithFormat.FormatItem(from, stringId)
        }?.sortedByDescending { it.from }?.takeIf { it.isNotEmpty() }
        ?: throw adaptyError(
            message = "format in WheelRangePicker must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

    val itemHeight = (this["item_height"] as? Number)?.toFloat() ?: 40f
    val visibleItems = ((this["visible_items"] as? Number)?.toInt() ?: 5).let { v ->
        if (v % 2 == 0) v + 1 else v
    }

    val indicator = this["indicator"] as? Map<*, *>

    return WheelPickerElement(
        value = binding,
        dataSource = WheelPickerDataSource.RangeWithFormat(min, max, step, formatItems),
        itemHeightDp = itemHeight,
        visibleItems = visibleItems,
        textAttributes = this.toTextAttributes(),
        selectedColor = this["selected_color"]?.toVisualValue(),
        indicatorColor = indicator?.get("color")?.toVisualValue(),
        indicatorCornerRadius = indicator?.get("rect_corner_radius")?.toCornerRadius(),
        stringId = null,
        baseProps = this.extractBaseProps(),
    )
}
