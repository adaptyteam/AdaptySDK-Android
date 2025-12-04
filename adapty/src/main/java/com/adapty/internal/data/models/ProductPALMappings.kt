package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.android.billingclient.api.Purchase
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ProductPALMappings(
    @SerializedName("items")
    val items: Map<String, Item>
) {
    class Item(
        @SerializedName("access_level_id")
        val accessLevelId: String,
        @SerializedName("product_type")
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