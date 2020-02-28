package com.adapty.api.entity.restore

import com.google.gson.annotations.SerializedName

class RestoreItem {
    @SerializedName("is_subscription")
    var isSubscription: Boolean? = null

    @SerializedName("product_id")
    var productId: String? = null

    @SerializedName("purchase_token")
    var purchaseToken: String? = null

}