package com.adapty.api.requests

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class PurchaserInfoRequest {
    @SerializedName("data")
    var data: BaseData? = null
}