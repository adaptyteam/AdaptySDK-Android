package com.adapty.internal.crossplatform

import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class UpdateExternalAttributionArgsTypeAdapterFactory :
    BaseTypeAdapterFactory<UpdateExternalAttributionArgs>(UpdateExternalAttributionArgs::class.java) {

    private companion object {
        const val ATTRIBUTION = "attribution"
    }

    override fun write(
        out: JsonWriter,
        value: UpdateExternalAttributionArgs,
        delegateAdapter: TypeAdapter<UpdateExternalAttributionArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        delegateAdapter.write(out, value)
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<UpdateExternalAttributionArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ): UpdateExternalAttributionArgs? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        val attributionJson = kotlin.runCatching {
            jsonObject.get(ATTRIBUTION)?.asJsonPrimitive?.asString
        }.getOrNull() ?: return null
        elementAdapter.fromJson(attributionJson)?.takeIf { it.isJsonObject } ?: return null

        return delegateAdapter.fromJsonTree(jsonObject)
    }
}