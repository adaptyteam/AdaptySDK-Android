package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.ProductMapper
import com.adapty.internal.utils.formatPrice
import com.adapty.models.ProductSubscriptionPeriodModel
import com.android.billingclient.api.SkuDetails
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

    @JvmSynthetic
    fun setDetails(sd: SkuDetails?, productMapper: ProductMapper) {
        if (sd == null) return

        localizedDescription = sd.description
        price = formatPrice(sd.priceAmountMicros)
        currencyCode = sd.priceCurrencyCode
        subscriptionPeriod = productMapper.mapSubscriptionPeriodModel(sd.subscriptionPeriod)
    }

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