package com.adapty.api.entity.syncmeta

import com.google.gson.annotations.SerializedName

class AttributeSyncMetaRes : AttributeSyncMetaReq() {
    @SerializedName("app_id")
    var appId: String? = null

    @SerializedName("profile_id")
    var profileId: String? = null

    @SerializedName("cognito_id")
    var cognitoId: String? = null

    @SerializedName("iam_access_key_id")
    var iamAccessKeyId: String? = null

    @SerializedName("iam_secret_key")
    var iamSecretKey: String? = null

    @SerializedName("iam_session_token")
    var iamSessionToken: String? = null

    @SerializedName("iam_expiration")
    var iamExpiration: String? = null

    @SerializedName("last_active_at")
    var lastActiveAt: String? = null

    @SerializedName("created_at")
    var createdAt: String? = null

    @SerializedName("updated_at")
    var updatedAt: String? = null
}