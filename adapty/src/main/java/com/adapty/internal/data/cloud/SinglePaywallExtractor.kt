package com.adapty.internal.data.cloud

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.SinglePaywallExtractHelper
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class SinglePaywallExtractor(
    private val singlePaywallExtractHelper: SinglePaywallExtractHelper,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = jsonElement.asJsonObject

        if (!jsonObject.has(dataKey) || !jsonObject.has(metaKey))
            return extractInternal(jsonObject)

        val meta = (jsonElement as? JsonObject)?.get(metaKey) as? JsonObject
        val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)

        val data = jsonObject.getAsJsonObject(dataKey).getAsJsonObject(attributesKey)
        singlePaywallExtractHelper.addSnapshotAtIfMissing(data, snapshotAt)

        return extractInternal(data)
    }

    private fun extractInternal(jsonObject: JsonObject): JsonElement? {
        jsonObject.requires("developer_id") { "placementId in Paywall should not be null" }
        jsonObject.requires("variation_id") { "variationId in Paywall should not be null" }
        jsonObject.requires("paywall_id") { "paywallId in Paywall should not be null" }
        jsonObject.requires("placement_audience_version_id") { "placementAudienceVersionId in Paywall should not be null" }
        jsonObject.requires("revision") { "revision in Paywall should not be null" }
        jsonObject.requires("ab_test_name") { "abTestName in Paywall should not be null" }
        jsonObject.requires("paywall_name") { "name in Paywall should not be null" }

        val weight = runCatching { jsonObject.getAsJsonPrimitive("weight").asInt }.getOrNull()

        if (weight == null || weight !in 1..100)
            throw AdaptyError(
                message = "weight in Paywall should be between 1 and 100. Currently, it is $weight",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        val products = runCatching { jsonObject.getAsJsonArray("products") }.getOrNull()
            ?: throw AdaptyError(
                message = "products in Paywall should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        products.forEachIndexed { index, jsonElement ->
            (jsonElement as? JsonObject)?.let { product ->
                if (!product.has("paywall_product_index"))
                    product.addProperty("paywall_product_index", index)
            }
        }

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
        const val metaKey = "meta"
        const val responseCreatedAtKey = "response_created_at"
    }
}