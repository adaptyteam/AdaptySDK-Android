package com.adapty.api.entity.paywalls

import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class PaywallModel {
    @SerializedName("developer_id")
    var developerId: String? = null

    @SerializedName("revision")
    var revision: Int? = null

    @SerializedName("is_promo")
    var isPromo: Boolean? = null

    @SerializedName("variation_id")
    var variationId: String? = null

    @SerializedName("products")
    var products: ArrayList<ProductModel>? = null

    @SerializedName("custom_payload")
    var customPayload: String? = null
}