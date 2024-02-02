package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AnalyticsEvent
import com.google.android.gms.common.util.Base64Utils
import com.google.gson.*
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsEventTypeAdapter : JsonDeserializer<AnalyticsEvent>, JsonSerializer<AnalyticsEvent> {
    override fun deserialize(
        jsonElement: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): AnalyticsEvent? {
        val jsonObject = runCatching { jsonElement.asJsonObject }.getOrNull() ?: return null
        val eventJson = if (jsonObject.has(DATA) && jsonObject.has(PARTITION_KEY)) {
            runCatching { jsonObject.get(DATA).asString }.getOrNull()?.let { encoded ->
                val decoded = Base64Utils.decode(encoded.replace("\\u003d", "=")).decodeToString()
                runCatching { JsonParser.parseString(decoded).asJsonObject }.getOrNull() ?: return null
            } ?: return null
        } else {
            jsonObject
        }

        val otherParams = hashMapOf<String, Any>()
        eventJson.keySet().forEach { key ->
            if (key !in defaultKeys)
                (eventJson[key] as? JsonPrimitive)?.let { value -> otherParams[key] = value }
        }

        val eventId = getEventParam(eventJson, EVENT_ID) ?: return null
        val eventName = getEventParam(eventJson, EVENT_NAME) ?: return null
        val profileId = getEventParam(eventJson, PROFILE_ID) ?: return null
        val sessionId = getEventParam(eventJson, SESSION_ID) ?: return null
        val deviceId = getEventParam(eventJson, DEVICE_ID) ?: getEventParam(eventJson, DEVICE_ID_OLD) ?: return null
        val createdAt = getEventParam(eventJson, CREATED_AT) ?: return null
        val platform = getEventParam(eventJson, PLATFORM) ?: return null
        val ordinal = runCatching { eventJson.getAsJsonPrimitive(COUNTER).asLong }.getOrNull() ?: 0L

        return AnalyticsEvent(eventId, eventName, profileId, sessionId, deviceId, createdAt, platform, otherParams)
            .also { event -> event.ordinal = ordinal }
    }

    override fun serialize(
        src: AnalyticsEvent,
        typeOfSrc: Type,
        context: JsonSerializationContext,
    ): JsonElement {
        return JsonObject().apply {
            addProperty(EVENT_ID, src.eventId)
            addProperty(EVENT_NAME, src.eventName)
            addProperty(PROFILE_ID, src.profileId)
            addProperty(SESSION_ID, src.sessionId)
            addProperty(DEVICE_ID, src.deviceId)
            addProperty(CREATED_AT, src.createdAt)
            addProperty(PLATFORM, src.platform)
            addProperty(COUNTER, src.ordinal)
            src.other.forEach { (key, value) ->
                when (value) {
                    is Number -> add(key, JsonPrimitive(value))
                    is String -> add(key, JsonPrimitive(value))
                    is Boolean -> add(key, JsonPrimitive(value))
                    else -> Unit
                }
            }
        }
    }

    private fun getEventParam(eventJson: JsonObject, paramKey: String): String? {
        return runCatching { eventJson.getAsJsonPrimitive(paramKey).asString }.getOrNull()
    }

    private companion object {
        const val EVENT_ID = "event_id"
        const val EVENT_NAME = "event_name"
        const val PROFILE_ID = "profile_id"
        const val SESSION_ID = "session_id"
        const val DEVICE_ID = "device_id"
        const val DEVICE_ID_OLD = "profile_installation_meta_id"
        const val CREATED_AT = "created_at"
        const val PLATFORM = "platform"
        const val COUNTER = "counter"

        const val DATA = "Data"
        const val PARTITION_KEY = "PartitionKey"

        val defaultKeys = setOf(
            EVENT_ID,
            EVENT_NAME,
            PROFILE_ID,
            SESSION_ID,
            DEVICE_ID,
            CREATED_AT,
            PLATFORM,
            COUNTER,
        )
    }
}