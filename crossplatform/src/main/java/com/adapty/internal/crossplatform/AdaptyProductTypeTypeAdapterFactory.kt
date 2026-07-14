@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.crossplatform

import com.adapty.internal.domain.models.BackendProduct.SubscriptionData
import com.adapty.internal.domain.models.ProductType
import com.adapty.internal.utils.InternalAdaptyApi
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyProductTypeTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!ProductType::class.java.isAssignableFrom(type.rawType))
            return null

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<ProductType>() {
            override fun write(out: JsonWriter, value: ProductType) {
                val jsonObject = JsonObject()
                if (value is ProductType.Subscription) {
                    val subData = value.subscriptionData
                    jsonObject.addProperty(BASE_PLAN_ID, subData.basePlanId)
                    subData.offerId?.let { offerId ->
                        jsonObject.addProperty(OFFER_ID, offerId)
                    }
                }
                jsonObject.addProperty(IS_CONSUMABLE, value is ProductType.Consumable)

                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): ProductType {
                val jsonObject = elementAdapter.read(`in`).asJsonObject

                val basePlanIdElement = jsonObject.remove(BASE_PLAN_ID)
                val basePlanId = (basePlanIdElement as? JsonPrimitive)?.getAsString()

                val offerIdElement = jsonObject.remove(OFFER_ID)
                val offerId = (offerIdElement as? JsonPrimitive)?.getAsString()

                val isConsumableElement = jsonObject.remove(IS_CONSUMABLE)
                val isConsumable = (isConsumableElement as? JsonPrimitive)?.getAsBoolean() ?: false

                val subData = basePlanId?.let { SubscriptionData(basePlanId, offerId) }
                if (subData != null)
                    return ProductType.Subscription(subData)

                if (isConsumable)
                    return ProductType.Consumable

                return ProductType.NonConsumable
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }

    companion object {
        private const val IS_CONSUMABLE = "is_consumable"
        private const val BASE_PLAN_ID = "base_plan_id"
        private const val OFFER_ID = "offer_id"
    }
}
