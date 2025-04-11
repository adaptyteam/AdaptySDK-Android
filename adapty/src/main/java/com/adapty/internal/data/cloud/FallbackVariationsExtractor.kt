package com.adapty.internal.data.cloud

import com.adapty.internal.utils.SinglePaywallExtractHelper
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class FallbackVariationsExtractor(
    private val singlePaywallExtractHelper: SinglePaywallExtractHelper,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = jsonElement.asJsonObject

        val meta = jsonObject.remove(metaKey) as? JsonObject
        val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val variations = JsonArray()

        jsonObject.getAsJsonObject(dataKey).entrySet()
            .first { (key, value) ->
                val desiredArray = (value as? JsonArray)?.isEmpty == false
                desiredArray.also {
                    if (desiredArray) jsonObject.addProperty(placementIdKey, key)
                }
            }
            .value.asJsonArray
            .forEach { element ->
                ((element as? JsonObject)?.get(attributesKey) as? JsonObject)
                    ?.let { paywall ->
                        singlePaywallExtractHelper.addSnapshotAtIfMissing(paywall, snapshotAt)
                        variations.add(paywall)
                    }
            }

        jsonObject.add(dataKey, variations)
        return jsonObject
    }

    private companion object {
        const val dataKey = "data"
        const val attributesKey = "attributes"
        const val metaKey = "meta"
        const val placementIdKey = "placement_id"
        const val responseCreatedAtKey = "response_created_at"
    }
}