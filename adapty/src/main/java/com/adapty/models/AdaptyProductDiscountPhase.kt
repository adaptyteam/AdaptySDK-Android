package com.adapty.models

import com.adapty.models.AdaptyPaywallProduct.Price

/**
 * @property[localizedNumberOfPeriods] The total duration of the discount phase period.
 * i.e. If you have a 3-month subscription and an intro offer with recurring billing of 2 cycles –
 * this property value equals "6 months" in English while [localizedSubscriptionPeriod] equals "3 months",
 * and the [price] shows the price for a single billing period (in this case – 3 months).
 * @property[localizedSubscriptionPeriod] A formatted subscription period of the discount phase for a user’s locale.
 * @property[numberOfPeriods] A number of periods the discount phase is available.
 * @property[paymentMode] A payment mode for the discount phase.
 * @property[price] [Price] of the discount phase in a local currency.
 * @property[subscriptionPeriod] An information about the period for the discount phase.
 */
public class AdaptyProductDiscountPhase(
    public val price: Price,
    public val numberOfPeriods: Int,
    public val paymentMode: PaymentMode,
    public val localizedNumberOfPeriods: String,
    public val subscriptionPeriod: AdaptyProductSubscriptionPeriod,
    public val localizedSubscriptionPeriod: String,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyProductDiscountPhase

        if (price != other.price) return false
        if (numberOfPeriods != other.numberOfPeriods) return false
        if (paymentMode != other.paymentMode) return false
        if (localizedNumberOfPeriods != other.localizedNumberOfPeriods) return false
        if (subscriptionPeriod != other.subscriptionPeriod) return false
        if (localizedSubscriptionPeriod != other.localizedSubscriptionPeriod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = price.hashCode()
        result = 31 * result + numberOfPeriods
        result = 31 * result + paymentMode.hashCode()
        result = 31 * result + localizedNumberOfPeriods.hashCode()
        result = 31 * result + subscriptionPeriod.hashCode()
        result = 31 * result + localizedSubscriptionPeriod.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptySubscriptionPhase(price=$price, numberOfPeriods=$numberOfPeriods, paymentMode=$paymentMode, localizedNumberOfPeriods=$localizedNumberOfPeriods, subscriptionPeriod=$subscriptionPeriod, localizedSubscriptionPeriod=$localizedSubscriptionPeriod)"
    }

    public enum class PaymentMode { PAY_AS_YOU_GO, PAY_UPFRONT, FREE_TRIAL, UNKNOWN }
}