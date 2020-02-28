package com.adapty.api.responses

import com.adapty.api.entity.validate.DataRestoreReceiptRes
import com.adapty.api.entity.validate.DataValidateReceiptRes
import com.google.gson.annotations.SerializedName

class RestoreReceiptResponse {
    @SerializedName("data")
    var data: DataRestoreReceiptRes? = null

}