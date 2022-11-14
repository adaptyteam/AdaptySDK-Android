package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CustomAttributeValidator {

    fun validate(attrs: Map<String, Any?>) {
        var attrCount = 0
        attrs.forEach { (key, value) ->
            if (value != null) attrCount++
            when {
                attrCount > 10 ->
                    throwWrongParamError("There must be no more than 10 attributes")
                key.trim().length !in 1..30 ->
                    throwWrongParamError("The key must not be empty and be no more than 30 characters")
                value is String && value.trim().length !in 1..30 ->
                    throwWrongParamError("The value must not be empty and be no more than 30 characters")
                !"[\\dA-Za-z_.-]+".toRegex().matches(key) ->
                    throwWrongParamError("Only letters, numbers, dashes, points and underscores allowed in keys")
            }
        }
    }

    private fun throwWrongParamError(message: String): Nothing {
        Logger.logError { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER
        )
    }
}