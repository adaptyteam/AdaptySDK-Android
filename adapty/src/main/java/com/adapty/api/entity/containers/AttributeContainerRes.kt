package com.adapty.api.entity.containers

import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class AttributeContainerRes {
    @SerializedName("developer_id")
    var developerId: String? = null

    @SerializedName("revision")
    var revision: Int? = null

    @SerializedName("is_winback")
    var isWinback: Boolean? = null

    @SerializedName("variation_id")
    var variationId: String? = null

    @SerializedName("products")
    var products: ArrayList<Product>? = null
}