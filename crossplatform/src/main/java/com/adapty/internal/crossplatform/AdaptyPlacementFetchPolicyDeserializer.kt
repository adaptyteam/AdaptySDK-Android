package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyPlacementFetchPolicy
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class AdaptyPlacementFetchPolicyDeserializer : JsonDeserializer<AdaptyPlacementFetchPolicy> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AdaptyPlacementFetchPolicy {
        val jsonObject = (json as? JsonObject) ?: return AdaptyPlacementFetchPolicy.Default
        return when (jsonObject.get("type")?.takeIf(JsonElement::isJsonPrimitive)?.asString) {
            "return_cache_data_else_load" -> AdaptyPlacementFetchPolicy.ReturnCacheDataElseLoad
            "reload_revalidating_cache_data" -> AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData
            "return_cache_data_if_not_expired_else_load" -> {
                val maxAgeMillis = jsonObject.get("max_age")?.takeIf(JsonElement::isJsonPrimitive)?.asNumber?.toDouble()?.times(1000)?.toLong()
                if (maxAgeMillis != null)
                    AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad(maxAgeMillis)
                else
                    AdaptyPlacementFetchPolicy.Default
            }
            else -> AdaptyPlacementFetchPolicy.Default
        }
    }
}