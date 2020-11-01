package com.adapty.api.entity.purchaserInfo.model

data class NonSubscriptionInfoModel(
    var purchaseId: String?,
    var vendorProductId: String?,
    var store: String?,
    var purchasedAt: String?,
    var isOneTime: Boolean?,
    var isSandbox: Boolean?,
    var isRefund: Boolean?
)