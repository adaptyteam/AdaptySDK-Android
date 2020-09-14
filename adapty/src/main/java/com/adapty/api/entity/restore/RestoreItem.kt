package com.adapty.api.entity.restore

import com.adapty.api.entity.containers.Product
import com.adapty.utils.formatPrice
import com.android.billingclient.api.SkuDetails
import com.google.gson.annotations.SerializedName

data class RestoreItem(
    @SerializedName("is_subscription")
    var isSubscription: Boolean? = null,

    @SerializedName("product_id")
    var productId: String? = null,

    @SerializedName("purchase_token")
    var purchaseToken: String? = null,

    @SerializedName("title")
    var localizedTitle: String? = null,

    @SerializedName("localizedDescription")
    var localizedDescription: String? = null,

    var skuDetails: SkuDetails? = null,

    @SerializedName("price")
    var price: String? = null,

    @SerializedName("currencyCode")
    var currencyCode: String? = null,

    @SerializedName("subscriptionPeriod")
    var subscriptionPeriod: Product.ProductSubscriptionPeriodModel? = null,

    @SerializedName("transaction_id")
    var transactionId: String? = null
) {

    fun setDetails(sd: SkuDetails?) {
        sd?.let {
            this.skuDetails = it
            this.localizedDescription = it.description
            this.price = formatPrice(it.priceAmountMicros)
            this.currencyCode = it.priceCurrencyCode
            this.subscriptionPeriod = Product.ProductSubscriptionPeriodModel(it.subscriptionPeriod)
        }
    }
}