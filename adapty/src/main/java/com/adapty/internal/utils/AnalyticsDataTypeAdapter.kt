package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AnalyticsData
import com.adapty.internal.data.models.AnalyticsEvent
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsDataTypeAdapter : JsonDeserializer<AnalyticsData>, JsonSerializer<AnalyticsData> {

    private val eventsListType = object : TypeToken<ArrayList<AnalyticsEvent>>() {}.type

    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): AnalyticsData? {
        when (jsonElement) {
            is JsonObject -> {
                val events = runCatching { jsonElement.getAsJsonArray(EVENTS) }.getOrNull()?.let { jsonArray ->
                    context.deserialize<ArrayList<AnalyticsEvent>>(jsonArray, eventsListType)
                } ?: arrayListOf()
                val prevOrdinal = runCatching { jsonElement.getAsJsonPrimitive(PREV_ORDINAL).asLong }.getOrNull() ?: 0L

                return AnalyticsData(events, prevOrdinal)
            }

            is JsonArray -> {
                val events =
                    context.deserialize<ArrayList<AnalyticsEvent>>(jsonElement, eventsListType)
                        .onEachIndexed { i, event -> event.ordinal = (i + 1).toLong() }
                val prevOrdinal = events.lastOrNull()?.ordinal ?: 0L

                return AnalyticsData(events, prevOrdinal)
            }

            else -> {
                return null
            }
        }
    }

    override fun serialize(
        src: AnalyticsData,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        return JsonObject().apply {
            add(EVENTS, context.serialize(src.events, eventsListType))
            addProperty(PREV_ORDINAL, src.prevOrdinal)
        }
    }

    private companion object {
        const val EVENTS = "events"
        const val PREV_ORDINAL = "prev_ordinal"
    }
}