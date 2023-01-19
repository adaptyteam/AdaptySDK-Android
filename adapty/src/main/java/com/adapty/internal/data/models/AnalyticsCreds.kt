package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AnalyticsCreds(
    @SerializedName("iam_access_key_id")
    val iamAccessKeyId: String?,
    @SerializedName("iam_secret_key")
    val iamSecretKey: String?,
    @SerializedName("iam_session_token")
    val iamSessionToken: String?,
)