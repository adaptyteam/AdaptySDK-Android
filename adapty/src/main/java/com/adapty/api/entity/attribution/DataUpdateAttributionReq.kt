package com.adapty.api.entity.attribution

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataUpdateAttributionReq : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeUpdateAttributionReq? = null
}