package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyProfile
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonWriter

internal class AdaptyProfileTypeAdapterFactory :
    BaseTypeAdapterFactory<AdaptyProfile>(AdaptyProfile::class.java) {

    private companion object {
        const val SEGMENT_HASH = "segment_hash"
        const val TIMESTAMP = "timestamp"
        const val APPLIED_ATTRIBUTION_SOURCES = "applied_attribution_sources"
    }

    override fun write(
        out: JsonWriter,
        value: AdaptyProfile,
        delegateAdapter: TypeAdapter<AdaptyProfile>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject
        jsonObject.addProperty(SEGMENT_HASH, "not implemented")
        jsonObject.addProperty(TIMESTAMP, -1)
        val attributionSources = jsonObject.get(APPLIED_ATTRIBUTION_SOURCES)
        if (attributionSources is JsonArray && attributionSources.isEmpty)
            jsonObject.remove(APPLIED_ATTRIBUTION_SOURCES)
        elementAdapter.write(out, jsonObject)
    }
}