package com.adapty.internal.crossplatform

import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal class SetIntegrationIdArgsTypeAdapterFactory :
    BaseTypeAdapterFactory<SetIntegrationIdArgs>(SetIntegrationIdArgs::class.java) {

    private companion object {
        const val KEY_VALUES = "key_values"
        const val KEY = "key"
        const val VALUE = "value"
    }

    override fun write(
        out: JsonWriter,
        value: SetIntegrationIdArgs,
        delegateAdapter: TypeAdapter<SetIntegrationIdArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ) {
        delegateAdapter.write(out, value)
    }

    override fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<SetIntegrationIdArgs>,
        elementAdapter: TypeAdapter<JsonElement>
    ): SetIntegrationIdArgs? {
        val jsonObject = elementAdapter.read(`in`).asJsonObject

        val keyValuesJson = kotlin.runCatching {
            jsonObject.remove(KEY_VALUES)?.asJsonObject
        }.getOrNull() ?: return null

        keyValuesJson.entrySet().firstOrNull()?.let { entry ->
            val key = entry.key ?: return null
            val value = kotlin.runCatching { entry.value.asJsonPrimitive.asString }.getOrNull() ?: return null
            jsonObject.addProperty(KEY, key)
            jsonObject.addProperty(VALUE, value)
        }

        return delegateAdapter.fromJsonTree(jsonObject)
    }
}