package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.android.billingclient.api.BillingClient

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchaseRecordModel(
    val purchaseToken: String,
    val purchaseTime: Long,
    val products: List<String>,
    @BillingClient.ProductType val type: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PurchaseRecordModel) return false

        if (purchaseToken != other.purchaseToken) return false
        if (purchaseTime != other.purchaseTime) return false
        if (products != other.products) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = purchaseToken.hashCode()
        result = 31 * result + purchaseTime.hashCode()
        result = 31 * result + products.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}