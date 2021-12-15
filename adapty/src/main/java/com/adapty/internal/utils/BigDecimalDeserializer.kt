package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.google.gson.*
import java.lang.reflect.Type
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BigDecimalDeserializer : JsonDeserializer<BigDecimal> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        jsonElement: JsonElement,
        type: Type?,
        jsonDeserializationContext: JsonDeserializationContext?
    ): BigDecimal {
        return try {
            jsonElement.asBigDecimal
        } catch (e: NumberFormatException) {
            try {
                JsonPrimitive(
                    jsonElement.asString.replace(",", ".").replace("[^0-9.]".toRegex(), "")
                ).asBigDecimal
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        }
    }
}