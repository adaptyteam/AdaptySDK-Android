package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CreateProfileRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String = "adapty_analytics_profile",
        @SerializedName("attributes")
        val attributes: Attributes?
    ) {

        internal class Attributes(
            @SerializedName("customer_user_id")
            val customerUserId: String
        )
    }

    internal companion object {
        fun create(profileId: String, customerUserId: String?) = CreateProfileRequest(
            Data(
                id = profileId,
                attributes = customerUserId?.takeIf(String::isNotEmpty)
                    ?.let { Data.Attributes(it) })
        )
    }
}