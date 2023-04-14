package com.adapty.models

import com.android.billingclient.api.SkuDetails
import java.math.BigDecimal

/**
 * @property[currencyCode] The currency code of the locale used to format the price of the product.
 * @property[currencySymbol] The currency symbol of the locale used to format the price of the product.
 * @property[freeTrialPeriod] The object containing free trial information for the product.
 * @property[introductoryDiscount] The object containing introductory price information for the product.
 * @property[introductoryOfferEligibility] User’s eligibility for your introductory offer. Check this property before displaying info about introductory offers.
 * @property[localizedDescription] A description of the product.
 * @property[localizedFreeTrialPeriod] The period’s language is determined by the preferred language set on the device.
 * @property[localizedPrice] The formatted price from Google Play as is.
 * @property[localizedSubscriptionPeriod] The period’s language is determined by the preferred language set on the device.
 * @property[localizedTitle] The name of the product.
 * @property[paywallABTestName] Same as [abTestName][AdaptyPaywall.abTestName] property of the parent [AdaptyPaywall].
 * @property[paywallName] Same as [name][AdaptyPaywall.name] property of the parent [AdaptyPaywall].
 * @property[price] The cost of the product in the local currency.
 * @property[skuDetails] Underlying system representation of the product.
 * @property[subscriptionPeriod] The period details for products that are subscriptions.
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
    public val price: BigDecimal,
    public val localizedPrice: String,
    public val currencyCode: String,
    public val currencySymbol: String,
    public val subscriptionPeriod: AdaptyProductSubscriptionPeriod?,
    public val localizedSubscriptionPeriod: String?,
    public val introductoryOfferEligibility: AdaptyEligibility,
    public val introductoryDiscount: AdaptyProductDiscount?,
    public val freeTrialPeriod: AdaptyProductSubscriptionPeriod?,
    public val localizedFreeTrialPeriod: String?,
    public val skuDetails: SkuDetails,
    private val timestamp: Long,
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
        if (localizedPrice != other.localizedPrice) return false
        if (currencyCode != other.currencyCode) return false
        if (currencySymbol != other.currencySymbol) return false
        if (subscriptionPeriod != other.subscriptionPeriod) return false
        if (introductoryOfferEligibility != other.introductoryOfferEligibility) return false
        if (introductoryDiscount != other.introductoryDiscount) return false
        if (freeTrialPeriod != other.freeTrialPeriod) return false
        if (skuDetails != other.skuDetails) return false

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
        result = 31 * result + localizedPrice.hashCode()
        result = 31 * result + currencyCode.hashCode()
        result = 31 * result + currencySymbol.hashCode()
        result = 31 * result + (subscriptionPeriod?.hashCode() ?: 0)
        result = 31 * result + introductoryOfferEligibility.hashCode()
        result = 31 * result + (introductoryDiscount?.hashCode() ?: 0)
        result = 31 * result + (freeTrialPeriod?.hashCode() ?: 0)
        result = 31 * result + skuDetails.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyPaywallProduct(vendorProductId=$vendorProductId, localizedTitle=$localizedTitle, localizedDescription=$localizedDescription, paywallName=$paywallName, paywallABTestName=$paywallABTestName, variationId=$variationId, price=$price, localizedPrice=$localizedPrice, currencyCode=$currencyCode, currencySymbol=$currencySymbol, subscriptionPeriod=$subscriptionPeriod, localizedSubscriptionPeriod=$localizedSubscriptionPeriod, introductoryOfferEligibility=$introductoryOfferEligibility, introductoryDiscount=$introductoryDiscount, freeTrialPeriod=$freeTrialPeriod, localizedFreeTrialPeriod=$localizedFreeTrialPeriod, skuDetails=$skuDetails)"
    }

    internal class Payload internal constructor(@get:JvmSynthetic val priceAmountMicros: Long, @get:JvmSynthetic val type: Type)

    internal enum class Type { SUBS, CONSUMABLE, NON_CONSUMABLE }
}