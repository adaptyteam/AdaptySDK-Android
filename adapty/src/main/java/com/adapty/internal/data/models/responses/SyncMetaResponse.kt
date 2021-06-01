package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SyncMetaResponse(
    @SerializedName("data")
    val data: Data?
) {
    internal class Data(
        @SerializedName("id")
        val id: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("attributes")
        val attributes: Attributes?
    ) {

        internal class Attributes(
            @SerializedName("profile_id")
            val profileId: String?,
            @SerializedName("iam_access_key_id")
            val iamAccessKeyId: String?,
            @SerializedName("iam_secret_key")
            val iamSecretKey: String?,
            @SerializedName("iam_session_token")
            val iamSessionToken: String?,
        )
    }
}