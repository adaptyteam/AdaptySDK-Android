package com.adapty.api.entity.purchaserInfo

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataPurchaserInfoRes : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributePurchaserInfoRes? = null
}