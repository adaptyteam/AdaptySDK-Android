package com.adapty.api.entity.paywalls

import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class PaywallModel(
    val developerId: String?,
    val revision: Int?,
    val isPromo: Boolean?,
    val variationId: String?,
    val products: ArrayList<ProductModel>?,
    val customPayloadString: String?
) {
    var customPayload: Map<String, Any>? = null
}

internal class PaywallDto(
    @SerializedName("developer_id")
    var developerId: String? = null,
    @SerializedName("revision")
    var revision: Int? = null,
    @SerializedName("is_promo")
    var isPromo: Boolean? = null,
    @SerializedName("variation_id")
    var variationId: String? = null,
    @SerializedName("products")
    var products: ArrayList<ProductModel>? = null,
    @SerializedName("custom_payload")
    var customPayload: String? = null
)