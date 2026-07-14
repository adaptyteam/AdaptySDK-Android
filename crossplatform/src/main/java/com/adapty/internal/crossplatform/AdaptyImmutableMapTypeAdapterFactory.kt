package com.adapty.internal.crossplatform

import com.adapty.utils.ImmutableMap
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class AdaptyImmutableMapTypeAdapterFactory :
    BaseTypeAdapterFactory<ImmutableMap<*, *>>(ImmutableMap::class.java) {

    private companion object {
        const val MAP = "map"
    }

    override fun write(
        out: JsonWriter,
        value: ImmutableMap<*, *>,
        delegateAdapter: TypeAdapter<ImmutableMap<*, *>>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        val jsonObject = delegateAdapter.toJsonTree(value).asJsonObject
        elementAdapter.write(out, jsonObject.getAsJsonObject(MAP))
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<ImmutableMap<*, *>>,
        elementAdapter: TypeAdapter<JsonElement>
    ): ImmutableMap<*, *>? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject
        return delegateAdapter.fromJsonTree(
            JsonObject().apply { add(MAP, jsonObject) }
        )
    }
}