package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyInstallationDetails
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class AdaptyInstallationDetailsPayloadSerializer : JsonSerializer<AdaptyInstallationDetails.Payload> {

    override fun serialize(
        src: AdaptyInstallationDetails.Payload,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.jsonString)
    }
}