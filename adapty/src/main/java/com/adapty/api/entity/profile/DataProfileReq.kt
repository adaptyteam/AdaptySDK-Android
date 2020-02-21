package com.adapty.api.entity.profile

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataProfileReq : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeProfileReq? = null
}