package com.adapty.api.entity.purchaserInfo

import com.google.gson.annotations.SerializedName

class AttributePurchaserInfoRes {
    @SerializedName("app_id")
    var appId: String? = null

    @SerializedName("customer_user_id")
    var customerUserId: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("idfa")
    var idfa: String? = null

    @SerializedName("first_name")
    var firstName: String? = null

    @SerializedName("last_name")
    var lastName: String? = null

    @SerializedName("facebook_user_id")
    var facebookUserId: String? = null

    @SerializedName("amplitude_user_id")
    var amplitudeUserId: String? = null

    @SerializedName("mixpanel_user_id")
    var mixpanelUserId: String? = null

    @SerializedName("promotional_offer_eligibility")
    var promotionalOfferEligibility: Boolean? = null

    @SerializedName("introductory_offer_eligibility")
    var introductoryOfferEligibility: Boolean? = null

    @SerializedName("cognito_id")
    var cognitoId: String? = null

    @SerializedName("gender")
    var gender: String? = null

    @SerializedName("birthday")
    var birthday: String? = null

    @SerializedName("created_at")
    var createdAt: String? = null

    @SerializedName("updated_at")
    var updatedAt: String? = null

    @SerializedName("paid_access_levels")
    var paidAccessLevels: HashMap<String, PaidAccessLevelPurchaserInfoRes>? = null

    @SerializedName("subscriptions")
    var subscriptions: HashMap<String, SubscriptionsPurchaserInfoRes>? = null

    @SerializedName("non_subscriptions")
    var nonSubscriptions: HashMap<String, ArrayList<NonSubscriptionsPurchaserInfoRes>>? = null

}