package com.adapty.api.entity.profile

import com.google.gson.annotations.SerializedName
import java.util.*

open class AttributeProfileReq {
    @SerializedName("customer_user_id")
    var customerUserId: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("phone_number")
    var phoneNumber: String? = null

    @SerializedName("idfa")
    var idfa: String? = null

    @SerializedName("facebook_user_id")
    var facebookUserId: String? = null

    @SerializedName("first_name")
    var firstName: String? = null

    @SerializedName("last_name")
    var lastName: String? = null

    @SerializedName("gender")
    var gender: String? = null

    @SerializedName("birthday")
    var birthday: String? = null

    @SerializedName("app_id")
    var appId: String? = null

    @SerializedName("amplitude_user_id")
    var amplitudeUserId: String? = null

    @SerializedName("mixpanel_user_id")
    var mixpanelUserId: String? = null

    @SerializedName("cognito_id")
    var cognitoId: String? = null

    @SerializedName("created_at")
    var createdAt: Date? = null

    @SerializedName("updated_at")
    var updatedAt: Date? = null

    @SerializedName("advertising_id")
    var advertisingId: String? = null
}