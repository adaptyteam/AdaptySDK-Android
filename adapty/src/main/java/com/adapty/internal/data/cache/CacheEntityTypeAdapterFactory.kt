package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CacheEntityTypeAdapterFactory : TypeAdapterFactory {

    private companion object {
        const val CACHED_AT = "cached_at"
        const val VALUE = "value"
        const val VERSION = "version"
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!CacheEntity::class.java.isAssignableFrom(type.rawType)) {
            return null
        }

        val delegateAdapter =
            gson.getDelegateAdapter(this, type)

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<T>() {

            override fun write(out: JsonWriter, value: T) {
                delegateAdapter.write(out, value)
            }

            override fun read(`in`: JsonReader): T? {
                val jsonObject = elementAdapter.read(`in`).asJsonObject
                val cachedAt = kotlin.runCatching { jsonObject.get(CACHED_AT)?.asLong }.getOrNull()
                val version = kotlin.runCatching { jsonObject.get(VERSION)?.asInt }.getOrNull()
                val jsonTree = if (cachedAt != null) {
                    if (version == null)
                        jsonObject.addProperty(VERSION, 1)
                    jsonObject
                } else {
                    JsonObject().apply {
                        add(VALUE, jsonObject)
                        addProperty(CACHED_AT, 0L)
                        addProperty(VERSION, 1)
                    }
                }
                return delegateAdapter.fromJsonTree(jsonTree)
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}