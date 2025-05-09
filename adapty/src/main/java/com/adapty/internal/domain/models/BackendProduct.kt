package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.InternalAdaptyApi
import com.android.billingclient.api.BillingClient

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalAdaptyApi
public class BackendProduct internal constructor(
    public val id: String,
    public val vendorProductId: String,
    public val paywallProductIndex: Int,
    public val timestamp: Long,
    public val type: ProductType,
) {
    public class SubscriptionData(
        public val basePlanId: String,
        public val offerId: String?,
    )
}

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalAdaptyApi
public sealed class ProductType {
    public object Consumable: ProductType() {

        internal val NAME = "cons"

        override fun toString(): String {
            return NAME
        }
    }
    public object NonConsumable: ProductType() {

        internal val NAME = "noncons"

        override fun toString(): String {
            return NAME
        }
    }
    public class Subscription(public val subscriptionData: BackendProduct.SubscriptionData): ProductType() {

        internal companion object {
            internal val NAME = BillingClient.ProductType.SUBS
        }

        override fun toString(): String {
            return NAME
        }
    }
}