package com.adapty.api.entity.purchaserInfo

import com.google.gson.annotations.SerializedName

class NonSubscriptionsPurchaserInfoRes {
    @SerializedName("purchase_id")
    var purchaseId: String? = null

    @SerializedName("vendor_product_id")
    var vendorProductId: String? = null

    @SerializedName("store")
    var store: String? = null

    @SerializedName("purchased_at")
    var purchasedAt: String? = null

    @SerializedName("is_one_time")
    var isOneTime: Boolean? = null

    @SerializedName("is_sandbox")
    var isSandbox: Boolean? = null
}