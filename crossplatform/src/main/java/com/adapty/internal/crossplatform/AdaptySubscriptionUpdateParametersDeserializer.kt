package com.adapty.internal.crossplatform

import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class AdaptySubscriptionUpdateParametersDeserializer : JsonDeserializer<AdaptySubscriptionUpdateParameters> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AdaptySubscriptionUpdateParameters? {
        return (json as? JsonObject)?.let { jsonObject ->
            val oldSubVendorProductIdStr = runCatching {
                jsonObject.getAsJsonPrimitive("old_sub_vendor_product_id").asString.split(":")[0]
            }.getOrNull()
            val replacementMode = jsonObject.get("replacement_mode")

            if (oldSubVendorProductIdStr == null || replacementMode == null) return null

            return AdaptySubscriptionUpdateParameters(
                oldSubVendorProductIdStr,
                context.deserialize(replacementMode, AdaptySubscriptionUpdateParameters.ReplacementMode::class.java)
            )
        }
    }
}