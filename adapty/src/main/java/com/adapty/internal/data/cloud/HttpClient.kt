package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import java.lang.reflect.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface HttpClient {

    fun <T> newCall(request: Request, typeOfT: Type, onResponseReceived: () -> Unit = {}): Response<T>
}