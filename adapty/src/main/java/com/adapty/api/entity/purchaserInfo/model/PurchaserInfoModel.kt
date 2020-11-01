package com.adapty.api.entity.purchaserInfo.model

data class PurchaserInfoModel(
    var customerUserId: String?,
    var accessLevels: Map<String, AccessLevelInfoModel>?,
    var subscriptions: Map<String, SubscriptionInfoModel>?,
    var nonSubscriptions: Map<String, List<NonSubscriptionInfoModel>>?,
    var customAttributes: Map<String, Any>?
)