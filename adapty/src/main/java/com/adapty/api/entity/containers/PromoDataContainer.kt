package com.adapty.api.entity.containers

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

internal class PromoDataContainer : BaseData() {
    @SerializedName("attributes")
    var attributes: Promo? = null
}