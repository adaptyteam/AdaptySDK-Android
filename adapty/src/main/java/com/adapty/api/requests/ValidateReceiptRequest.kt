package com.adapty.api.requests

import com.adapty.api.entity.receipt.DataValidateReceiptReq
import com.google.gson.annotations.SerializedName

class ValidateReceiptRequest {
    @SerializedName("data")
    var data: DataValidateReceiptReq? = null

}