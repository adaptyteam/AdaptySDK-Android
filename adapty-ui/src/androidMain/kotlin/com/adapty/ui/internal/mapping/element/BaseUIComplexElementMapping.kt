@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAlign
import com.adapty.ui.internal.mapping.attributes.toDimSpec
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.GridItem
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.NO_SHRINK

internal fun Map<*, *>.toGridItem(dimAxis: DimSpec.Axis, childMapper: (Map<*, *>) -> UIElement?): GridItem {
    return GridItem(
        dimAxis = dimAxis,
        sideSpec = this["fixed"]?.let { item -> item.toDimSpec(dimAxis) },
        content = (this["content"] as? Map<*, *>)?.let(childMapper::invoke)
            ?: throw adaptyError(
                message = "content in RowItem must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
        align = this.toAlign(),
        baseProps = BaseProps(weight = this["weight"]?.toWeightOrNull()),
    ).also { gridItem ->
        val isRow = dimAxis == DimSpec.Axis.X
        val baseProps = gridItem.baseProps
        val hasSideSpec = (isRow && baseProps.widthSpec != null) || (!isRow && baseProps.heightSpec != null)
        if (!hasSideSpec && baseProps.weight == null)
            throw adaptyError(
                message = "Either side or weight in GridItem must not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
    }
}

internal fun Map<*, *>.extractBasePropsWithShrinkInheritance(inheritShrink: Int): Pair<BaseProps, Int> {
    if (!this.hasChildren())
        return extractBaseProps() to NO_SHRINK

    var nextInheritShrink = inheritShrink
    val inheritWidth = inheritShrink and SHRINK_WIDTH_ONLY != 0
    val inheritHeight = inheritShrink and SHRINK_HEIGHT_ONLY != 0
    val baseProps = extractBaseProps().run {
        if (!inheritWidth && !inheritHeight) {
            if (this.widthSpec is DimSpec.Shrink)
                nextInheritShrink = nextInheritShrink or SHRINK_WIDTH_ONLY
            if (this.heightSpec is DimSpec.Shrink)
                nextInheritShrink = nextInheritShrink or SHRINK_HEIGHT_ONLY
            return@run this
        }

        var newWidthSpec: DimSpec? = null
        var newHeightSpec: DimSpec? = null
        if (inheritWidth) {
            when (val widthSpec = this.widthSpec) {
                is DimSpec.Specified -> nextInheritShrink = nextInheritShrink and SHRINK_HEIGHT_ONLY
                is DimSpec.Min -> {
                    if (this@extractBasePropsWithShrinkInheritance.isVerticalContainer())
                        newWidthSpec = DimSpec.Shrink(min = widthSpec.value, maxValue = widthSpec.maxValue, DimSpec.Axis.X)
                }
                null -> {
                    if (this@extractBasePropsWithShrinkInheritance.isVerticalContainer())
                        newWidthSpec = DimSpec.Shrink(min = DimUnit.Exact(0f), null, DimSpec.Axis.X)
                }
                is DimSpec.Shrink -> nextInheritShrink = nextInheritShrink or SHRINK_WIDTH_ONLY
                is DimSpec.FillMax -> {}
            }
        }
        if (inheritHeight) {
            when (val heightSpec = this.heightSpec) {
                is DimSpec.Specified -> nextInheritShrink = nextInheritShrink and SHRINK_WIDTH_ONLY
                is DimSpec.Min -> {
                    if (this@extractBasePropsWithShrinkInheritance.isHorizontalContainer())
                        newHeightSpec = DimSpec.Shrink(min = heightSpec.value, maxValue = heightSpec.maxValue, DimSpec.Axis.Y)
                }
                null -> {
                    if (this@extractBasePropsWithShrinkInheritance.isHorizontalContainer())
                        newHeightSpec = DimSpec.Shrink(min = DimUnit.Exact(0f), maxValue = null, DimSpec.Axis.Y)
                }
                is DimSpec.Shrink -> nextInheritShrink = nextInheritShrink or SHRINK_HEIGHT_ONLY
                is DimSpec.FillMax -> {}
            }
        }

        if (newWidthSpec == null && newHeightSpec == null)
            return@run this

        this.copy(widthSpec = newWidthSpec ?: this.widthSpec, heightSpec = newHeightSpec ?: this.heightSpec)
    }

    val children = this.children
    if (children != null && children.size > 1 && children.any { item -> (item as? Map<*, *>)?.get("type") !in multiContainerTypes }) {
        nextInheritShrink = NO_SHRINK
    }

    return Pair(baseProps, nextInheritShrink)
}

private fun Map<*, *>.hasChildren() = this["content"] is Map<*, *> || this["content"] is Collection<*>
private fun Map<*, *>.isHorizontalContainer() = this["type"] in horizontalContainerTypes
private fun Map<*, *>.isVerticalContainer() = this["type"] in verticalContainerTypes
private val Map<*, *>.children get() = (this["content"] as? Map<*, *>)?.let { listOf(it) } ?: (this["content"] as? List<*>)
private const val SHRINK_WIDTH_ONLY = 0b1
private const val SHRINK_HEIGHT_ONLY = 0b10

private val horizontalContainerTypes = setOf("row", "legacy_row", "h_stack")
private val verticalContainerTypes = setOf("column", "legacy_column", "v_stack")
private val multiContainerTypes = setOf(*horizontalContainerTypes.toTypedArray(), *verticalContainerTypes.toTypedArray(), "z_stack")
