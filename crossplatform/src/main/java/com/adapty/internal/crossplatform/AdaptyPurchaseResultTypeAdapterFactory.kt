package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.models.AdaptyPurchaseResult.*
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyPurchaseResultTypeAdapterFactory : TypeAdapterFactory {

    companion object {
        const val TYPE = "type"
        const val PROFILE = "profile"
        const val PURCHASE_TOKEN = "google_purchase_token"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyPurchaseResult::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val profileAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyProfile::class.java))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyPurchaseResult>() {

            override fun write(out: JsonWriter, value: AdaptyPurchaseResult) {
                val jsonObject = JsonObject().apply {
                    when (value) {
                        is Success -> {
                            add(PROFILE, profileAdapter.toJsonTree(value.profile).asJsonObject)
                            addProperty(PURCHASE_TOKEN, value.purchase?.purchaseToken)
                            addProperty(TYPE, "success")
                        }

                        is UserCanceled -> {
                            addProperty(TYPE, "user_cancelled")
                        }

                        is Pending -> {
                            addProperty(TYPE, "pending")
                        }
                    }
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): AdaptyPurchaseResult? {
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}