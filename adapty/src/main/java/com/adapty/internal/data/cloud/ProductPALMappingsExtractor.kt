package com.adapty.internal.data.cloud

import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal class ProductPALMappingsExtractor: ResponseDataExtractor {

    override fun extract(jsonElement: JsonElement): JsonElement? {
        val jsonObject = runCatching { jsonElement.asJsonObject }.getOrNull() ?: return null

        if (jsonObject.has(itemsKey))
            return jsonObject

        val dataArray = runCatching { jsonObject.getAsJsonArray(dataKey) }.getOrNull()
            ?: return null

        val items = JsonObject()

        dataArray.forEach {
            runCatching {
                val jsonObject = it.asJsonObject
                val vendorProductId = jsonObject.getAsJsonPrimitive(vendorProductIdKey)
                val productType = jsonObject.getAsJsonPrimitive(productTypeKey)
                val accessLevelId = jsonObject.getAsJsonPrimitive(accessLevelIdKey)

                items.add(
                    vendorProductId.asString,
                    JsonObject().apply {
                        add(productTypeKey, productType)
                        add(accessLevelIdKey, accessLevelId)
                    }
                )
            }
        }

        if (items.isEmpty) return null

        return JsonObject().apply {
            add(itemsKey, items)
        }
    }

    private companion object {
        const val dataKey = "data"
        const val itemsKey = "items"
        const val vendorProductIdKey = "vendor_product_id"
        const val productTypeKey = "product_type"
        const val accessLevelIdKey = "access_level_id"
    }
}