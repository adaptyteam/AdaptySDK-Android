package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.requests.SendEventRequest
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SendEventRequestSerializer : JsonSerializer<SendEventRequest> {

    private companion object {
        const val DATA = "data"
        const val TYPE = "type"
        const val ATTRS = "attributes"
        const val EVENTS = "events"
    }

    override fun serialize(
        src: SendEventRequest,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        val attrsJsonObject = JsonObject().apply {
            add(EVENTS, context.serialize(src.events))
        }

        val dataJsonObject = JsonObject().apply {
            addProperty(TYPE, "sdk_background_event")
            add(ATTRS, attrsJsonObject)
        }

        return JsonObject().apply {
            add(DATA, dataJsonObject)
        }
    }
}