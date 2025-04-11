package com.adapty.internal.utils

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class SinglePaywallExtractHelper {

    fun addSnapshotAtIfMissing(jsonObject: JsonObject, snapshotAt: JsonPrimitive) {
        if (!jsonObject.has(snapshotAtKey))
            jsonObject.add(snapshotAtKey, snapshotAt)
    }

    private companion object {
        const val snapshotAtKey = "snapshot_at"
    }
}