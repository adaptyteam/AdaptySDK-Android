package com.adapty.api.entity.paywalls

import com.google.gson.annotations.SerializedName

class MetaGetContainerRes {
    @SerializedName("products")
    var products: ArrayList<ProductModel>? = null
}