@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.WheelColumnDefaults
import com.adapty.ui.internal.ui.element.WheelPickerDataSource
import com.adapty.ui.internal.ui.element.WheelPickerElement

internal fun Map<*, *>.toWheelItemsPickerElement(assets: Assets): UIElement {
    val binding = this["value"]?.toTwoWayBinding() ?: throw adaptyError(
        message = "value in WheelItemsPicker must not be null",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )

    val items = (this["items"] as? Iterable<*>)?.mapNotNull { entry ->
        val item = entry as? Map<*, *> ?: return@mapNotNull null
        val value = item["value"]?.takeIf { it is String || it is Number || it is Boolean }
            ?: throw adaptyError(
                message = "Missing or non-primitive key \"value\"",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        val stringId = item["string_id"]?.toStringId() ?: throw adaptyError(
            message = "Missing key \"string_id\"",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
        WheelPickerDataSource.Items.Item(
            value = value,
            stringId = stringId,
        )
    }?.takeIf { it.isNotEmpty() } ?: throw adaptyError(
        message = "items in WheelItemsPicker must not be empty",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
    )

    return WheelPickerElement(
        value = binding,
        dataSource = WheelPickerDataSource.Items(items),
        itemHeightDp = WheelColumnDefaults.ItemHeightDp,
        visibleItems = WheelColumnDefaults.VisibleItems,
        textAttributes = null,
        selectedColor = null,
        indicatorColor = null,
        indicatorCornerRadius = null,
        stringId = null,
        baseProps = this.extractBaseProps(),
    )
}
