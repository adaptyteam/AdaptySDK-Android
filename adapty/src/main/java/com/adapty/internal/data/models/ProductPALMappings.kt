package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.android.billingclient.api.Purchase

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ProductPALMappings(
    val items: Map<String, Item>
) {
    class Item(
        val accessLevelId: String,
        val productType: String,
    )

    class ItemExtended(
        val vendorProductId: String,
        val basePlanId: String?,
        val offerId: String?,
        val accessLevelId: String,
        val productType: String,
        val purchase: Purchase,
        val endTimestamp: Long,
    ) {
        val isLifetime get() = endTimestamp == Long.MAX_VALUE

        val willRenew get() = purchase.isAutoRenewing

        val transactionId get() = purchase.orderId
    }
}