package com.adapty.api.entity.paywalls

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataContainer : BaseData() {
    @SerializedName("attributes")
    var attributes: PaywallModel? = null
}