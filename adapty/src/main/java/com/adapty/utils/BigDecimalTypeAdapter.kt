package com.adapty.utils

import com.google.gson.*
import java.lang.NumberFormatException
import java.lang.reflect.Type
import java.math.BigDecimal

internal class BigDecimalTypeAdapter : JsonDeserializer<BigDecimal> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        jsonElement: JsonElement,
        type: Type?,
        jsonDeserializationContext: JsonDeserializationContext?
    ): BigDecimal {
        return try {
            jsonElement.asBigDecimal
        } catch (e: NumberFormatException) {
            JsonPrimitive(
                jsonElement.asString.replace(",", ".").replace("[^0-9.]".toRegex(), "")
            ).asBigDecimal
        }
    }
}