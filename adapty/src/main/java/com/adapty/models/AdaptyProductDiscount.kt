package com.adapty.models

import java.math.BigDecimal

/**
 * @property[localizedNumberOfPeriods] The total duration of the introductory price period.
 * i.e. If you have a 3-month subscription and an intro offer with recurring billing of 2 cycles –
 * this property value equals "6 months" in English while [localizedSubscriptionPeriod] equals "3 months",
 * and the [price]/[localizedPrice] shows the price for a single billing period (in this case – 3 months).
 * @property[localizedPrice] The formatted introductory price from Google Play as is.
 * @property[localizedSubscriptionPeriod] A formatted subscription period of a discount for a user’s locale.
 * @property[numberOfPeriods] A number of periods this product discount is available.
 * @property[price] Introductory price of a product in a local currency.
 * @property[subscriptionPeriod] An information about period for a product discount.
 */
public class AdaptyProductDiscount(
    public val price: BigDecimal,
    public val localizedPrice: String,
    public val numberOfPeriods: Int,
    public val localizedNumberOfPeriods: String,
    public val subscriptionPeriod: AdaptyProductSubscriptionPeriod,
    public val localizedSubscriptionPeriod: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyProductDiscount

        if (price != other.price) return false
        if (numberOfPeriods != other.numberOfPeriods) return false
        if (localizedPrice != other.localizedPrice) return false
        if (subscriptionPeriod != other.subscriptionPeriod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = price.hashCode()
        result = 31 * result + numberOfPeriods
        result = 31 * result + localizedPrice.hashCode()
        result = 31 * result + subscriptionPeriod.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyProductDiscount(price=$price, localizedPrice='$localizedPrice', numberOfPeriods=$numberOfPeriods, localizedNumberOfPeriods='$localizedNumberOfPeriods, subscriptionPeriod=$subscriptionPeriod, localizedSubscriptionPeriod='$localizedSubscriptionPeriod')"
    }
}