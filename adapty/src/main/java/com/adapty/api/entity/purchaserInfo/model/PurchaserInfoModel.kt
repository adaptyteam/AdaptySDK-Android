package com.adapty.api.entity.purchaserInfo.model

import com.adapty.api.entity.purchaserInfo.NonSubscriptionsPurchaserInfoRes
import com.adapty.api.entity.purchaserInfo.PaidAccessLevelPurchaserInfoRes
import com.adapty.api.entity.purchaserInfo.SubscriptionsPurchaserInfoRes

data class PurchaserInfoModel(
    var appId: String?,
    var customerUserId: String?,
    var email: String?,
    var idfa: String?,
    var firstName: String?,
    var lastName: String?,
    var facebookUserId: String?,
    var amplitudeUserId: String?,
    var mixpanelUserId: String?,
    var cognitoId: String?,
    var gender: String?,
    var birthday: String?,
    var createdAt: String?,
    var updatedAt: String?,
    var paidAccessLevels: HashMap<String, PaidAccessLevelPurchaserInfoModel>?,
    var subscriptions: HashMap<String, SubscriptionsPurchaserInfoModel>?,
    var nonSubscriptions: HashMap<String, ArrayList<NonSubscriptionsPurchaserInfoModel>>?
)