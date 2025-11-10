package com.adapty.internal.data.cloud

import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal class ProductPALMappingsExtractor: ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = runCatching { jsonElement.asJsonObject }.getOrNull() ?: return null

        if (jsonObject.has("items"))
            return jsonObject

        val dataArray = runCatching { jsonObject.getAsJsonArray("data") }.getOrNull()
            ?: return null

        val items = JsonObject()

        dataArray.forEach {
            runCatching {
                val jsonObject = it.asJsonObject
                val vendorProductId = jsonObject.getAsJsonPrimitive("vendor_product_id")
                val productType = jsonObject.getAsJsonPrimitive("product_type")
                val accessLevelId = jsonObject.getAsJsonPrimitive("access_level_id")

                items.add(
                    vendorProductId.asString,
                    JsonObject().apply {
                        add("product_type", productType)
                        add("access_level_id", accessLevelId)
                    }
                )
            }
        }

        if (items.isEmpty) return null

        return JsonObject().apply {
            add("items", items)
        }
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