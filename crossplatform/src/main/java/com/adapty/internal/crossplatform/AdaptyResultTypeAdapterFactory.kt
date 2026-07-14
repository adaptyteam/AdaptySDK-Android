package com.adapty.internal.crossplatform

import com.adapty.utils.AdaptyResult
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyResultTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyResult::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val successAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyResult.Success::class.java))

        val errorAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyResult.Error::class.java))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyResult<*>>() {

            override fun write(out: JsonWriter, value: AdaptyResult<*>) {
                val jsonObject = when (value) {
                    is AdaptyResult.Success<*> -> successAdapter.toJsonTree(value).asJsonObject.apply {
                        val data = remove("value")
                        add("success", data)

                        if (value.value == null)
                            out.serializeNulls = true
                    }
                    is AdaptyResult.Error -> errorAdapter.toJsonTree(value).asJsonObject
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): AdaptyResult<*>? {
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}