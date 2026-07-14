@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyWebPresentation
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyWebPresentationTypeAdapterFactory : TypeAdapterFactory {

    companion object {
        const val BROWSER_IN_APP = "browser_in_app"
        const val BROWSER_OUT_APP = "browser_out_app"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyWebPresentation::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<AdaptyWebPresentation>() {

            override fun write(out: JsonWriter, value: AdaptyWebPresentation) {
                val jsonValue = when (value) {
                    is AdaptyWebPresentation.InAppBrowser -> BROWSER_IN_APP
                    is AdaptyWebPresentation.ExternalBrowser -> BROWSER_OUT_APP
                }
                elementAdapter.write(out, JsonPrimitive(jsonValue))
            }

            override fun read(`in`: JsonReader): AdaptyWebPresentation? {
                val jsonElement = elementAdapter.read(`in`)
                return when (jsonElement.asString) {
                    BROWSER_IN_APP -> AdaptyWebPresentation.InAppBrowser
                    BROWSER_OUT_APP -> AdaptyWebPresentation.ExternalBrowser
                    else -> null
                }
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}
