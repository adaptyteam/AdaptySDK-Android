package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyPeriodUnit
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.*

internal class AdaptyPeriodUnitSerializer : JsonSerializer<AdaptyPeriodUnit> {

    override fun serialize(
        src: AdaptyPeriodUnit,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.name.lowercase(Locale.ENGLISH))
    }
}