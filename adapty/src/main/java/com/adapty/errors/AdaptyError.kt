package com.adapty.errors

public open class AdaptyError internal constructor(
    public val originalError: Throwable? = null,
    message: String = "",
    public val adaptyErrorCode: AdaptyErrorCode = AdaptyErrorCode.UNKNOWN,
) : Exception(message, originalError)
