package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CustomAttributeValidator {

    fun validate(attrs: Map<String, Any?>) {
        var attrCount = 0
        attrs.forEach { (key, value) ->
            if (value != null) attrCount++
            when {
                attrCount > MAX_ATTRS_COUNT ->
                    throwWrongParamError("There must be no more than $MAX_ATTRS_COUNT attributes")
                key.trim().length !in 1..MAX_KEY_LENGTH ->
                    throwWrongParamError("The key must not be empty and be no more than $MAX_KEY_LENGTH characters")
                value is String && value.trim().length !in 1..MAX_VALUE_LENGTH ->
                    throwWrongParamError("The value must not be empty and be no more than $MAX_VALUE_LENGTH characters")
                !"[\\dA-Za-z_.-]+".toRegex().matches(key) ->
                    throwWrongParamError("Only letters, numbers, dashes, points and underscores allowed in keys")
            }
        }
    }

    private fun throwWrongParamError(message: String): Nothing {
        Logger.log(ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
        )
    }

    private companion object {
        const val MAX_ATTRS_COUNT = 30
        const val MAX_KEY_LENGTH = 30
        const val MAX_VALUE_LENGTH = 50
    }
}