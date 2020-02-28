package com.adapty.api.requests

import com.adapty.api.entity.validate.DataRestoreReceiptReq
import com.google.gson.annotations.SerializedName

class RestoreReceiptRequest {
    @SerializedName("data")
    var data: DataRestoreReceiptReq? = null
}