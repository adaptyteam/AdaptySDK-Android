package com.adapty.internal.crossplatform

import com.adapty.ui.AdaptyCustomAsset
import com.adapty.ui.AdaptyCustomAssets
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class AdaptyCustomAssetsDeserializer : JsonDeserializer<AdaptyCustomAssets> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AdaptyCustomAssets? {
        return (json as? JsonArray)?.let { array ->
            val assets = array.mapNotNull { element ->
                if (element !is JsonObject) return@mapNotNull null
                val assetId = kotlin.runCatching { element.getAsJsonPrimitive("id").asString }.getOrNull()
                    ?: return@mapNotNull null
                val asset = context.deserialize<AdaptyCustomAsset>(element, AdaptyCustomAsset::class.java)
                    ?: return@mapNotNull null
                assetId to asset
            }

            if (assets.isNotEmpty())
                AdaptyCustomAssets.of(assets.toMap())
            else
                AdaptyCustomAssets.Empty
        }
    }
}