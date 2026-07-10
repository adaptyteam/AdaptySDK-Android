package com.adapty.internal.crossplatform

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class IdentifyArgsTypeAdapterFactory :
    BaseTypeAdapterFactory<IdentifyArgs>(IdentifyArgs::class.java) {

    override fun write(
        out: JsonWriter,
        value: IdentifyArgs,
        delegateAdapter: TypeAdapter<IdentifyArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        delegateAdapter.write(out, value)
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<IdentifyArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ): IdentifyArgs? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        jsonObject.removeNode("parameters").let { (_, value) ->
            if (value is JsonObject) {
                val obfuscatedAccountIdNode = value.removeNode("obfuscated_account_id")
                jsonObject.addNodeIfNotEmpty(obfuscatedAccountIdNode.copy(first = "gp_obfuscated_account_id"))
            }
        }

        return delegateAdapter.fromJsonTree(jsonObject)
    }
}