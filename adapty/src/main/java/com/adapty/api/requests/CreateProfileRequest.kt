package com.adapty.api.requests

import com.adapty.api.entity.profile.DataProfileReq
import com.google.gson.annotations.SerializedName

class CreateProfileRequest {
    @SerializedName("data")
    var data: DataProfileReq? = null
}