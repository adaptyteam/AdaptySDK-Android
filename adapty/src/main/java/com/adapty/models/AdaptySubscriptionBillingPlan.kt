package com.adapty.models

/**
 * A billing plan of a subscription purchase.
 *
 * The set of possible values is not limited to the predefined constants: new identifiers
 * may be introduced over time. Instances are compared by [value], so the predefined
 * constants can be used for comparison.
 *
 * Google Play subscriptions are always [UP_FRONT]. Other values come from purchases
 * made in other stores, which the profile shares across platforms.
 *
 * @property[value] The raw string identifier of the billing plan.
 */
public class AdaptySubscriptionBillingPlan(
    value: String,
) {

    public val value: String = value.trim()

    public companion object {
        /**
         * The whole subscription period is paid at once.
         */
        @JvmField
        public val UP_FRONT: AdaptySubscriptionBillingPlan = AdaptySubscriptionBillingPlan("up_front")

        /**
         * The subscription is paid monthly over a longer commitment period.
         */
        @JvmField
        public val MONTHLY: AdaptySubscriptionBillingPlan = AdaptySubscriptionBillingPlan("monthly")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptySubscriptionBillingPlan

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }
}
