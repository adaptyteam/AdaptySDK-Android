package com.adapty.api.responses

import com.adapty.api.entity.receipt.DataValidateReceiptRes
import com.google.gson.annotations.SerializedName

class ValidateReceiptResponse {
    @SerializedName("data")
    var data: DataValidateReceiptRes? = null

}