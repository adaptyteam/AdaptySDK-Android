package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyExternalAttributionProvider
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class AdaptyExternalAttributionProviderSerializer : JsonSerializer<AdaptyExternalAttributionProvider> {

    override fun serialize(
        src: AdaptyExternalAttributionProvider,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.value)
    }
}
