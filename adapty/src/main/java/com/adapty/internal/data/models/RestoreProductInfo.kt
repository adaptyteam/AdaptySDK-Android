package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.models.ProductSubscriptionPeriodModel
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RestoreProductInfo(
    @SerializedName("is_subscription")
    val isSubscription: Boolean?,
    @SerializedName("product_id")
    val productId: String?,
    @SerializedName("purchase_token")
    val purchaseToken: String?,
    @SerializedName("title")
    var localizedTitle: String?,
    @SerializedName("localizedDescription")
    var localizedDescription: String?,
    @SerializedName("price")
    var price: String?,
    @SerializedName("currencyCode")
    var currencyCode: String?,
    @SerializedName("subscriptionPeriod")
    var subscriptionPeriod: ProductSubscriptionPeriodModel?,
    @SerializedName("transaction_id")
    val transactionId: String?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RestoreProductInfo

        if (purchaseToken != other.purchaseToken) return false

        return true
    }

    override fun hashCode(): Int {
        return purchaseToken?.hashCode() ?: 0
    }

}