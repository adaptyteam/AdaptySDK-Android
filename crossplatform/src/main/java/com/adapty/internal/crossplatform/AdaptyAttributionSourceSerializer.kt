package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyAttributionSource
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class AdaptyAttributionSourceSerializer : JsonSerializer<AdaptyAttributionSource> {

    override fun serialize(
        src: AdaptyAttributionSource,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.value)
    }
}
