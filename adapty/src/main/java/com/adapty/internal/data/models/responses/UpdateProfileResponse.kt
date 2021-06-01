package com.adapty.internal.data.models.responses

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UpdateProfileResponse(
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
            @SerializedName("customer_user_id")
            val customerUserId: String?,
        )
    }
}