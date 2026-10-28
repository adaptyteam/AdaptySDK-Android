package com.adapty.internal.crossplatform

import com.adapty.models.AdaptySubscriptionBillingPlan
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

internal class AdaptySubscriptionBillingPlanSerializer : JsonSerializer<AdaptySubscriptionBillingPlan> {

    override fun serialize(
        src: AdaptySubscriptionBillingPlan,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.value)
    }
}
