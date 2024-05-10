package com.adapty.internal.utils

import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.models.AdaptyPaywall
import com.adapty.utils.AdaptyLogLevel
import com.adapty.utils.AdaptyResult

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Adapty API, please don't rely on it."
)
public annotation class InternalAdaptyApi

/**
 * @suppress
 */
@JvmSynthetic @InternalAdaptyApi
public fun adaptyError(
    originalError: Throwable? = null,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyError = AdaptyError(originalError, message, adaptyErrorCode)

/**
 * @suppress
 */
@JvmSynthetic @InternalAdaptyApi
public fun <T> adaptyResult(
    value: T,
): AdaptyResult<T> = AdaptyResult.Success(value)

/**
 * @suppress
 */
@JvmSynthetic @InternalAdaptyApi
public fun adaptyResult(
    originalError: Throwable? = null,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyResult<Nothing> = AdaptyResult.Error(AdaptyError(originalError, message, adaptyErrorCode))

/**
 * @suppress
 */
@JvmSynthetic @InternalAdaptyApi
public fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    Logger.log(messageLogLevel, msg)
}

/**
 * @suppress
 */
@get:JvmSynthetic @InternalAdaptyApi
public val adaptySdkVersion: String get() = com.adapty.BuildConfig.VERSION_NAME

/**
 * @suppress
 */
@JvmSynthetic @InternalAdaptyApi
public fun getOrderedOriginalProductIds(paywall: AdaptyPaywall): List<String> =
    paywall.products.map { it.id }

/**
 * @suppress
 */
@JvmSynthetic @InternalAdaptyApi
public fun errorCodeFromNetwork(responseCode: Int): AdaptyErrorCode =
    AdaptyErrorCode.fromNetwork(responseCode)