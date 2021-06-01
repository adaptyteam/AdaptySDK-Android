package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class Response<out R> {
    data class Success<out T>(val body: T) : Response<T>()
    data class Error(val error: AdaptyError) : Response<Nothing>()
}