package com.adapty.internal.crossplatform

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

internal abstract class BaseTypeAdapterFactory<TYPE>(private val clazz: Class<TYPE>) :
    TypeAdapterFactory {

    open fun write(
        out: JsonWriter,
        value: TYPE,
        delegateAdapter: TypeAdapter<TYPE>,
        elementAdapter: TypeAdapter<JsonElement>,
    ) {
        delegateAdapter.write(out, value)
    }

    open fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<TYPE>,
        elementAdapter: TypeAdapter<JsonElement>,
    ): TYPE? {
        return delegateAdapter.read(`in`)
    }

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!clazz.isAssignableFrom(type.rawType)) {
            return null
        }

        val delegateAdapter =
            gson.getDelegateAdapter(this, TypeToken.get(clazz))

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<TYPE>() {

            override fun write(out: JsonWriter, value: TYPE) {
                write(out, value, delegateAdapter, elementAdapter)
            }

            override fun read(`in`: JsonReader): TYPE? {
                return read(`in`, delegateAdapter, elementAdapter)
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }
}