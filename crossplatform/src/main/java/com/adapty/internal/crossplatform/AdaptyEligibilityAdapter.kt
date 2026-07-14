package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyEligibility
import com.google.gson.*
import java.lang.reflect.Type
import java.util.*

internal class AdaptyEligibilityAdapter : JsonSerializer<AdaptyEligibility>,
    JsonDeserializer<AdaptyEligibility> {

    override fun serialize(
        src: AdaptyEligibility,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.name.lowercase(Locale.ENGLISH))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AdaptyEligibility {
        return when (json.asString) {
            "eligible" -> AdaptyEligibility.ELIGIBLE
            "ineligible" -> AdaptyEligibility.INELIGIBLE
            else -> AdaptyEligibility.NOT_APPLICABLE
        }
    }
}