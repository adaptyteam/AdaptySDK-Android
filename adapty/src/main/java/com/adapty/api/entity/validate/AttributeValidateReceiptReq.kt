package com.adapty.api.entity.validate

import com.google.gson.annotations.SerializedName

class AttributeValidateReceiptReq {
    @SerializedName("profile_id")
    var profileId: String? = null

    @SerializedName("variation_id")
    var variationId: String? = null

    @SerializedName("store_country")
    var storeCountry: String? = null

    @SerializedName("original_price")
    var originalPrice: String? = null

    @SerializedName("discount_price")
    var discountPrice: String? = null

    @SerializedName("price_locale")
    var priceLocale: String? = null

    @SerializedName("is_subscription")
    var isSubscription: Boolean? = null

    @SerializedName("product_id")
    var productId: String? = null

    @SerializedName("purchase_token")
    var purchaseToken: String? = null

    @SerializedName("transaction_id")
    var transactionId: String? = null
}