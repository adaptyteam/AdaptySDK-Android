package com.adapty.api.entity.syncmeta

import com.google.gson.annotations.SerializedName

open class AttributeSyncMetaRes : AttributeSyncMetaReq() {
    @SerializedName("appId")
    var appId: String? = null

    @SerializedName("profileId")
    var profileId: String? = null

    @SerializedName("adaptySdkVersionInt")
    var adaptySdkVersionInt: String? = null

    @SerializedName("deviceToken")
    var deviceToken: String? = null

    @SerializedName("cognitoId")
    var cognitoId: String? = null

    @SerializedName("iamAccessKeyId")
    var iamAccessKeyId: String? = null

    @SerializedName("iamSecretKey")
    var iamSecretKey: String? = null

    @SerializedName("iamSessionToken")
    var iamSessionToken: String? = null

    @SerializedName("iamExpiration")
    var iamExpiration: String? = null

    @SerializedName("lastActiveAt")
    var lastActiveAt: String? = null

    @SerializedName("createdAt")
    var createdAt: String? = null

    @SerializedName("updatedAt")
    var updatedAt: String? = null
}