package com.adapty.errors

import com.adapty.internal.data.models.BackendError
import java.io.IOException

public class AdaptyError internal constructor(
    public val originalError: Throwable? = null,
    message: String = "",
    public val adaptyErrorCode: AdaptyErrorCode = AdaptyErrorCode.UNKNOWN,
    @get:JvmSynthetic internal val backendError: BackendError? = null,
) : Exception(message, originalError) {

    @JvmSynthetic
    internal fun getRetryType(isInfinite: Boolean) = when {
        adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR -> if (isInfinite) RetryType.PROGRESSIVE else RetryType.SIMPLE
        originalError is IOException -> RetryType.SIMPLE
        else -> RetryType.NONE
    }

    internal enum class RetryType {
        @JvmSynthetic
        SIMPLE,

        @JvmSynthetic
        PROGRESSIVE,

        @JvmSynthetic
        NONE
    }
}