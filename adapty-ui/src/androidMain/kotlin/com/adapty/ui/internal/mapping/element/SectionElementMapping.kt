@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toInterpolator
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.ui.element.SectionElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toSectionElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    val baseProps = extractBaseProps()
    return SectionElement(
        this["index"]?.toOneWayBinding() ?: throw adaptyError(
            message = "index in Section must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        (this["content"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let { content -> childMapper(content, inheritShrink) }
        } ?: throw adaptyError(
            message = "content in Section must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        animationDurationMillis = (this["duration"] as? Number)?.toInt(),
        animationInterpolator = this["interpolator"].toInterpolator(),
        baseProps = baseProps,
    )
}
