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
    private val createTypeAdapter: (typeToken: TypeToken<TYPE>, gson: Gson, typeAdapterFactory: AdaptyResponseTypeAdapterFactory<TYPE>) -> TypeAdapter<*>,
) : TypeAdapterFactory {

    constructor(
        typeToken: TypeToken<TYPE>,
        responseDataExtractor: ResponseDataExtractor,
    ): this(typeToken, { typeToken, gson, typeAdapterFactory ->
        val delegateAdapter =
            gson.getDelegateAdapter(typeAdapterFactory, typeToken)

        val elementAdapter = gson.getAdapter(JsonElement::class.java)

        val result = object : TypeAdapter<TYPE>() {

            override fun write(out: JsonWriter, value: TYPE) {
                typeAdapterFactory.write(out, value, delegateAdapter)
            }

            override fun read(`in`: JsonReader): TYPE? {
                return typeAdapterFactory.read(`in`, delegateAdapter, elementAdapter, responseDataExtractor)
            }
        }.nullSafe()

        result
    })

    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        try {
            if (!typeToken.isAssignableFrom(type.type)) {
                return null
            }
        } catch (t: Throwable) { return null }

        return createTypeAdapter(typeToken, gson, this) as TypeAdapter<T>
    }

    fun write(
        out: JsonWriter,
        value: TYPE,
        delegateAdapter: TypeAdapter<TYPE>,
    ) {
        delegateAdapter.write(out, value)
    }

    fun read(
        `in`: JsonReader,
        delegateAdapter: TypeAdapter<TYPE>,
        elementAdapter: TypeAdapter<JsonElement>,
        responseDataExtractor: ResponseDataExtractor,
    ): TYPE? {
        val jsonElement = elementAdapter.read(`in`)
        return delegateAdapter.fromJsonTree(
            responseDataExtractor.extract(jsonElement) ?: jsonElement
        )
    }
}