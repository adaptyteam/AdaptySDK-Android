package com.adapty.api.requests

import com.adapty.api.entity.attribution.DataUpdateAttributionReq
import com.google.gson.annotations.SerializedName

class UpdateAttributionRequest {
    @SerializedName("data")
    var data: DataUpdateAttributionReq? = null

}