package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyConfig.StoreMessagesHandling
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal class AdaptyConfigStoreMessagesHandlingDeserializer : JsonDeserializer<StoreMessagesHandling> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StoreMessagesHandling {
        return runCatching { json.asJsonPrimitive.asString }.getOrNull().let { value ->
            when(value) {
                "manual" -> StoreMessagesHandling.MANUAL
                else -> StoreMessagesHandling.AUTO
            }
        }
    }
}
