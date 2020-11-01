package com.adapty.purchase

import com.android.billingclient.api.BillingClient

enum class PurchaseType {
    SUBS, INAPP;

    override fun toString() = when (this) {
        SUBS -> BillingClient.SkuType.SUBS
        INAPP -> BillingClient.SkuType.INAPP
    }
}