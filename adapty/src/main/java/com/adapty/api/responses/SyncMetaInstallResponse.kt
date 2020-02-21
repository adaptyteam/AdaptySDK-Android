package com.adapty.api.responses

import com.adapty.api.entity.syncmeta.DataSyncMetaRes
import com.google.gson.annotations.SerializedName

class SyncMetaInstallResponse {
    @SerializedName("data")
    var data: DataSyncMetaRes? = null

}