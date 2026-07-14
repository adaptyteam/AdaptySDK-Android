package com.adapty.internal.crossplatform

import com.adapty.utils.ImmutableList
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class AdaptyImmutableListSerializer :
    JsonSerializer<ImmutableList<*>>, JsonDeserializer<ImmutableList<*>> {

    override fun serialize(
        src: ImmutableList<*>,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(src.map { it })
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ImmutableList<*> {
        val elementType = (typeOfT as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
            ?: Any::class.java
        val list = json.asJsonArray.map { element ->
            context.deserialize<Any?>(element, elementType)
        }
        return ImmutableList(list)
    }
}
