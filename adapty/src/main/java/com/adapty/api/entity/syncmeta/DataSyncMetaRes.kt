package com.adapty.api.entity.syncmeta

import com.adapty.api.entity.BaseData
import com.google.gson.annotations.SerializedName

class DataSyncMetaRes : BaseData() {
    @SerializedName("attributes")
    var attributes: AttributeSyncMetaRes? = null
}