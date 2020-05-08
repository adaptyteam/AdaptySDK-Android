package com.adapty.api.responses

import com.adapty.api.entity.containers.DataContainer
import com.adapty.api.entity.containers.MetaGetContainerRes
import com.google.gson.annotations.SerializedName

class PurchaseContainersResponse {
    @SerializedName("data")
    var data: ArrayList<DataContainer>? = null

    @SerializedName("meta")
    var meta: MetaGetContainerRes? = null
}