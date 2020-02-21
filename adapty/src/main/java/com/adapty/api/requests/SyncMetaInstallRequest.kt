package com.adapty.api.requests

import com.adapty.api.entity.syncmeta.DataSyncMetaReq
import com.google.gson.annotations.SerializedName

class SyncMetaInstallRequest {
    @SerializedName("data")
    var data: DataSyncMetaReq? = null
}