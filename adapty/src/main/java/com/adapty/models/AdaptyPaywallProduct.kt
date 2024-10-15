@file:OptIn(InternalAdaptyApi::class)

package com.adapty.models

import com.adapty.internal.domain.models.BackendProduct.SubscriptionData
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct.Price
import com.android.billingclient.api.ProductDetails
import java.math.BigDecimal

/**
 * @property[localizedDescription] A description of the product.
 * @property[localizedTitle] The name of the product.
 * @property[paywallABTestName] Same as [abTestName][AdaptyPaywall.abTestName] property of the parent [AdaptyPaywall].
 * @property[paywallName] Same as [name][AdaptyPaywall.name] property of the parent [AdaptyPaywall].
 * @property[price] The [price][Price] of the product in the local currency.
 * @property[productDetails] Underlying system representation of the product.
 * @property[subscriptionDetails] Consolidates all subscription-related properties if the product is a subscription, otherwise `null`.
 * @property[variationId] Same as [variationId][AdaptyPaywall.variationId] property of the parent [AdaptyPaywall].
 * @property[vendorProductId] Unique identifier of a product from App Store Connect or Google Play Console.
 */
public class AdaptyPaywallProduct internal constructor(
    public val vendorProductId: String,
    public val localizedTitle: String,
    public val localizedDescription: String,
    public val paywallName: String,
    public val paywallABTestName: String,
    public val variationId: String,
    public val price: Price,
    public val subscriptionDetails: AdaptyProductSubscriptionDetails?,
    public val productDetails: ProductDetails,
    @get:JvmSynthetic internal val payloadData: Payload,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyPaywallProduct

        if (vendorProductId != other.vendorProductId) return false
        if (localizedTitle != other.localizedTitle) return false
        if (localizedDescription != other.localizedDescription) return false
        if (paywallName != other.paywallName) return false
        if (paywallABTestName != other.paywallABTestName) return false
        if (variationId != other.variationId) return false
        if (price != other.price) return false
        if (subscriptionDetails != other.subscriptionDetails) return false
        if (productDetails != other.productDetails) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vendorProductId.hashCode()
        result = 31 * result + localizedTitle.hashCode()
        result = 31 * result + localizedDescription.hashCode()
        result = 31 * result + paywallName.hashCode()
        result = 31 * result + paywallABTestName.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + (subscriptionDetails?.hashCode() ?: 0)
        result = 31 * result + productDetails.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyPaywallProduct(vendorProductId=$vendorProductId, localizedTitle=$localizedTitle, localizedDescription=$localizedDescription, paywallName=$paywallName, paywallABTestName=$paywallABTestName, variationId=$variationId, price=$price, subscriptionDetails=$subscriptionDetails, productDetails=$productDetails)"
    }

    /**
     * @property[amount] The cost of the product in the local currency.
     * @property[currencyCode] The currency code of the locale used to format the price of the product.
     * @property[currencySymbol] The currency symbol of the locale used to format the price of the product.
     * @property[localizedString] The formatted price from Google Play as is.
     */
    public class Price(
        public val amount: BigDecimal,
        public val localizedString: String,
        public val currencyCode: String,
        public val currencySymbol: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Price

            if (amount != other.amount) return false
            if (localizedString != other.localizedString) return false
            if (currencyCode != other.currencyCode) return false
            if (currencySymbol != other.currencySymbol) return false

            return true
        }

        override fun hashCode(): Int {
            var result = amount.hashCode()
            result = 31 * result + localizedString.hashCode()
            result = 31 * result + currencyCode.hashCode()
            result = 31 * result + currencySymbol.hashCode()
            return result
        }

        override fun toString(): String {
            return "Price(amount=$amount, localizedString=$localizedString, currencyCode=$currencyCode, currencySymbol=$currencySymbol)"
        }
    }

    internal class Payload internal constructor(@get:JvmSynthetic val priceAmountMicros: Long, @get:JvmSynthetic val currencyCode: String, @get:JvmSynthetic val type: String, @get:JvmSynthetic val subscriptionData: SubscriptionData?)
}