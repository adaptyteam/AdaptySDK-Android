package com.adapty.models

import com.android.billingclient.api.Purchase

public sealed class AdaptyPurchaseResult {

    /**
     * The purchase succeeded with an `AdaptyProfile`.
     *
     * @property[profile] An [AdaptyProfile] which contains the up-to-date information about the user.
     * @property[purchase] A [Purchase] object, which represents the payment.
     */
    public class Success(
        public val profile: AdaptyProfile,
        public val purchase: Purchase?,
    ): AdaptyPurchaseResult() {
        override fun toString(): String {
            return "AdaptyPurchaseResult.Success(profile=$profile, purchase=$purchase)"
        }
    }

    /**
     * The user canceled the purchase.
     */
    public object UserCanceled: AdaptyPurchaseResult() {
        override fun toString(): String {
            return "AdaptyPurchaseResult.UserCanceled"
        }
    }

    /**
     * The purchase is pending some user action.
     */
    public object Pending: AdaptyPurchaseResult() {
        override fun toString(): String {
            return "AdaptyPurchaseResult.Pending"
        }
    }
}