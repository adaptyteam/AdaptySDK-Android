package com.adapty.api.entity.purchaserInfo

import com.google.gson.annotations.SerializedName

open class AttributePurchaserInfoRes {

    @SerializedName("profile_id")
    var profileId: String? = null

    @SerializedName("customer_user_id")
    var customerUserId: String? = null

    @SerializedName("paid_access_levels")
    var accessLevels: HashMap<String, AccessLevelInfoRes>? = null

    @SerializedName("subscriptions")
    var subscriptions: HashMap<String, SubscriptionsPurchaserInfoRes>? = null

    @SerializedName("non_subscriptions")
    var nonSubscriptions: HashMap<String, ArrayList<NonSubscriptionsPurchaserInfoRes>>? = null

    @SerializedName("custom_attributes")
    var customAttributes: HashMap<String, Any>? = null
}