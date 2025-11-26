package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyConfig
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class NetConfigExtractor(
    private val serverCluster: AdaptyConfig.ServerCluster,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = runCatching { jsonElement.asJsonObject }.getOrNull() ?: return null

        if (!jsonObject.has(dataKey))
            return extractInternal(jsonObject)

        val data = runCatching { jsonObject.getAsJsonObject(dataKey) }.getOrNull() ?: return null

        return extractInternal(data)
    }

    private fun extractInternal(jsonObject: JsonObject): JsonElement? {
        val expiresIn = runCatching {
            jsonObject.remove(expiresInKey).asJsonPrimitive.asNumber.toLong()
        }.getOrNull() ?: defaultExpiresIn

        jsonObject.addProperty(expiresAtKey, System.currentTimeMillis() + expiresIn)

        val retryInterval = runCatching {
            jsonObject.remove(retryIntervalKey).asJsonPrimitive.asNumber.toLong()
        }.getOrNull() ?: defaultRetryInterval

        jsonObject.addProperty(retryIntervalKey, retryInterval)

        val eventBlacklist = runCatching {
            jsonObject.remove(eventBlacklistKey).asJsonArray
        }.getOrNull() ?: JsonArray()

        jsonObject.add(eventsExcludedFromRecordingKey, eventBlacklist)
        jsonObject.add(eventsExcludedFromSendingKey, eventBlacklist)

        runCatching {
            val apiEndpoints = jsonObject.remove(apiEndpointsKey).asJsonObject
                .let { endpoints ->
                    when (serverCluster) {
                        AdaptyConfig.ServerCluster.DEFAULT -> {
                            runCatching {
                                endpoints.getAsJsonArray(serverCluster.name.lowercase(Locale.ENGLISH))
                            }.getOrNull()
                                ?: JsonArray()
                        }
                        else -> {
                            runCatching {
                                endpoints.getAsJsonArray(serverCluster.name.lowercase(Locale.ENGLISH))
                            }.getOrNull()
                                ?: runCatching {
                                    endpoints.getAsJsonArray(AdaptyConfig.ServerCluster.DEFAULT.name.lowercase(Locale.ENGLISH))
                                }.getOrNull()
                                ?: JsonArray()
                        }
                    }
                }

            jsonObject.add(apiEndpointsKey, apiEndpoints)
        }.getOrNull() ?: return null

        return jsonObject
    }

    private companion object {
        const val dataKey = "data"
        const val expiresInKey = "expires_in"
        const val expiresAtKey = "expires_at"
        const val retryIntervalKey = "retry_interval"
        const val eventBlacklistKey = "event_blacklist"
        const val eventsExcludedFromRecordingKey = "events_excluded_from_recording"
        const val eventsExcludedFromSendingKey = "events_excluded_from_sending"
        const val apiEndpointsKey = "api_endpoints"

        const val defaultExpiresIn = 3600_000L
        const val defaultRetryInterval = 1000L
    }
}