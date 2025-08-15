package com.adapty.internal.data.cloud

import com.adapty.internal.utils.SingleVariationExtractHelper
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class FallbackVariationsExtractor(
    private val singleVariationExtractHelper: SingleVariationExtractHelper,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = jsonElement.asJsonObject

        val meta = jsonObject.remove(metaKey) as? JsonObject
        val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val variations = JsonArray()

        jsonObject.getAsJsonObject(dataKey).entrySet()
            .first { (key, value) ->
                val desiredPlacement = (value as? JsonObject)?.isEmpty == false
                desiredPlacement.also {
                    if (desiredPlacement) jsonObject.addProperty(placementIdKey, key)
                }
            }
            .value.asJsonObject
            .let { value ->
                val placement = value.getAsJsonObject(metaKey).getAsJsonObject(placementKey)
                value.getAsJsonArray(dataKey)
                    .forEach { element ->
                        if (element !is JsonObject) return@forEach
                        val variation = element.get(attributesKey) as? JsonObject ?: element
                        singleVariationExtractHelper.addSnapshotAtIfMissing(variation, snapshotAt)
                        singleVariationExtractHelper.addPlacementIfMissing(variation, placement)
                        variations.add(variation)
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
        const val placementKey = "placement"
        const val responseCreatedAtKey = "response_created_at"
    }
}