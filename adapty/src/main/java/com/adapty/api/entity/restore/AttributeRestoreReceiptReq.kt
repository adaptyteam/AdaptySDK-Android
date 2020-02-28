package com.adapty.api.entity.validate

import com.adapty.api.entity.restore.RestoreItem
import com.google.gson.annotations.SerializedName

open class AttributeRestoreReceiptReq {
    @SerializedName("profile_id")
    var profileId: String? = null

    @SerializedName("restore_items")
    var restoreItems: ArrayList<RestoreItem>? = null
}