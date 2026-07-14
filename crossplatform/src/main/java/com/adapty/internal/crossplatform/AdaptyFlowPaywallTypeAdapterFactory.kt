package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyFlowPaywall
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyFlowPaywallTypeAdapterFactory :
    BaseTypeAdapterFactory<AdaptyFlowPaywall>(AdaptyFlowPaywall::class.java) {

    private companion object {
        const val PRODUCTS = "products"
        const val BASE_PLAN_ID = "base_plan_id"
        const val OFFER_ID = "offer_id"
        const val TYPE = "type"
        const val IS_CONSUMABLE = "is_consumable"
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<AdaptyFlowPaywall>,
        elementAdapter: TypeAdapter<JsonElement>
    ): AdaptyFlowPaywall? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        kotlin.runCatching {
            jsonObject.getAsJsonArray(PRODUCTS)?.forEach { element ->
                element.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let { product ->
                    val basePlanId = product.remove(BASE_PLAN_ID)?.takeIf(JsonElement::isJsonPrimitive)?.asString
                    val offerId = product.remove(OFFER_ID)?.takeIf(JsonElement::isJsonPrimitive)?.asString
                    val isConsumable = product.remove(IS_CONSUMABLE)?.takeIf(JsonElement::isJsonPrimitive)?.asBoolean

                    product.add(TYPE, JsonObject().apply {
                        addProperty(IS_CONSUMABLE, isConsumable ?: false)
                        if (basePlanId != null) {
                            addProperty(BASE_PLAN_ID, basePlanId)
                            offerId?.let { addProperty(OFFER_ID, it) }
                        }
                    })
                }
            }
        }

        return delegateAdapter.fromJsonTree(jsonObject)
    }

    override fun write(
        out: JsonWriter,
        value: AdaptyFlowPaywall,
        delegateAdapter: TypeAdapter<AdaptyFlowPaywall>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject
        jsonObject.getAsJsonArray(PRODUCTS)?.forEach { element ->
            element.takeIf(JsonElement::isJsonObject)?.asJsonObject?.let { product ->
                product.remove(TYPE)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
                    ?.let { productType ->
                        productType.get(IS_CONSUMABLE)?.takeIf(JsonElement::isJsonPrimitive)?.asBoolean?.let { isConsumable ->
                            product.addProperty(IS_CONSUMABLE, isConsumable)
                        }
                        productType.get(BASE_PLAN_ID)?.takeIf(JsonElement::isJsonPrimitive)?.asString?.let { basePlanId ->
                            product.addProperty(BASE_PLAN_ID, basePlanId)
                        }
                        productType.get(OFFER_ID)?.takeIf(JsonElement::isJsonPrimitive)?.asString?.let { offerId ->
                            product.addProperty(OFFER_ID, offerId)
                        }
                    }
            }
        }

        elementAdapter.write(out, jsonObject)
    }
}
