@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.utils.AdaptyLogLevel
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal class AdaptyLogLevelDeserializer : JsonDeserializer<AdaptyLogLevel> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AdaptyLogLevel? {
        return kotlin.runCatching { json.asJsonPrimitive.asString }.getOrNull().let { value ->
            when(value) {
                "none" -> AdaptyLogLevel.NONE
                "error" -> AdaptyLogLevel.ERROR
                "warn" -> AdaptyLogLevel.WARN
                "info" -> AdaptyLogLevel.INFO
                "verbose", "debug" -> AdaptyLogLevel.VERBOSE
                else -> null
            }
        }
    }
}