package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AdaptyResponseTypeAdapterFactory<TYPE>(
    private val typeToken: TypeToken<TYPE>,
    private val responseDataExtractor: ResponseDataExtractor,
) : TypeAdapterFactory {

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        try {
            if (!typeToken.isAssignableFrom(type.type)) {
                return null
            }
        } catch (t: Throwable) { return null }

        val delegateAdapter =
            gson.getDelegateAdapter(this, typeToken)

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<TYPE>() {

            override fun write(out: JsonWriter, value: TYPE) {
                write(out, value, delegateAdapter)
            }

            override fun read(`in`: JsonReader): TYPE? {
                return read(`in`, delegateAdapter, elementAdapter)
            }
        }.nullSafe()

        return result as TypeAdapter<T>
    }

    private fun write(
        out: JsonWriter,
        value: TYPE,
        delegateAdapter: TypeAdapter<TYPE>,
    ) {
        delegateAdapter.write(out, value)
    }

    private fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<TYPE>,
        elementAdapter: TypeAdapter<JsonElement>,
    ): TYPE? {
        val jsonElement = elementAdapter.read(`in`)
        return delegateAdapter.fromJsonTree(
            responseDataExtractor.extract(jsonElement) ?: jsonElement
        )
    }
}