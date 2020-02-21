package com.adapty.api.entity.syncmeta

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataSyncMetaReq : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeSyncMetaReq? = null
}