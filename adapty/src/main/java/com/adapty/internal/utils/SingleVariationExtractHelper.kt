package com.adapty.internal.utils

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class SingleVariationExtractHelper {

    fun addSnapshotAtIfMissing(jsonObject: JsonObject, snapshotAt: JsonPrimitive) {
        if (!jsonObject.has(snapshotAtKey))
            jsonObject.add(snapshotAtKey, snapshotAt)
    }

    fun addPlacementIfMissing(jsonObject: JsonObject, placementJsonObject: JsonObject) {
        if (!jsonObject.has(placementKey))
            jsonObject.add(placementKey, placementJsonObject)
    }

    fun extractPlacementForCompatIfMissing(jsonObject: JsonObject) {
        if (!jsonObject.has(placementKey))
            jsonObject.add(
                placementKey,
                JsonObject().apply {
                    listOf(
                        "developer_id",
                        "audience_name",
                        "placement_audience_version_id",
                        "revision",
                        "ab_test_name",
                        "is_tracking_purchases",
                    ).forEach { key ->
                        jsonObject.get(key)?.let { value -> add(key, value) }
                    }
                }
            )
    }

    private companion object {
        const val snapshotAtKey = "snapshot_at"
        const val placementKey = "placement"
    }
}