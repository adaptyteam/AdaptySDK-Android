package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.google.gson.Gson

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface ResponseBodyConverter {
    fun <T> convertSuccess(response: String, classOfT: Class<T>): Response.Success<T>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultResponseBodyConverter(private val gson: Gson) : ResponseBodyConverter {
    override fun <T> convertSuccess(
        response: String,
        classOfT: Class<T>
    ) = Response.Success(gson.fromJson(response, classOfT))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class KinesisResponseBodyConverter(private val gson: Gson) : ResponseBodyConverter {
    override fun <T> convertSuccess(
        response: String,
        classOfT: Class<T>
    ) = Response.Success(gson.fromJson("", classOfT))
}