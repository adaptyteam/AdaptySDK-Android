package com.adapty.api.entity.profile

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataProfileRes : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeProfileRes? = null
}