package com.adapty.internal.utils

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.RemoteConfigDto
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

internal class RemoteConfigDtoDeserializer : JsonDeserializer<RemoteConfigDto> {

    private val dataMapType by lazy {
        object : TypeToken<HashMap<String, Any>>() {}.type
    }

    private companion object {
        const val LANG = "lang"
        const val DATA = "data"
    }

    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): RemoteConfigDto? {
        when (jsonElement) {
            is JsonObject -> {
                val lang = kotlin.runCatching { jsonElement.getAsJsonPrimitive(LANG).asString }.getOrNull()
                    ?: throw AdaptyError(
                        message = "lang in RemoteConfig should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                    )

                val data = kotlin.runCatching { jsonElement.get(DATA) }.getOrNull()
                    ?: return null

                val dataStr = kotlin.runCatching { data.asString }.getOrNull()
                    ?: throw AdaptyError(
                        message = "data in RemoteConfig should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                    )

                val dataMap = kotlin.runCatching {
                    context.deserialize<Map<String, Any>>(JsonParser.parseString(dataStr), dataMapType) ?: emptyMap()
                }.getOrElse { e ->
                    throw AdaptyError(
                        originalError = e,
                        message = "Couldn't parse data string in RemoteConfig: ${e.localizedMessage}",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }

                return RemoteConfigDto(lang, dataStr, dataMap)
            }

            else -> {
                return null
            }
        }
    }
}
