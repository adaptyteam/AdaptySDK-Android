@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toTextAlign
import com.adapty.ui.internal.mapping.attributes.toInterpolator
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.TextProgressElement
import com.adapty.ui.internal.ui.element.TextProgressElement.Format
import com.adapty.ui.internal.ui.element.TextProgressElement.FormatItem
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toTextProgressElement(assets: Assets): UIElement {
    val value = this["value"]?.toOneWayBinding()
        ?: throw adaptyError(
            message = "value in TextProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val duration = (this["duration"] as? Number)?.toInt()
        ?: throw adaptyError(
            message = "duration in TextProgress must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    val actions = (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (this["action"] as? Map<*, *>)?.toAction()?.let { listOf(it) }
            .orEmpty()

    val format = this["format"]?.toStringId()?.let { stringId ->
        Format(listOf(FormatItem(Double.MAX_VALUE, stringId)))
    }
        ?: (this["format"] as? Iterable<*>)?.mapNotNull { item ->
            if (item !is Map<*, *>) return@mapNotNull null
            val stringId = item["string_id"]?.toStringId() ?: return@mapNotNull null
            FormatItem(
                (item["from"] as? Number)?.toDouble()
                    ?: throw adaptyError(
                        message = "from in TextProgress format item must not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                stringId,
            )
        }
            ?.sortedByDescending { it.fromValue }
            ?.takeIf { it.isNotEmpty() }
            ?.let { formatItems -> Format(formatItems) }
        ?: throw adaptyError(
            message = "format in TextProgress must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

    val textAlign = this["align"].toTextAlign()

    return TextProgressElement(
        value = value,
        durationMillis = duration,
        min = (this["min"] as? Number)?.toFloat() ?: 0f,
        max = (this["max"] as? Number)?.toFloat() ?: 1f,
        skipAnimationOnOverflow = (this["skip_animation_on_overflow"] as? Boolean) ?: false,
        interpolator = this["interpolator"].toInterpolator(),
        actions = actions,
        format = format,
        textAlign = textAlign,
        attributes = this.toTextAttributes(),
        baseProps = this.extractBaseProps(),
    )
}
