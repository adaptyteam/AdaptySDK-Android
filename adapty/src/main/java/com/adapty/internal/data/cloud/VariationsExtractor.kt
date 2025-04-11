package com.adapty.internal.data.cloud

import com.adapty.internal.utils.SinglePaywallExtractHelper
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class VariationsExtractor(
    private val singlePaywallExtractHelper: SinglePaywallExtractHelper,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val meta = (jsonElement as? JsonObject)?.get(metaKey) as? JsonObject

        val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val version = (meta?.get(versionKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val variations = JsonArray()

        ((jsonElement as? JsonObject)?.get(dataKey) as? JsonArray)
            ?.forEach { element ->
                ((element as? JsonObject)?.get(attributesKey) as? JsonObject)
                    ?.let { paywall ->
                        singlePaywallExtractHelper.addSnapshotAtIfMissing(paywall, snapshotAt)
                        variations.add(paywall)
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
    }
}