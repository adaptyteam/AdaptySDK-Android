@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAlign
import com.adapty.ui.internal.mapping.attributes.toDimSpec
import com.adapty.ui.internal.mapping.attributes.toTransition
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.MainAxisBehaviour
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.ColumnElement
import com.adapty.ui.internal.ui.element.FlexElement
import com.adapty.ui.internal.ui.element.GridItem
import com.adapty.ui.internal.ui.element.RowElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toFlexElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val deviceKind = DeviceKind.current()
    val runtimeConditions = (this["condition"] as? List<*>).parseConditionGroup(deviceKind)
    val direction = when (this["direction"]) {
        "row", "horizontal" ->
            if (runtimeConditions == null) FlexElement.Direction.VERTICAL else FlexElement.Direction.HORIZONTAL
        "column", "vertical" ->
            if (runtimeConditions == null) FlexElement.Direction.HORIZONTAL else FlexElement.Direction.VERTICAL
        else -> throw adaptyError(
            message = "Invalid flex direction: ${this["direction"]}",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )
    }
    val condition = runtimeConditions ?: emptyList()

    val items = (this["items"] as? List<*>).orEmpty().mapNotNull { item ->
        (item as? Map<*, *>)?.toFlexGridItems(childMapper, inheritShrink)
    }

    val width = MainAxisBehaviour.fromValueOrNull(this["width"] as? String) ?: MainAxisBehaviour.FILL
    val height = MainAxisBehaviour.fromValueOrNull(this["height"] as? String) ?: MainAxisBehaviour.FILL
    val hSpacing = (this["h_spacing"] as? Number)?.toFloat()?.takeIf { it != 0f }
    val vSpacing = (this["v_spacing"] as? Number)?.toFloat()?.takeIf { it != 0f }

    val transition = if (condition.isNotEmpty() && this["duration"] != null) this.toTransition() else null

    return FlexElement(
        condition = condition,
        direction = direction,
        rowElement = RowElement(items.map { it.first }, hSpacing, width, BaseProps()),
        columnElement = ColumnElement(items.map { it.second }, vSpacing, height, BaseProps()),
        transition = transition,
        baseProps = this.extractBaseProps(),
    )
}

private fun Map<*, *>.toFlexGridItems(
    childMapper: ChildMapperShrinkable,
    inheritShrink: Int,
): Pair<GridItem, GridItem> {
    val content = (this["content"] as? Map<*, *>)?.let { childMapper(it, inheritShrink) }
        ?: throw adaptyError(
            message = "content in flex item must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )
    val align = this.toAlign()
    val weight = this["weight"]?.toWeightOrNull()
    val fixed = this["fixed"]
    if (fixed == null && weight == null)
        throw adaptyError(
            message = "Either fixed or weight in flex item must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
        )
    return GridItem(
        dimAxis = DimSpec.Axis.X,
        sideSpec = fixed?.toDimSpec(DimSpec.Axis.X),
        content = content,
        align = align,
        baseProps = BaseProps(weight = weight),
    ) to GridItem(
        dimAxis = DimSpec.Axis.Y,
        sideSpec = fixed?.toDimSpec(DimSpec.Axis.Y),
        content = content,
        align = align,
        baseProps = BaseProps(weight = weight),
    )
}
