package com.adapty.api.entity.validate

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataValidateReceiptReq : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeValidateReceiptReq? = null
}