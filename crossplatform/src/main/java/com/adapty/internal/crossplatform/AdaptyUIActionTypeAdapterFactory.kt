package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.AdaptyUI
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyUIActionTypeAdapterFactory : TypeAdapterFactory {

    private companion object {
        const val TYPE = "type"
        const val TYPE_CLOSE = "close"
        const val TYPE_OPEN_URL = "open_url"
        const val TYPE_CUSTOM = "custom"
        const val VALUE = "value"
        const val OPEN_IN = "open_in"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!AdaptyUI.Action::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val elementAdapter = gson.getAdapter(JsonElement::class.java)
        val webPresentationAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(AdaptyWebPresentation::class.java))

        val result = object : TypeAdapter<AdaptyUI.Action>() {

            override fun write(out: JsonWriter, value: AdaptyUI.Action) {
                val jsonObject = JsonObject().apply {
                    when (value) {
                        AdaptyUI.Action.Close -> addProperty(TYPE, TYPE_CLOSE)
                        is AdaptyUI.Action.OpenUrl -> {
                            addProperty(TYPE, TYPE_OPEN_URL)
                            addProperty(VALUE, value.url)
                            add(OPEN_IN, webPresentationAdapter.toJsonTree(value.presentation))
                        }
                        is AdaptyUI.Action.Custom -> {
                            addProperty(TYPE, TYPE_CUSTOM)
                            addProperty(VALUE, value.customId)
                        }
                    }
                }
                elementAdapter.write(out, jsonObject)
            }

            override fun read(`in`: JsonReader): AdaptyUI.Action? {
                return null
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}