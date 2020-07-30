package com.adapty.api.entity.attribution

import com.google.gson.annotations.SerializedName

open class AttributeUpdateAttributionReq {
    @SerializedName("source")
    var source: String? = null

    @SerializedName("attribution")
    var attribution: Any? = null

    @SerializedName("network_user_id")
    var networkUserId: String? = null
}