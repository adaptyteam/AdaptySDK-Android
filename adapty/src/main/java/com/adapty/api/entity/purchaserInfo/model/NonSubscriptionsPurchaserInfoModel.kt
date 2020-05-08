package com.adapty.api.entity.purchaserInfo.model

import com.google.gson.annotations.SerializedName

data class NonSubscriptionsPurchaserInfoModel(
    var purchaseId: String?,
    var vendorProductId: String?,
    var store: String?,
    var purchasedAt: String?,
    var isOneTime: Boolean?,
    var isSandbox: Boolean?
)