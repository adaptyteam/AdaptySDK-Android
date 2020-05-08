package com.adapty.api.entity.containers

import com.google.gson.annotations.SerializedName

class MetaGetContainerRes {
    @SerializedName("products")
    var products: ArrayList<Product>? = null
}