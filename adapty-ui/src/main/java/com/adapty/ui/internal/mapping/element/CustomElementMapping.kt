@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.mapping.attributes.toVisualValue
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.CustomElement
import com.adapty.ui.internal.ui.element.UIElement
import com.adapty.ui.internal.utils.NO_SHRINK
import com.adapty.ui.internal.utils.StringSource
import com.adapty.ui.internal.utils.VisualValue

internal fun Map<*, *>.toCustomElement(
    assets: Assets,
    stateMap: StateMap,
    inheritShrink: Int,
    childMapper: ChildMapperShrinkable,
): UIElement {
    return CustomElement(
        customId = requireCustomString("custom_id"),
        customType = requireCustomString("custom_type"),
        assets = mapCustomReferences("assets") {
            if (it == "") VisualValue.any(StringSource.Value("")) else it.toVisualValue()
        },
        strings = mapCustomReferences("strings") {
            if (it == "") StringId.Str(StringSource.Value(""), null) else it.toStringId()
        },
        bindings = mapCustomReferences("bindings") { it.toTwoWayBinding() },
        payload = when (val payload = this["payload"]) {
            null -> null
            is String -> payload
            else -> throw customDecodingError("payload in Custom must be a string")
        },
        fallback = when (val fallback = this["fallback"]) {
            null -> null
            is Map<*, *> -> childMapper(fallback, NO_SHRINK)
            else -> throw customDecodingError("fallback in Custom must be an element")
        },
        baseProps = this.extractBaseProps(),
    )
}

private fun Map<*, *>.requireCustomString(key: String): String =
    this[key] as? String ?: throw customDecodingError("$key in Custom must be a string")

private fun <T : Any> Map<*, *>.mapCustomReferences(
    key: String,
    mapValue: (Any) -> T?,
): Map<String, T>? {
    val references = when (val value = this[key]) {
        null -> return null
        is Map<*, *> -> value
        else -> throw customDecodingError("$key in Custom must be an object")
    }
    return references.entries.associate { (refKey, refValue) ->
        val name = refKey as? String
            ?: throw customDecodingError("$key in Custom must have string keys")
        name to (refValue?.let(mapValue)
            ?: throw customDecodingError("$key.$name in Custom is not a valid reference"))
    }
}

private fun customDecodingError(message: String) =
    adaptyError(message = message, adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED)
