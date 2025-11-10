package com.adapty.models

public class AdaptyPurchaseParameters private constructor(
    @get:JvmSynthetic internal val subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
    @get:JvmSynthetic internal val isOfferPersonalized: Boolean,
) {

    public class Builder {

        private var subscriptionUpdateParams: AdaptySubscriptionUpdateParameters? = null

        private var isOfferPersonalized = false

        /**
         * @param[subscriptionUpdateParams] An [AdaptySubscriptionUpdateParameters] object, used when
         * you need a subscription to be replaced with another one, [read more](https://adapty.io/docs/android-making-purchases#change-subscription-when-making-a-purchase).
         */
        public fun withSubscriptionUpdateParams(subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?): Builder {
            this.subscriptionUpdateParams = subscriptionUpdateParams
            return this
        }

        /**
         * @param[isOfferPersonalized] Indicates whether the price is personalized, [read more](https://developer.android.com/google/play/billing/integrate#personalized-price).
         */
        public fun withOfferPersonalized(isOfferPersonalized: Boolean): Builder {
            this.isOfferPersonalized = isOfferPersonalized
            return this
        }

        public fun build(): AdaptyPurchaseParameters {
            return AdaptyPurchaseParameters(
                subscriptionUpdateParams,
                isOfferPersonalized,
            )
        }
    }

    public companion object {
        @JvmField
        public val Empty: AdaptyPurchaseParameters = Builder().build()
    }

    override fun toString(): String {
        return "AdaptyPurchaseParameters(subscriptionUpdateParams=$subscriptionUpdateParams, isOfferPersonalized=$isOfferPersonalized)"
    }
}