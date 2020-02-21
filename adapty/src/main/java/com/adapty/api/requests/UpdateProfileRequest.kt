package com.adapty.api.requests

import com.adapty.api.entity.profile.DataProfileReq
import com.google.gson.annotations.SerializedName

class UpdateProfileRequest {
    @SerializedName("data")
    var data: DataProfileReq? = null
}