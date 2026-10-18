@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toTextAlign
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.TextElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toTextElement(assets: Assets): UIElement {
    return TextElement(
        this["string_id"]?.toStringId()
            ?: throw adaptyError(
                message = "string_id in Text must not be empty",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
        this["align"].toTextAlign(),
        (this["max_rows"] as? Number)?.toInt()?.takeIf { it > 0 },
        this.toOnOverflowMode(),
        this.toTextAttributes(),
        this.extractBaseProps(),
    )
}
