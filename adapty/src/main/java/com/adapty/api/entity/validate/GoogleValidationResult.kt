package com.adapty.api.entity.validate

import com.google.gson.annotations.SerializedName

class GoogleValidationResult {
    @SerializedName("kind")
    var kind: String? = null

    @SerializedName("startTimeMillis")
    var startTimeMillis: String? = null

    @SerializedName("expiryTimeMillis")
    var expiryTimeMillis: String? = null

    @SerializedName("autoRenewing")
    var autoRenewing: Boolean? = null

    @SerializedName("priceCurrencyCode")
    var priceCurrencyCode: String? = null

    @SerializedName("priceAmountMicros")
    var priceAmountMicros: String? = null

    @SerializedName("countryCode")
    var countryCode: String? = null

    @SerializedName("developerPayload")
    var developerPayload: String? = null

    @SerializedName("paymentState")
    var paymentState: Int? = null

    @SerializedName("orderId")
    var orderId: String? = null

    @SerializedName("purchaseType")
    var purchaseType: Int? = null

    @SerializedName("acknowledgementState")
    var acknowledgementState: Int? = null
}