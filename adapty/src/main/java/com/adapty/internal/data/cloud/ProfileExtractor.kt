package com.adapty.internal.data.cloud

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal class ProfileExtractor: ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = jsonElement.asJsonObject

        if (!jsonObject.has(dataKey))
            return extractInternal(jsonObject)

        val data = jsonObject.getAsJsonObject(dataKey).getAsJsonObject(attributesKey)

        return extractInternal(data)
    }

    private fun extractInternal(jsonObject: JsonObject): JsonElement? {
        jsonObject.requires("profile_id") { "profileId in Profile should not be null" }
        jsonObject.requires("segment_hash") { "segmentHash in Profile should not be null" }

        return jsonObject
    }

    private inline fun JsonObject.requires(key: String, errorMessage: () -> String) {
        if (!has(key))
            throw AdaptyError(
                message = errorMessage(),
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )
    }

    private companion object {
        const val dataKey = "data"
        const val attributesKey = "attributes"
    }
}