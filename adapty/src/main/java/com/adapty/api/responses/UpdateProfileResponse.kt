package com.adapty.api.responses

import com.adapty.api.entity.profile.DataProfileRes
import com.adapty.api.entity.profile.update.DataUpdateProfileRes
import com.google.gson.annotations.SerializedName

class UpdateProfileResponse {
    @SerializedName("data")
    var data: DataUpdateProfileRes? = null
}