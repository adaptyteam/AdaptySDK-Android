package com.adapty.internal.utils

import com.adapty.internal.data.models.BackendError
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class BackendInternalErrorDeserializer : JsonDeserializer<Set<BackendError.InternalError>> {

    private companion object {
        const val ERRORS = "errors"
        const val CODE = "code"
        const val ERROR_CODE = "error_code"
    }

    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): Set<BackendError.InternalError> {
        when (jsonElement) {
            is JsonObject -> {
                kotlin.runCatching { jsonElement.getAsJsonArray("detail") }.getOrNull()
                    ?.let { detail ->
                        return detail.mapNotNull { element ->
                            runCatching { element.asJsonObject.getAsJsonPrimitive("type").asString }
                                .getOrNull()?.let { type -> BackendError.InternalError(type) }
                        }.toSet()
                    }

                kotlin.runCatching { jsonElement.getAsJsonPrimitive(ERROR_CODE).asString }.getOrNull()
                    ?.let { code -> return setOf(BackendError.InternalError(code)) }

                val errors = runCatching { jsonElement.getAsJsonArray(ERRORS) }.getOrNull() ?: return emptySet()

                return errors.mapNotNullTo(mutableSetOf()) { error ->
                    runCatching { error.asJsonObject.get(CODE).asString }.getOrNull()
                        ?.let { code -> BackendError.InternalError(code) }
                }
            }

            else -> {
                return setOf()
            }
        }
    }
}
