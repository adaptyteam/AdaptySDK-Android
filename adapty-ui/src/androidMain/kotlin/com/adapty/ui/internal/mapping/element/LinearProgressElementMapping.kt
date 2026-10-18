@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAspectRatio
import com.adapty.ui.internal.mapping.attributes.toCornerRadius
import com.adapty.ui.internal.mapping.attributes.toHorizontalAlign
import com.adapty.ui.internal.mapping.attributes.toInterpolator
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toVerticalAlign
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.ui.attributes.HorizontalAlign
import com.adapty.ui.internal.ui.attributes.VerticalAlign
import com.adapty.ui.internal.ui.element.LinearProgressElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toHorizontalProgressElement(assets: Assets): UIElement {
    val assetId = this["asset_id"]?.toVisualValue()
        ?: throw adaptyError(
            message = "asset_id in HorizontalProgress must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val value = this["value"]?.toOneWayBinding()
        ?: throw adaptyError(
            message = "value in HorizontalProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val duration = (this["duration"] as? Number)?.toInt()
        ?: throw adaptyError(
            message = "duration in HorizontalProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val actions = (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (this["action"] as? Map<*, *>)?.toAction()?.let { listOf(it) }
            .orEmpty()

    return LinearProgressElement(
        orientation = LinearProgressElement.Orientation.Horizontal(this["align"].toHorizontalAlign(HorizontalAlign.START)),
        assetId = assetId,
        value = value,
        durationMillis = duration,
        min = (this["min"] as? Number)?.toFloat() ?: 0f,
        max = (this["max"] as? Number)?.toFloat() ?: 1f,
        skipAnimationOnOverflow = (this["skip_animation_on_overflow"] as? Boolean) ?: false,
        cornerRadius = this["corner_radius"]?.toCornerRadius(),
        imageAspect = this["image_aspect"].toAspectRatio(),
        clip = (this["clip"] as? Boolean) ?: true,
        interpolator = this["interpolator"].toInterpolator(),
        actions = actions,
        baseProps = this.extractBaseProps(),
    )
}

internal fun Map<*, *>.toVerticalProgressElement(assets: Assets): UIElement {
    val assetId = this["asset_id"]?.toVisualValue()
        ?: throw adaptyError(
            message = "asset_id in VerticalProgress must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val value = this["value"]?.toOneWayBinding()
        ?: throw adaptyError(
            message = "value in VerticalProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val duration = (this["duration"] as? Number)?.toInt()
        ?: throw adaptyError(
            message = "duration in VerticalProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val actions = (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (this["action"] as? Map<*, *>)?.toAction()?.let { listOf(it) }
            .orEmpty()

    return LinearProgressElement(
        orientation = LinearProgressElement.Orientation.Vertical(this["align"].toVerticalAlign(VerticalAlign.BOTTOM)),
        assetId = assetId,
        value = value,
        durationMillis = duration,
        min = (this["min"] as? Number)?.toFloat() ?: 0f,
        max = (this["max"] as? Number)?.toFloat() ?: 1f,
        skipAnimationOnOverflow = (this["skip_animation_on_overflow"] as? Boolean) ?: false,
        cornerRadius = this["corner_radius"]?.toCornerRadius(),
        imageAspect = this["image_aspect"].toAspectRatio(),
        clip = (this["clip"] as? Boolean) ?: true,
        interpolator = this["interpolator"].toInterpolator(),
        actions = actions,
        baseProps = this.extractBaseProps(),
    )
}
