@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.AttributionData
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.google.gson.Gson

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AttributionHelper(
    private val gson: Gson,
) {

    fun createAttributionData(
        attribution: Map<String, Any>,
        provider: String,
        profileId: String,
    ): AttributionData {
        validate(attribution)
        return createAttributionData(gson.toJson(attribution), provider, profileId)
    }

    private fun validate(value: Any?) {
        when (value) {
            null, is String, is Boolean -> Unit
            is Double -> if (!value.isFinite()) throwWrongParamError("Attribution values must not contain NaN or infinite numbers")
            is Float -> if (!value.isFinite()) throwWrongParamError("Attribution values must not contain NaN or infinite numbers")
            is Number -> Unit
            is Map<*, *> -> value.forEach { (key, nested) ->
                if (key !is String) throwWrongParamError("Attribution keys must be strings")
                validate(nested)
            }
            is Iterable<*> -> value.forEach(::validate)
            is Array<*> -> value.forEach(::validate)
            else -> throwWrongParamError("Attribution values must be JSON-compatible: strings, numbers, booleans, nulls, maps or lists; got ${value.javaClass.name}")
        }
    }

    private fun throwWrongParamError(message: String): Nothing {
        Logger.log(ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
        )
    }

    fun createAttributionData(
        attributionJson: String,
        provider: String,
        profileId: String,
    ) = AttributionData(
        provider,
        attributionJson,
        profileId,
    )
}
