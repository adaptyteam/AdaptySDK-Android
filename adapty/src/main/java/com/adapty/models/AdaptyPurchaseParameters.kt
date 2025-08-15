package com.adapty.models

public class AdaptyPurchaseParameters private constructor(
    @get:JvmSynthetic internal val subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
    @get:JvmSynthetic internal val isOfferPersonalized: Boolean,
    @get:JvmSynthetic internal val obfuscatedAccountId: String?,
    @get:JvmSynthetic internal val obfuscatedProfileId: String?,
) {

    public class Builder {

        private var subscriptionUpdateParams: AdaptySubscriptionUpdateParameters? = null

        private var isOfferPersonalized = false

        private var obfuscatedAccountId: String? = null

        private var obfuscatedProfileId: String? = null

        /**
         * @param[subscriptionUpdateParams] An [AdaptySubscriptionUpdateParameters] object, used when
         * you need a subscription to be replaced with another one, [read more](https://adapty.io/docs/making-purchases#change-subscription-when-making-a-purchase).
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

        /**
         * @param[obfuscatedAccountId] The obfuscated account identifier, [read more](https://developer.android.com/google/play/billing/developer-payload#attribute).
         */
        public fun withObfuscatedAccountId(obfuscatedAccountId: String?): Builder {
            this.obfuscatedAccountId = obfuscatedAccountId
            return this
        }

        /**
         * @param[obfuscatedProfileId] The obfuscated profile identifier, [read more](https://developer.android.com/google/play/billing/developer-payload#attribute).
         */
        public fun withObfuscatedProfileId(obfuscatedProfileId: String?): Builder {
            this.obfuscatedProfileId = obfuscatedProfileId
            return this
        }

        public fun build(): AdaptyPurchaseParameters {
            return AdaptyPurchaseParameters(
                subscriptionUpdateParams,
                isOfferPersonalized,
                obfuscatedAccountId,
                obfuscatedProfileId,
            )
        }
    }

    public companion object {
        @JvmField
        public val Empty: AdaptyPurchaseParameters = Builder().build()
    }

    override fun toString(): String {
        return "AdaptyPurchaseParameters(subscriptionUpdateParams=$subscriptionUpdateParams, isOfferPersonalized=$isOfferPersonalized, obfuscatedAccountId=$obfuscatedAccountId, obfuscatedProfileId=$obfuscatedProfileId)"
    }
}