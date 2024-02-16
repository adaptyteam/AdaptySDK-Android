package com.adapty.internal.utils

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.models.AdaptyPaywall
import com.adapty.utils.AdaptyLogLevel

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Adapty API, please don't rely on it."
)
public annotation class InternalAdaptyApi

@JvmSynthetic @InternalAdaptyApi
public fun adaptyError(
    originalError: Throwable?,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyError = AdaptyError(originalError, message, adaptyErrorCode)

@JvmSynthetic @InternalAdaptyApi
public fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    Logger.log(messageLogLevel, msg)
}

@get:JvmSynthetic @InternalAdaptyApi
public val adaptySdkVersion: String get() = com.adapty.BuildConfig.VERSION_NAME

@JvmSynthetic @InternalAdaptyApi
public fun getOrderedOriginalProductIds(paywall: AdaptyPaywall): List<String> =
    paywall.products.map { it.id }