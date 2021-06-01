package com.adapty.errors

import java.io.IOException

class AdaptyError internal constructor(
    val originalError: Throwable? = null,
    message: String = "",
    val adaptyErrorCode: AdaptyErrorCode = AdaptyErrorCode.UNKNOWN
) : Throwable(message, originalError) {

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