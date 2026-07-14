package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyProductSubscriptionDetails.RenewalType
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.Locale

internal class AdaptyRenewalTypeAdapter : JsonSerializer<RenewalType>,
    JsonDeserializer<RenewalType> {

    override fun serialize(
        src: RenewalType,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.name.lowercase(Locale.ENGLISH))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): RenewalType {
        return when (json.asString) {
            "prepaid" -> RenewalType.PREPAID
            else -> RenewalType.AUTORENEWABLE
        }
    }
}