package com.adapty.api.entity.receipt

import com.google.gson.annotations.SerializedName

open class AttributeValidateReceiptReq {
    @SerializedName("profileId")
    var profileId: String? = null

    @SerializedName("token")
    var token: String? = null

    @SerializedName("product_id")
    var productId: String? = null

    @SerializedName("variationId")
    var variationId: Int? = null

    @SerializedName("storeCountry")
    var storeCountry: String? = null

    @SerializedName("originalPrice")
    var originalPrice: String? = null

    @SerializedName("discountPrice")
    var discountPrice: String? = null

    @SerializedName("priceLocale")
    var priceLocale: String? = null

    @SerializedName("receiptEncoded")
    var receiptEncoded: String? = null
}