package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.models.ProductSubscriptionPeriodModel
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class RestoreProductInfo(
    @SerializedName("is_subscription")
    val isSubscription: Boolean? = null,
    @SerializedName("product_id")
    val productId: String? = null,
    @SerializedName("purchase_token")
    val purchaseToken: String? = null,
    @SerializedName("title")
    var localizedTitle: String? = null,
    @SerializedName("localizedDescription")
    var localizedDescription: String? = null,
    @SerializedName("price")
    var price: String? = null,
    @SerializedName("currencyCode")
    var currencyCode: String? = null,
    @SerializedName("subscriptionPeriod")
    var subscriptionPeriod: ProductSubscriptionPeriodModel? = null,
    @SerializedName("transaction_id")
    val transactionId: String? = null
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