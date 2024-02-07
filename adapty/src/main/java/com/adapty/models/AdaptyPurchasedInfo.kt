package com.adapty.models

import com.android.billingclient.api.Purchase

/**
 * @property[profile] An [AdaptyProfile] which contains the up-to-date information about the user.
 * @property[purchase] A [Purchase] object, which represents the payment.
 */
public class AdaptyPurchasedInfo(
    public val profile: AdaptyProfile,
    public val purchase: Purchase,
) {
    override fun toString(): String {
        return "AdaptyPurchasedInfo(profile=$profile, purchase=$purchase)"
    }
}