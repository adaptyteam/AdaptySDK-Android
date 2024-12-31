package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SetIntegrationIdRequest(
    @SerializedName("data")
    private val data: Map<String, String>
) {
    internal companion object {
        fun create(
            profileId: String,
            key: String,
            value: String,
        ) =
            SetIntegrationIdRequest(mapOf(key to value, "profile_id" to profileId))
    }
}