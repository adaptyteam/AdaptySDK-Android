package com.adapty.models

import java.math.BigDecimal

class ProductDiscountModel(
    val price: BigDecimal,
    val numberOfPeriods: Int,
    val localizedPrice: String,
    val subscriptionPeriod: ProductSubscriptionPeriodModel,
    val localizedSubscriptionPeriod: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductDiscountModel

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
        return "ProductDiscountModel(price=$price, numberOfPeriods=$numberOfPeriods, localizedPrice='$localizedPrice', subscriptionPeriod=$subscriptionPeriod, localizedSubscriptionPeriod='$localizedSubscriptionPeriod')"
    }
}