package com.adapty.models

import java.math.BigDecimal

public class AdaptyProductDiscount(
    public val price: BigDecimal,
    public val numberOfPeriods: Int,
    public val localizedPrice: String,
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
        return "AdaptyProductDiscount(price=$price, numberOfPeriods=$numberOfPeriods, localizedPrice='$localizedPrice', subscriptionPeriod=$subscriptionPeriod, localizedSubscriptionPeriod='$localizedSubscriptionPeriod')"
    }
}