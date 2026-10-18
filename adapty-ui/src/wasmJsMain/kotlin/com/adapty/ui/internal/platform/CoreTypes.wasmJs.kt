package com.adapty.ui.internal.platform

// Web stand-ins for the core SDK model types: plain data holders with the declared members.

public actual class AdaptyPaywallProduct(
    public actual val vendorProductId: String,
    public actual val localizedTitle: String,
    public actual val localizedDescription: String,
    public actual val paywallName: String,
    public actual val paywallABTestName: String,
    public actual val variationId: String,
    public actual val accessLevelId: String,
    public actual val productType: String,
    public actual val price: Price,
    public actual val subscriptionDetails: AdaptyProductSubscriptionDetails?,
    internal val adaptyProductId: String,
    internal val flowProductId: String?,
) {
    public actual class Price(
        public actual val amount: BigDecimalLike,
        public actual val localizedString: String,
        public actual val currencyCode: String,
        public actual val currencySymbol: String,
    )
}

public actual class AdaptyProductSubscriptionDetails(
    public actual val basePlanId: String,
    public actual val offerId: String?,
    public actual val subscriptionPeriod: AdaptyProductSubscriptionPeriod,
    public actual val localizedSubscriptionPeriod: String,
    public actual val introductoryOfferPhases: ImmutableList<AdaptyProductDiscountPhase>,
    public actual val introductoryOfferEligibility: AdaptyEligibility,
)

public actual class ImmutableList<T>(private val list: List<T>) : Iterable<T> {
    actual override fun iterator(): Iterator<T> = list.iterator()
}

public actual class AdaptyProductSubscriptionPeriod(
    public actual val unit: AdaptyPeriodUnit,
    public actual val numberOfUnits: Int,
)

public actual class AdaptyProductDiscountPhase(
    public actual val price: AdaptyPaywallProduct.Price,
    public actual val numberOfPeriods: Int,
    public actual val paymentMode: PaymentMode,
    public actual val localizedNumberOfPeriods: String,
    public actual val subscriptionPeriod: AdaptyProductSubscriptionPeriod,
    public actual val localizedSubscriptionPeriod: String,
)

public actual enum class PaymentMode { PAY_AS_YOU_GO, PAY_UPFRONT, FREE_TRIAL, UNKNOWN }

public actual enum class AdaptyPeriodUnit { DAY, WEEK, MONTH, YEAR, UNKNOWN }

public actual enum class AdaptyEligibility { ELIGIBLE, INELIGIBLE, NOT_APPLICABLE }

public actual class AdaptyPlacement(
    public actual val id: String,
    public actual val abTestName: String,
)

public actual class AdaptyFlow(
    public actual val id: String,
    public actual val variationId: String,
    public actual val name: String,
    public actual val placement: AdaptyPlacement,
)

public actual class AdaptyOnboarding

public actual class AdaptyProfile

public actual sealed class AdaptyPurchaseResult {
    public actual class Success(public val profile: AdaptyProfile) : AdaptyPurchaseResult()
    public actual object UserCanceled : AdaptyPurchaseResult()
    public actual object Pending : AdaptyPurchaseResult()
}

public actual class AdaptyPurchaseParameters private constructor() {
    public actual companion object {
        public actual val Empty: AdaptyPurchaseParameters = AdaptyPurchaseParameters()
    }
}

public actual sealed class AdaptyWebPresentation {
    private class InApp : AdaptyWebPresentation() {
        override fun toString(): String = "InAppBrowser"
    }

    private class External : AdaptyWebPresentation() {
        override fun toString(): String = "ExternalBrowser"
    }

    public actual companion object {
        public actual val InAppBrowser: AdaptyWebPresentation = InApp()
        public actual val ExternalBrowser: AdaptyWebPresentation = External()
    }
}

public actual open class AdaptyError(
    public actual val originalError: Throwable?,
    message: String,
    public actual val adaptyErrorCode: AdaptyErrorCode,
) : Exception(message, originalError)

public actual enum class AdaptyErrorCode {
    DECODING_FAILED,
    WRONG_ASSET_TYPE,
    INVALID_ACTION_URL,
    JS_EXCEPTION,
    NAVIGATOR_NOT_FOUND,
}

public actual class AdaptyLogLevel internal constructor(private val name: String, internal val value: Int) {
    override fun toString(): String = name
}

public actual class TimeInterval internal constructor(internal val millis: Long) {
    override fun toString(): String = "TimeInterval(${millis}ms)"
}

public actual sealed class FileLocation {
    internal class Asset(val relativePath: String) : FileLocation()
    internal class UriLocation(val uri: Uri) : FileLocation()
}

public actual interface ResultCallback<T>

internal actual fun FileLocation.assetRelativePathOrNull(): String? = (this as? FileLocation.Asset)?.relativePath

internal actual fun FileLocation.requireUri(): Uri = (this as FileLocation.UriLocation).uri

private val logLevelError = AdaptyLogLevel("ERROR", 0b1)
private val logLevelWarn = AdaptyLogLevel("WARN", 0b11)
private val logLevelInfo = AdaptyLogLevel("INFO", 0b111)
private val logLevelVerbose = AdaptyLogLevel("VERBOSE", 0b1111)

internal actual val VERBOSE: AdaptyLogLevel get() = logLevelVerbose
internal actual val INFO: AdaptyLogLevel get() = logLevelInfo
internal actual val WARN: AdaptyLogLevel get() = logLevelWarn
internal actual val ERROR: AdaptyLogLevel get() = logLevelError

internal actual val Int.days: TimeInterval get() = TimeInterval(this * 86_400_000L)

internal actual fun adaptyError(
    originalError: Throwable?,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyError = AdaptyError(originalError, message, adaptyErrorCode)

/** Web log threshold (VERBOSE messages are dropped). */
internal var webLogLevel: AdaptyLogLevel = logLevelInfo

internal actual fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String) {
    if (messageLogLevel.value and webLogLevel.value != messageLogLevel.value) return
    println("[Adapty v-web] $messageLogLevel: ${msg()}")
}

internal actual fun runOnMain(block: () -> Unit) {
    block()
}
