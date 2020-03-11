package com.adapty.api.entity.validate

import com.google.gson.annotations.SerializedName

class GoogleValidationResult {
    @SerializedName("kind")
    var kind: String? = null

    @SerializedName("start_time_millis")
    var startTimeMillis: String? = null

    @SerializedName("expiry_time_millis")
    var expiryTimeMillis: String? = null

    @SerializedName("auto_renewing")
    var autoRenewing: Boolean? = null

    @SerializedName("price_currency_code")
    var priceCurrencyCode: String? = null

    @SerializedName("price_amount_micros")
    var priceAmountMicros: String? = null

    @SerializedName("country_code")
    var countryCode: String? = null

    @SerializedName("developer_payload")
    var developerPayload: String? = null

    @SerializedName("payment_state")
    var paymentState: Int? = null

    @SerializedName("order_id")
    var orderId: String? = null

    @SerializedName("purchase_type")
    var purchaseType: Int? = null

    @SerializedName("acknowledgement_state")
    var acknowledgementState: Int? = null
}