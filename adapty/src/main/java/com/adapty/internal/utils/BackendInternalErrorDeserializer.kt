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
    }

    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): Set<BackendError.InternalError> {
        when (jsonElement) {
            is JsonObject -> {
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