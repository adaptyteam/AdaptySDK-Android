package com.adapty.models

import com.android.billingclient.api.SkuDetails
import java.math.BigDecimal

public class ProductModel(
    public val vendorProductId: String,
    public val localizedTitle: String,
    public val localizedDescription: String,
    public val paywallName: String?,
    public val paywallABTestName: String?,
    public val variationId: String?,
    public val price: BigDecimal,
    public val localizedPrice: String?,
    public val currencyCode: String?,
    public val currencySymbol: String?,
    public val subscriptionPeriod: ProductSubscriptionPeriodModel?,
    public val localizedSubscriptionPeriod: String?,
    public val introductoryOfferEligibility: Boolean,
    public val introductoryDiscount: ProductDiscountModel?,
    public val freeTrialPeriod: ProductSubscriptionPeriodModel?,
    public val localizedFreeTrialPeriod: String?,
    public val skuDetails: SkuDetails?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductModel

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
        result = 31 * result + (paywallName?.hashCode() ?: 0)
        result = 31 * result + (paywallABTestName?.hashCode() ?: 0)
        result = 31 * result + (variationId?.hashCode() ?: 0)
        result = 31 * result + price.hashCode()
        result = 31 * result + (localizedPrice?.hashCode() ?: 0)
        result = 31 * result + (currencyCode?.hashCode() ?: 0)
        result = 31 * result + (currencySymbol?.hashCode() ?: 0)
        result = 31 * result + (subscriptionPeriod?.hashCode() ?: 0)
        result = 31 * result + introductoryOfferEligibility.hashCode()
        result = 31 * result + (introductoryDiscount?.hashCode() ?: 0)
        result = 31 * result + (freeTrialPeriod?.hashCode() ?: 0)
        result = 31 * result + (skuDetails?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ProductModel(vendorProductId=$vendorProductId, localizedTitle=$localizedTitle, localizedDescription=$localizedDescription, paywallName=$paywallName, paywallABTestName=$paywallABTestName, variationId=$variationId, price=$price, localizedPrice=$localizedPrice, currencyCode=$currencyCode, currencySymbol=$currencySymbol, subscriptionPeriod=$subscriptionPeriod, localizedSubscriptionPeriod=$localizedSubscriptionPeriod, introductoryOfferEligibility=$introductoryOfferEligibility, introductoryDiscount=$introductoryDiscount, freeTrialPeriod=$freeTrialPeriod, localizedFreeTrialPeriod=$localizedFreeTrialPeriod, skuDetails=$skuDetails)"
    }
}