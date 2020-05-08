package com.adapty.api.entity.containers

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataContainer : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeContainerRes? = null
}