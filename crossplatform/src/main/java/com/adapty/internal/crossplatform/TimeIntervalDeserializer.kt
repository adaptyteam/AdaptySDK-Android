@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import com.adapty.internal.utils.DEFAULT_PLACEMENT_TIMEOUT
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.utils.TimeInterval
import com.adapty.utils.millis
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal class TimeIntervalDeserializer : JsonDeserializer<TimeInterval> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): TimeInterval {
        return kotlin.runCatching {
            json.asJsonPrimitive.asNumber.toDouble().times(PLACEMENT_TIMEOUT_MULTIPLIER)
                .toInt().millis
        }.getOrNull() ?: DEFAULT_PLACEMENT_TIMEOUT
    }

    private companion object {
        const val PLACEMENT_TIMEOUT_MULTIPLIER = 1000
    }
}