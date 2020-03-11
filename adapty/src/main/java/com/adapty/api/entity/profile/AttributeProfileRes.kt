package com.adapty.api.entity.profile

import com.google.gson.annotations.SerializedName
import java.util.*

class AttributeProfileRes : AttributeProfileReq() {
    @SerializedName("profile_id")
    var profileId: String? = null
}