package com.adapty.ui.listeners

import com.adapty.models.AdaptyPaywallProduct

/**
 * Implement this interface to indicate whether the price is personalized, [read more](https://developer.android.com/google/play/billing/integrate#personalized-price).
 */
public fun interface AdaptyUiPersonalizedOfferResolver {
    /**
     * Function that maps a product to a boolean value that indicates whether the price is personalized, [read more](https://developer.android.com/google/play/billing/integrate#personalized-price).
     *
     * @param[product] An [AdaptyPaywallProduct] to be purchased.
     *
     * @return `true`, if the price of the [product] is personalized, otherwise `false`.
     */
    public fun resolve(product: AdaptyPaywallProduct): Boolean

    public companion object {
        /**
         * The default implementation that returns `false`.
         */
        @JvmField
        public val DEFAULT: AdaptyUiPersonalizedOfferResolver =
            AdaptyUiPersonalizedOfferResolver { _ -> false }
    }
}