@file:OptIn(com.adapty.internal.utils.InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.platform

// Core SDK names used by shared renderer code: actuals are the core SDK types.

public actual typealias AdaptyPaywallProduct = com.adapty.models.AdaptyPaywallProduct
public actual typealias AdaptyProductSubscriptionDetails = com.adapty.models.AdaptyProductSubscriptionDetails
public actual typealias AdaptyProductSubscriptionPeriod = com.adapty.models.AdaptyProductSubscriptionPeriod
public actual typealias ImmutableList<T> = com.adapty.utils.ImmutableList<T>
public actual typealias AdaptyError = com.adapty.errors.AdaptyError
public actual typealias AdaptyErrorCode = com.adapty.errors.AdaptyErrorCode
public actual typealias AdaptyFlow = com.adapty.models.AdaptyFlow
public actual typealias AdaptyPlacement = com.adapty.models.AdaptyPlacement
public actual typealias AdaptyProfile = com.adapty.models.AdaptyProfile
public actual typealias AdaptyPurchaseResult = com.adapty.models.AdaptyPurchaseResult
public actual typealias AdaptyPurchaseParameters = com.adapty.models.AdaptyPurchaseParameters
public actual typealias AdaptyWebPresentation = com.adapty.models.AdaptyWebPresentation
public actual typealias AdaptyPeriodUnit = com.adapty.models.AdaptyPeriodUnit
public actual typealias AdaptyProductDiscountPhase = com.adapty.models.AdaptyProductDiscountPhase
public actual typealias PaymentMode = com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
public actual typealias AdaptyEligibility = com.adapty.models.AdaptyEligibility
public actual typealias AdaptyLogLevel = com.adapty.utils.AdaptyLogLevel
public typealias AdaptyResult<T> = com.adapty.utils.AdaptyResult<T>
public actual typealias TimeInterval = com.adapty.utils.TimeInterval
public actual typealias FileLocation = com.adapty.utils.FileLocation
public actual typealias ResultCallback<T> = com.adapty.utils.ResultCallback<T>
public typealias ErrorCallback = com.adapty.utils.ErrorCallback
public actual typealias AdaptyOnboarding = com.adapty.models.AdaptyOnboarding

// Core-internal FileLocation subclasses (Android-only)
internal typealias FileLocationAsset = com.adapty.utils.FileLocation.Asset
internal typealias FileLocationUri = com.adapty.utils.FileLocation.Uri

internal actual fun FileLocation.assetRelativePathOrNull(): String? =
    if (this is FileLocationAsset) relativePath else null

internal actual fun FileLocation.requireUri(): Uri = (this as FileLocationUri).uri

internal actual val VERBOSE: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.VERBOSE
internal actual val INFO: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.INFO
internal actual val WARN: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.WARN
internal actual val ERROR: AdaptyLogLevel get() = com.adapty.utils.AdaptyLogLevel.ERROR

internal actual val Int.days: TimeInterval get() = com.adapty.utils.TimeInterval.days(this)

internal actual fun adaptyError(
    originalError: Throwable?,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyError = com.adapty.internal.utils.adaptyError(originalError, message, adaptyErrorCode)

private val logExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

internal actual fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    logExecutor.execute { com.adapty.internal.utils.log(messageLogLevel, msg) }
}

internal actual fun runOnMain(block: () -> Unit) {
    com.adapty.internal.utils.runOnMain(block)
}
