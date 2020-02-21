package com.adapty.api.responses

import com.adapty.api.entity.profile.DataProfileRes
import com.google.gson.annotations.SerializedName

class UpdateProfileResponse {
    @SerializedName("data")
    var data: DataProfileRes? = null
}