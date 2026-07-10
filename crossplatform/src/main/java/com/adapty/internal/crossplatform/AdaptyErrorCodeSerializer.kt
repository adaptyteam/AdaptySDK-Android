@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.internal.crossplatform

import com.adapty.errors.AdaptyErrorCode
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class AdaptyErrorCodeSerializer : JsonSerializer<AdaptyErrorCode> {

    override fun serialize(
        src: AdaptyErrorCode,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.value)
    }
}