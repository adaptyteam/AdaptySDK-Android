package com.adapty.api.responses

import com.adapty.api.entity.containers.PromoDataContainer
import com.google.gson.annotations.SerializedName

internal class PromoResponse {
    @SerializedName("data")
    var data: PromoDataContainer? = null
}