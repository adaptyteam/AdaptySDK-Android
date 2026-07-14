@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.mapping.attributes.toInterpolator
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.ui.element.RadialProgressElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toRadialProgressElement(assets: Assets): UIElement {
    val assetId = this["asset_id"]?.toVisualValue()
        ?: throw adaptyError(
            message = "asset_id in RadialProgress must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val value = this["value"]?.toOneWayBinding()
        ?: throw adaptyError(
            message = "value in RadialProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val duration = (this["duration"] as? Number)?.toInt()
        ?: throw adaptyError(
            message = "duration in RadialProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val actions = (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (this["action"] as? Map<*, *>)?.toAction()?.let { listOf(it) }
            .orEmpty()

    return RadialProgressElement(
        assetId = assetId,
        value = value,
        durationMillis = duration,
        min = (this["min"] as? Number)?.toFloat() ?: 0f,
        max = (this["max"] as? Number)?.toFloat() ?: 1f,
        skipAnimationOnOverflow = (this["skip_animation_on_overflow"] as? Boolean) ?: false,
        thickness = (this["thickness"] as? Number)?.toFloat(),
        sweepAngle = (this["sweep_angle"] as? Number)?.toFloat() ?: 360f,
        startAngle = (this["start_angle"] as? Number)?.toFloat() ?: -90f,
        clockwise = (this["clockwise"] as? Boolean) ?: true,
        roundedCaps = (this["rounded_caps"] as? Boolean) ?: false,
        clip = (this["clip"] as? Boolean) ?: true,
        interpolator = this["interpolator"].toInterpolator(),
        actions = actions,
        baseProps = this.extractBaseProps(),
    )
}
