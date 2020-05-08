package com.adapty.api.entity.profile.update

import com.adapty.api.entity.BaseData
import com.adapty.api.entity.profile.AttributeProfileRes
import com.adapty.api.entity.purchaserInfo.AttributePurchaserInfoRes
import com.google.gson.annotations.SerializedName

class DataUpdateProfileRes : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeProfileRes? = null
}