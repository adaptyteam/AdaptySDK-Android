package com.adapty.internal.crossplatform

import com.adapty.models.AdaptySubscriptionUpdateParameters.ReplacementMode
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

internal class AdaptyReplacementModeDeserializer : JsonDeserializer<ReplacementMode> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ReplacementMode? {
        return when (json.asString) {
            "charge_full_price" -> ReplacementMode.CHARGE_FULL_PRICE
            "without_proration" -> ReplacementMode.WITHOUT_PRORATION
            "charge_prorated_price" -> ReplacementMode.CHARGE_PRORATED_PRICE
            "with_time_proration" -> ReplacementMode.WITH_TIME_PRORATION
            "deferred" -> ReplacementMode.DEFERRED
            else -> null
        }
    }
}