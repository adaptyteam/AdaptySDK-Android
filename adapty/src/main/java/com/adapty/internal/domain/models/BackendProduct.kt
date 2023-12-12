package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo
import com.android.billingclient.api.BillingClient

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BackendProduct(
    val vendorProductId: String,
    val timestamp: Long,
    val type: ProductType,
) {
    class SubscriptionData(
        val basePlanId: String,
        val offerId: String?,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class ProductType {
    object Consumable: ProductType() {

        val NAME = "cons"

        override fun toString(): String {
            return NAME
        }
    }
    object NonConsumable: ProductType() {

        val NAME = "noncons"

        override fun toString(): String {
            return NAME
        }
    }
    class Subscription(val subscriptionData: BackendProduct.SubscriptionData): ProductType() {

        companion object {
            val NAME = BillingClient.ProductType.SUBS
        }

        override fun toString(): String {
            return NAME
        }
    }
}