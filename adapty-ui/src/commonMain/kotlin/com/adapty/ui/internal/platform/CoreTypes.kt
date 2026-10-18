package com.adapty.ui.internal.platform

// Core SDK model types used by the shared renderer. Android: typealiases to the core SDK; web: plain holders.

public expect class AdaptyPaywallProduct {
    public val vendorProductId: String
    public val localizedTitle: String
    public val localizedDescription: String
    public val paywallName: String
    public val paywallABTestName: String
    public val variationId: String
    public val accessLevelId: String
    public val productType: String
    public val price: Price
    public val subscriptionDetails: AdaptyProductSubscriptionDetails?

    public class Price {
        public val amount: BigDecimalLike
        public val localizedString: String
        public val currencyCode: String
        public val currencySymbol: String
    }
}

public expect class AdaptyProductSubscriptionDetails {
    public val basePlanId: String
    public val offerId: String?
    public val subscriptionPeriod: AdaptyProductSubscriptionPeriod
    public val localizedSubscriptionPeriod: String
    public val introductoryOfferPhases: ImmutableList<AdaptyProductDiscountPhase>
    public val introductoryOfferEligibility: AdaptyEligibility
}

public expect class ImmutableList<T> : Iterable<T> {
    override fun iterator(): Iterator<T>
}

public expect class AdaptyProductSubscriptionPeriod {
    public val unit: AdaptyPeriodUnit
    public val numberOfUnits: Int
}

public expect class AdaptyProductDiscountPhase {
    public val price: AdaptyPaywallProduct.Price
    public val numberOfPeriods: Int
    public val paymentMode: PaymentMode
    public val localizedNumberOfPeriods: String
    public val subscriptionPeriod: AdaptyProductSubscriptionPeriod
    public val localizedSubscriptionPeriod: String
}

/** Android: AdaptyProductDiscountPhase.PaymentMode. */
public expect enum class PaymentMode { PAY_AS_YOU_GO, PAY_UPFRONT, FREE_TRIAL, UNKNOWN }

public expect enum class AdaptyPeriodUnit { DAY, WEEK, MONTH, YEAR, UNKNOWN }

public expect enum class AdaptyEligibility { ELIGIBLE, INELIGIBLE, NOT_APPLICABLE }

public expect class AdaptyPlacement {
    public val id: String
    public val abTestName: String
}

public expect class AdaptyFlow {
    public val id: String
    public val variationId: String
    public val name: String
    public val placement: AdaptyPlacement
}

public expect class AdaptyOnboarding

public expect class AdaptyProfile

public expect sealed class AdaptyPurchaseResult {
    public class Success : AdaptyPurchaseResult
    public object UserCanceled : AdaptyPurchaseResult
    public object Pending : AdaptyPurchaseResult
}

public expect class AdaptyPurchaseParameters {
    public companion object {
        public val Empty: AdaptyPurchaseParameters
    }
}

public expect sealed class AdaptyWebPresentation {
    public companion object {
        public val InAppBrowser: AdaptyWebPresentation
        public val ExternalBrowser: AdaptyWebPresentation
    }
}

public expect open class AdaptyError : Exception {
    public val originalError: Throwable?
    public val adaptyErrorCode: AdaptyErrorCode
}

public expect enum class AdaptyErrorCode {
    DECODING_FAILED,
    WRONG_ASSET_TYPE,
    INVALID_ACTION_URL,
    JS_EXCEPTION,
    NAVIGATOR_NOT_FOUND,
}

public expect class AdaptyLogLevel

public expect class TimeInterval

public expect sealed class FileLocation

public expect interface ResultCallback<T>

// Nested core classes (not reachable through a typealias qualifier)
internal typealias AdaptyPaywallProductPrice = AdaptyPaywallProduct.Price
internal typealias AdaptyPurchaseResultSuccess = AdaptyPurchaseResult.Success
internal typealias AdaptyPurchaseResultUserCanceled = AdaptyPurchaseResult.UserCanceled
internal typealias AdaptyPurchaseResultPending = AdaptyPurchaseResult.Pending

/** Relative path of an asset FileLocation, null for a uri one. */
internal expect fun FileLocation.assetRelativePathOrNull(): String?

/** Uri of a uri FileLocation (call only when [assetRelativePathOrNull] is null). */
internal expect fun FileLocation.requireUri(): Uri

internal expect val VERBOSE: AdaptyLogLevel
internal expect val INFO: AdaptyLogLevel
internal expect val WARN: AdaptyLogLevel
internal expect val ERROR: AdaptyLogLevel

internal expect val Int.days: TimeInterval

internal expect fun adaptyError(
    originalError: Throwable? = null,
    message: String,
    adaptyErrorCode: AdaptyErrorCode,
): AdaptyError

internal expect fun log(messageLogLevel: AdaptyLogLevel, msg: () -> String)

internal inline fun <reified T> Map<*, *>.getAs(key: String): T? = this[key] as? T

internal expect fun runOnMain(block: () -> Unit)
