package com.adapty.api.entity.containers

import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class Paywall {
    @SerializedName("developer_id")
    var developerId: String? = null

    @SerializedName("revision")
    var revision: Int? = null

    @SerializedName("is_promo")
    var isPromo: Boolean? = null

    @SerializedName("variation_id")
    var variationId: String? = null

    @SerializedName("products")
    var products: ArrayList<Product>? = null

    @SerializedName("custom_payload")
    var customPayload: String? = null
}