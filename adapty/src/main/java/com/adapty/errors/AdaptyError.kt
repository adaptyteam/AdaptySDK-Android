package com.adapty.errors

import java.io.IOException

public class AdaptyError internal constructor(
    public val originalError: Throwable? = null,
    message: String = "",
    public val adaptyErrorCode: AdaptyErrorCode = AdaptyErrorCode.UNKNOWN
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