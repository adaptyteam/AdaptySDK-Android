package com.adapty.api.entity.profile

import com.google.gson.annotations.SerializedName
import java.util.*

open class AttributeProfileReq {
    @SerializedName("customerUserId")
    var customerUserId: String? = null

    @SerializedName("email")
    var email: String? = null

    @SerializedName("phoneNumber")
    var phoneNumber: String? = null

    @SerializedName("idfa")
    var idfa: String? = null

    @SerializedName("facebookUserId")
    var facebookUserId: String? = null

    @SerializedName("firstName")
    var firstName: String? = null

    @SerializedName("lastName")
    var lastName: String? = null

    @SerializedName("gender")
    var gender: String? = null

    @SerializedName("birthday")
    var birthday: String? = null

    @SerializedName("appId")
    var appId: String? = null

    @SerializedName("amplitudeUserId")
    var amplitudeUserId: String? = null

    @SerializedName("mixpanelUserId")
    var mixpanelUserId: String? = null

    @SerializedName("cognitoId")
    var cognitoId: String? = null

    @SerializedName("createdAt")
    var createdAt: Date? = null

    @SerializedName("updatedAt")
    var updatedAt: Date? = null
}