package com.adapty.api.entity.validate

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataRestoreReceiptReq : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeRestoreReceiptReq? = null
}