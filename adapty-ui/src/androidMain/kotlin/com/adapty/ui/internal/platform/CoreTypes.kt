@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.platform

// Core SDK names used by shared renderer code (become expect/actual in Task 6c).

public typealias AdaptyPaywallProduct = com.adapty.models.AdaptyPaywallProduct
public typealias AdaptyError = com.adapty.errors.AdaptyError
public typealias AdaptyErrorCode = com.adapty.errors.AdaptyErrorCode
public typealias AdaptyFlow = com.adapty.models.AdaptyFlow
public typealias AdaptyProfile = com.adapty.models.AdaptyProfile
public typealias AdaptyPurchaseResult = com.adapty.models.AdaptyPurchaseResult
public typealias AdaptyPurchaseParameters = com.adapty.models.AdaptyPurchaseParameters
public typealias AdaptyWebPresentation = com.adapty.models.AdaptyWebPresentation
public typealias AdaptyPeriodUnit = com.adapty.models.AdaptyPeriodUnit
public typealias AdaptyProductDiscountPhase = com.adapty.models.AdaptyProductDiscountPhase
public typealias PaymentMode = com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
public typealias AdaptyEligibility = com.adapty.models.AdaptyEligibility
public typealias AdaptyLogLevel = com.adapty.utils.AdaptyLogLevel
public typealias AdaptyResult<T> = com.adapty.utils.AdaptyResult<T>
public typealias TimeInterval = com.adapty.utils.TimeInterval
public typealias FileLocation = com.adapty.utils.FileLocation
public typealias ResultCallback<T> = com.adapty.utils.ResultCallback<T>
public typealias ErrorCallback = com.adapty.utils.ErrorCallback
public typealias AdaptyOnboarding = com.adapty.models.AdaptyOnboarding

// Nested core classes (not reachable through a typealias qualifier)
internal typealias AdaptyPaywallProductPrice = com.adapty.models.AdaptyPaywallProduct.Price
internal typealias AdaptyPurchaseResultSuccess = com.adapty.models.AdaptyPurchaseResult.Success
internal typealias AdaptyPurchaseResultUserCanceled = com.adapty.models.AdaptyPurchaseResult.UserCanceled
internal typealias AdaptyPurchaseResultPending = com.adapty.models.AdaptyPurchaseResult.Pending
internal typealias FileLocationAsset = com.adapty.utils.FileLocation.Asset
internal typealias FileLocationUri = com.adapty.utils.FileLocation.Uri

internal val VERBOSE: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.VERBOSE
internal val INFO: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.INFO
internal val WARN: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.WARN
internal val ERROR: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.ERROR

internal val Int.days: TimeInterval get() = com.adapty.utils.TimeInterval.days(this)

internal fun adaptyError(
    originalError: Throwable? = null,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyError = com.adapty.internal.utils.adaptyError(originalError, message, adaptyErrorCode)

private val logExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

internal fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}

internal inline fun <reified T> Map<*, *>.getAs(key: String): T? = this[key] as? T

internal fun runOnMain(block: () -> Unit) {
    com.adapty.internal.utils.runOnMain(block)
}
