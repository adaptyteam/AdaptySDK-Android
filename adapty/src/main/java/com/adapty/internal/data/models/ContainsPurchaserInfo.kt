package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface ContainsPurchaserInfo {

    val profileId: String?
    val customerUserId: String?
    val accessLevels: HashMap<String, ProfileResponseData.Attributes.AccessLevelInfo>?
    val subscriptions: HashMap<String, ProfileResponseData.Attributes.SubscriptionsInfo>?
    val nonSubscriptions: HashMap<String, ArrayList<ProfileResponseData.Attributes.NonSubscriptionsInfo>>?

    fun extractPurchaserInfo(): ProfileResponseData.Attributes
}