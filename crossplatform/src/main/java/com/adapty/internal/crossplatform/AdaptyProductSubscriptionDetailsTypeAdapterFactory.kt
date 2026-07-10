package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyProductSubscriptionDetails
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyProductSubscriptionDetailsTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyProductSubscriptionDetails::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val delegateAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyProductSubscriptionDetails::class.java))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyProductSubscriptionDetails>() {

            override fun write(out: JsonWriter, value: AdaptyProductSubscriptionDetails) {
                val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject

                jsonObject.remove("introductory_offer_phases")?.let { phases ->
                    if (phases !is JsonArray || phases.isEmpty) return@let

                    val offerIdentifier = jsonObject.remove("offer_id")?.let { offerId ->
                        JsonObject().apply {
                            add("id", offerId)
                            addProperty("type", "introductory")
                        }
                    } ?: return@let

                    val offerJsonObject = JsonObject().apply {
                        add("phases", phases)
                        add("offer_identifier", offerIdentifier)
                    }
                    jsonObject.moveNodeIfExists(offerJsonObject, "offer_tags")
                    jsonObject.add("offer", offerJsonObject)
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): AdaptyProductSubscriptionDetails? {
                val jsonObject = elementAdapter.read(`in`).asJsonObject
                jsonObject.remove("offer")
                return delegateAdapter.fromJsonTree(jsonObject)
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}