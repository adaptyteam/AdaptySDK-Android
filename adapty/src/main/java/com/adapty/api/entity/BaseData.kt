package com.adapty.api.entity

import com.google.gson.annotations.SerializedName

open class BaseData {
    @SerializedName("id")
    var id: String? = null

    @SerializedName("type")
    var type: String? = null
}