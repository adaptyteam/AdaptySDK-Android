@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toHorizontalAlign
import com.adapty.ui.internal.mapping.attributes.toTransition
import com.adapty.ui.internal.mapping.attributes.toVerticalAlign
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.FlexElement
import com.adapty.ui.internal.ui.element.FlexStackElement
import com.adapty.ui.internal.ui.element.HStackElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.element.VStackElement

internal fun Map<*, *>.toFlexStackElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val deviceKind = DeviceKind.current()
    val runtimeConditions = (this["condition"] as? List<*>).parseConditionGroup(deviceKind)
    val direction = when (this["direction"]) {
        "h_stack", "horizontal" ->
            if (runtimeConditions == null) FlexElement.Direction.VERTICAL else FlexElement.Direction.HORIZONTAL
        "v_stack", "vertical" ->
            if (runtimeConditions == null) FlexElement.Direction.HORIZONTAL else FlexElement.Direction.VERTICAL
        else -> throw adaptyError(
            message = "Invalid flex_stack direction: ${this["direction"]}",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )
    }
    val condition = runtimeConditions ?: emptyList()

    val content = (this["content"] as? List<*>).orEmpty().mapNotNull { item ->
        (item as? Map<*, *>)?.let { childMapper(it, inheritShrink) }
    }

    val hSpacing = (this["h_spacing"] as? Number)?.toFloat()?.takeIf { it != 0f }
    val vSpacing = (this["v_spacing"] as? Number)?.toFloat()?.takeIf { it != 0f }
    val transition = if (condition.isNotEmpty() && this["duration"] != null) this.toTransition() else null

    return FlexStackElement(
        condition = condition,
        direction = direction,
        hStackElement = HStackElement(content, this["v_align"].toVerticalAlign(), hSpacing, BaseProps()),
        vStackElement = VStackElement(content, this["h_align"].toHorizontalAlign(), vSpacing, BaseProps()),
        transition = transition,
        baseProps = this.extractBaseProps(),
    )
}
