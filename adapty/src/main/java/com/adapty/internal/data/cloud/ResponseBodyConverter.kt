package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.google.gson.Gson
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface ResponseBodyConverter {
    fun <T> convert(response: String, typeOfT: Type): T
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultResponseBodyConverter(private val gson: Gson) : ResponseBodyConverter {
    override fun <T> convert(
        response: String,
        typeOfT: Type,
    ) = gson.fromJson<T>(response, typeOfT)
}