package com.adapty.internal.crossplatform

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyErrorTypeAdapterFactory : TypeAdapterFactory {

    private companion object {
        const val ADAPTY_CODE = "adapty_code"
        const val MESSAGE = "message"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyError::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val errorCodeAdapter = gson.getAdapter(AdaptyErrorCode::class.java)
        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyError>() {

            override fun write(out: JsonWriter, value: AdaptyError) {
                val jsonObject = JsonObject().apply {
                    add(ADAPTY_CODE, errorCodeAdapter.toJsonTree(value.adaptyErrorCode))
                    addProperty(MESSAGE, value.message.orEmpty())
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): AdaptyError? {
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}