@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAssetVisualValue
import com.adapty.ui.internal.mapping.attributes.toDimSpec
import com.adapty.ui.internal.mapping.attributes.toEdgeEntities
import com.adapty.ui.internal.mapping.attributes.toOffset
import com.adapty.ui.internal.mapping.attributes.toShape
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.mapping.attributes.toStaticRotation
import com.adapty.ui.internal.mapping.attributes.toStaticScale
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.ui.attributes.Animation
import com.adapty.ui.internal.ui.attributes.Box
import com.adapty.ui.internal.ui.attributes.DimSpec
import com.adapty.ui.internal.ui.attributes.DimUnit
import com.adapty.ui.internal.ui.attributes.EdgeEntities
import com.adapty.ui.internal.ui.element.BaseProps
import com.adapty.ui.internal.ui.element.BaseTextElement.OnOverflowMode
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.ui.event.EventHandler
import com.adapty.ui.internal.ui.event.appearAnimations

@InternalAdaptyApi
public fun Map<*, *>.extractBaseProps(): BaseProps {
    val isBox = this["type"] == "box"
    val eventHandlers = extractEventHandlers()
    val onAppearStarts = eventHandlers?.deriveOnAppearAnimationStarts()
    val parsedWidthSpec =
        if (isBox) this["width"]?.let { item -> item.toDimSpec(DimSpec.Axis.X) } else null
    val parsedHeightSpec =
        if (isBox) this["height"]?.let { item -> item.toDimSpec(DimSpec.Axis.Y) } else null
    val widthSpec = if (parsedWidthSpec is DimSpec.Specified)
        onAppearStarts?.widthStart?.let { DimSpec.Specified(it, DimSpec.Axis.X) } ?: parsedWidthSpec
    else parsedWidthSpec
    val heightSpec = if (parsedHeightSpec is DimSpec.Specified)
        onAppearStarts?.heightStart?.let { DimSpec.Specified(it, DimSpec.Axis.Y) } ?: parsedHeightSpec
    else parsedHeightSpec
    return BaseProps(
        widthSpec,
        heightSpec,
        this["weight"]?.toWeightOrNull(),
        this["decorator"]?.toShape(),
        this["padding"]?.toEdgeEntities(),
        this["offset"]?.toOffset(),
        (this["opacity"] as? Number)?.toFloat() ?: 1f,
        this["focus_id"] as? String,
        this["rotation"]?.toStaticRotation(),
        this["scale"]?.toStaticScale(),
        this["ui_enabled"]?.toOneWayBinding(),
        eventHandlers,
    )
}

private data class OnAppearAnimationStarts(
    val widthStart: DimUnit? = null,
    val heightStart: DimUnit? = null,
)

private fun List<EventHandler>.deriveOnAppearAnimationStarts(): OnAppearAnimationStarts? {
    val anims = appearAnimations()
        .takeIf { it.isNotEmpty() }
        ?: return null

    val boxStart = anims
        .filter { it.role == Animation.Role.Box }
        .minByOrNull { it.startDelayMillis }
        ?.start as? Box
    val widthStart = boxStart?.width
    val heightStart = boxStart?.height
    if (widthStart == null && heightStart == null) return null
    return OnAppearAnimationStarts(widthStart, heightStart)
}

internal fun Map<*, *>.toTextAttributes(): Attributes =
    Attributes(
        this["font"]?.toAssetVisualValue(),
        this["size"]?.toFloatOrNull(),
        (this["strike"] as? Boolean) ?: false,
        (this["underline"] as? Boolean) ?: false,
        this["color"]?.toVisualValue(),
        this["background"]?.toVisualValue(),
        this["tint"]?.toVisualValue(),
        this["letter_spacing"]?.toFloatOrNull(),
        this["line_height"]?.toFloatOrNull(),
    )

internal fun Any.toFloatOrNull(): Float? = (this as? Number)?.toFloat()

internal fun Any.toWeightOrNull(): Float? =
    toFloatOrNull()?.also { weight ->
        if (weight <= 0f)
            throw adaptyError(
                message = "weight ($weight) must be greater than 0",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
    }

internal fun Map<*, *>.extractSpacingOrNull(): Float? =
    this["spacing"]?.toFloatOrNull()?.takeIf { it != 0f }

internal fun Map<*, *>.toOnOverflowMode(): OnOverflowMode? =
    when (val value = this["on_overflow"]) {
        is List<*> -> if ("scale" in value) OnOverflowMode.SCALE else null
        else -> if (value == "scale") OnOverflowMode.SCALE else null
    }

internal fun shouldSkipContainer(content: Collection<UIElement>?, baseProps: BaseProps): Boolean =
    content.isNullOrEmpty()
            && baseProps.padding.isNullOrEmpty()
            && baseProps.widthSpec.isNullOrEmpty()
            && baseProps.heightSpec.isNullOrEmpty()

internal fun checkAsset(assetId: String, assets: Assets) {
    if (assets[assetId] == null)
        throw adaptyError(
            message = "asset_id ($assetId) does not exist",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
}

private fun EdgeEntities?.isNullOrEmpty(): Boolean {
    if (this == null) return true
    return listOf(start, top, end, bottom).all { (it as? DimUnit.Exact)?.value == 0f }
}

private fun DimSpec?.isNullOrEmpty(): Boolean {
    if (this !is DimSpec.Specified) return true
    return (value as? DimUnit.Exact)?.value == 0f
}
