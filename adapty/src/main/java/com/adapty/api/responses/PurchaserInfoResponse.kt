package com.adapty.api.responses

import com.adapty.api.entity.purchaserInfo.DataPurchaserInfoRes
import com.google.gson.annotations.SerializedName

class PurchaserInfoResponse {
    @SerializedName("data")
    var data: DataPurchaserInfoRes? = null
}