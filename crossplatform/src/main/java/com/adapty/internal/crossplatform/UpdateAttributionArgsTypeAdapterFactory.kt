package com.adapty.internal.crossplatform

import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class UpdateAttributionArgsTypeAdapterFactory :
    BaseTypeAdapterFactory<UpdateAttributionArgs>(UpdateAttributionArgs::class.java) {

    private companion object {
        const val ATTRIBUTION = "attribution"
    }

    override fun write(
        out: JsonWriter,
        value: UpdateAttributionArgs,
        delegateAdapter: TypeAdapter<UpdateAttributionArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        delegateAdapter.write(out, value)
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<UpdateAttributionArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ): UpdateAttributionArgs? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        val attributionJson = kotlin.runCatching {
            jsonObject.remove(ATTRIBUTION)?.asJsonPrimitive?.asString
        }.getOrNull() ?: return null
        val attribution = elementAdapter.fromJson(attributionJson)?.takeIf { it.isJsonObject }
            ?: return null
        jsonObject.add(ATTRIBUTION, attribution)

        return delegateAdapter.fromJsonTree(jsonObject)
    }
}