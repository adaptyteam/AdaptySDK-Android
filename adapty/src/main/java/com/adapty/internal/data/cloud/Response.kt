package com.adapty.internal.data.cloud

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.BackendError

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Response<T>(val data: T, val request: Request) {

    operator fun component1() = data
    operator fun component2() = request

    class Error(
        originalError: Throwable? = null,
        override val message: String,
        adaptyErrorCode: AdaptyErrorCode,
        val backendError: BackendError? = null,
        val request: Request,
    ): AdaptyError(originalError, message, adaptyErrorCode)
}
