package com.adapty.api.entity.validate

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataValidateReceiptRes : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeValidateReceiptRes? = null

}