@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toTextAlign
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.TimerElement
import com.adapty.ui.internal.ui.element.TimerElement.Format
import com.adapty.ui.internal.ui.element.TimerElement.FormatItem
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toTimerElement(assets: Assets): UIElement {
    val id = (this["id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: throw adaptyError(
            message = "id in Timer must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

    val actions = (this["action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
        ?: (this["action"] as? Map<*, *>)?.toAction()?.let { action -> listOf(action) }
            .orEmpty()

    val format = this["format"]?.toStringId()?.let { stringId ->
        Format(listOf(FormatItem(Long.MAX_VALUE, stringId)))
    }
        ?: (this["format"] as? Iterable<*>)?.mapNotNull { item ->
            if (item !is Map<*, *>) return@mapNotNull null
            val stringId = item["string_id"]?.toStringId() ?: return@mapNotNull null
            FormatItem(
                (item["from"] as? Number)?.toLong()
                    ?: throw adaptyError(
                        message = "from in Timer format item must not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                stringId,
            )
        }
            ?.sortedByDescending { it.fromSeconds }
            ?.takeIf { it.isNotEmpty() }
            ?.let { formatItems -> Format(formatItems) }
        ?: throw adaptyError(
            message = "format in Timer must not be empty",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )

    val textAlign = this["align"].toTextAlign()

    return TimerElement(
        id,
        actions,
        format,
        textAlign,
        this.toTextAttributes(),
        (this["max_rows"] as? Number)?.toInt()?.takeIf { it > 0 },
        this.toOnOverflowMode(),
        this.extractBaseProps(),
    )
}
