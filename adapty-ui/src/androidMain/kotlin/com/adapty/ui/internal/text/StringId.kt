@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.StringSource

@InternalAdaptyApi
public sealed class StringId {
    public class Str internal constructor(
        internal val source: StringSource,
        internal val tagValues: Map<String, TagValueSource>?,
    ): StringId()

    public class Product internal constructor(
        internal val productIdSource: StringSource,
        internal val suffix: String?,
    ): StringId()
}

internal fun Any.toStringId(): StringId? {
    if (this is Map<*, *>) {
        val productId = this["product"]?.let { mapStringSource(it) }

        if (productId != null) {
            val suffix = this["suffix"] as? String
            return StringId.Product(productId, suffix)
        }

        val nestedStringId = this["string_id"]
        if (nestedStringId != null) {
            val source = mapStringSource(nestedStringId) ?: return null
            val tagValues = mutableMapOf<String, TagValueSource>()
            for ((key, value) in this) {
                val keyStr = key as? String ?: continue
                if (keyStr == "string_id") continue
                val tagSource = value?.let { mapTagValueSource(it) } ?: continue
                tagValues[keyStr] = tagSource
            }
            return StringId.Str(source, tagValues.ifEmpty { null })
        }
    }

    return mapStringSource(this)?.let { StringId.Str(it, null) }
}

private fun mapStringSource(item: Any): StringSource? {
    return when {
        item is String && item.isNotEmpty() -> StringSource.Value(item)
        item is Map<*, *> -> StringSource.Binding(parseVariable(item), parseScope(item), item["converter"] as? String, item["converter_params"])
        else -> null
    }
}

private fun mapTagValueSource(item: Any): TagValueSource? {
    return when {
        item is String && item.isNotEmpty() -> TagValueSource.Literal(item)
        item is Map<*, *> -> TagValueSource.Binding(parseVariable(item), parseScope(item), item["converter"] as? String, item["converter_params"])
        else -> null
    }
}

private fun parseVariable(item: Map<*, *>): String =
    (item["var"] as? String) ?: throw adaptyError(
        message = "Couldn't find 'var' for data binding",
        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
    )

private fun parseScope(item: Map<*, *>): Scope = when (item["scope"] as? String) {
    "global" -> Scope.Global
    else -> Scope.Screen
}
