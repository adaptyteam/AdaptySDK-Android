package com.adapty.internal.data.cloud

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.SingleVariationExtractHelper
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

internal class SingleVariationExtractor(
    private val singleVariationExtractHelper: SingleVariationExtractHelper,
): ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = jsonElement.asJsonObject

        if (!jsonObject.has(dataKey) || !jsonObject.has(metaKey)) {
            singleVariationExtractHelper.extractPlacementForCompatIfMissing(jsonObject)
            return extractInternal(jsonObject)
        }

        val meta = (jsonElement as? JsonObject)?.get(metaKey) as? JsonObject
        val snapshotAt = (meta?.get(responseCreatedAtKey) as? JsonPrimitive) ?: JsonPrimitive(0)
        val placement = meta?.get(placementKey) as? JsonObject
            ?: throw AdaptyError(
                message = "placement in meta should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        val data = jsonObject.getAsJsonObject(dataKey).getAsJsonObject(attributesKey)
        singleVariationExtractHelper.addSnapshotAtIfMissing(data, snapshotAt)
        singleVariationExtractHelper.addPlacementIfMissing(data, placement)

        return extractInternal(data)
    }

    private fun extractInternal(jsonObject: JsonObject): JsonElement? {
        val placement = runCatching { jsonObject.getAsJsonObject(placementKey) }.getOrNull()
            ?: throw AdaptyError(
                message = "placement in Paywall should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        placement.requires("developer_id") { "id in Placement should not be null" }
        placement.requires("placement_audience_version_id") { "audienceVersionId in Placement should not be null" }
        placement.requires("audience_name") { "audienceName in Placement should not be null" }
        placement.requires("revision") { "revision in Placement should not be null" }
        placement.requires("ab_test_name") { "abTestName in Placement should not be null" }
        jsonObject.requires("variation_id") { "variationId in Paywall should not be null" }

        if (jsonObject.has("onboarding_id")) {
            jsonObject.requires("onboarding_name") { "name in Onboarding should not be null" }
            placement.requires("is_tracking_purchases") { "isTrackingPurchases in Placement should not be null" }
            val onboardingBuilder = runCatching { jsonObject.getAsJsonObject(onboardingBuilderKey) }.getOrNull()
                ?: throw AdaptyError(
                    message = "placement in Paywall should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
                )
            onboardingBuilder.requires("config_url") { "configUrl in OnboardingBuilder should not be null" }
            onboardingBuilder.requires("lang") { "lang in OnboardingBuilder should not be null" }
        } else {
            jsonObject.requires("paywall_id") { "paywallId in Paywall should not be null" }
            jsonObject.requires("paywall_name") { "name in Paywall should not be null" }

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
        }

        val weight = runCatching { jsonObject.getAsJsonPrimitive("weight").asInt }.getOrNull()

        if (weight == null || weight !in 1..100)
            throw AdaptyError(
                message = "weight in Variation should be between 1 and 100. Currently, it is $weight",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )

        return jsonObject
    }

    protected inline fun JsonObject.requires(key: String, errorMessage: () -> String) {
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
        const val placementKey = "placement"
        const val onboardingBuilderKey = "onboarding_builder"
    }
}