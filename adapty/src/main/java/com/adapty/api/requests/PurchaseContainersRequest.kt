package com.adapty.api.requests

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class PurchaseContainersRequest {
    @SerializedName("data")
    var data: BaseData? = null
}