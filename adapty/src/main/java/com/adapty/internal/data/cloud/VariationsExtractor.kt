package com.adapty.internal.data.cloud

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.SingleVariationExtractHelper
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class VariationsExtractor(
    private val singleVariationExtractHelper: SingleVariationExtractHelper,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val meta = (jsonElement as? JsonObject)?.get(metaKey) as? JsonObject

        val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val version = (meta?.get(versionKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val placement = meta?.get(placementKey) as? JsonObject
            ?: throw AdaptyError(
                message = "placement in meta should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        val variations = JsonArray()

        ((jsonElement as? JsonObject)?.get(dataKey) as? JsonArray)
            ?.forEach { element ->
                (element as? JsonObject)
                    ?.let { data ->
                        val variation = data.get(attributesKey) as? JsonObject ?: data
                        singleVariationExtractHelper.addSnapshotAtIfMissing(variation, snapshotAt)
                        singleVariationExtractHelper.addPlacementIfMissing(variation, placement)
                        variations.add(variation)
                    }
            }

        return JsonObject().apply {
            add(dataKey, variations)
            add(snapshotAtKey, snapshotAt)
            add(versionKey, version)
        }
    }

    private companion object {
        const val dataKey = "data"
        const val attributesKey = "attributes"
        const val metaKey = "meta"
        const val versionKey = "version"
        const val responseCreatedAtKey = "response_created_at"
        const val snapshotAtKey = "snapshot_at"
        const val placementKey = "placement"
    }
}