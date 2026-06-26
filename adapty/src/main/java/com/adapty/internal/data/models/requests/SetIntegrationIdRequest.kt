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
            keyValues: Map<String, String>,
        ) =
            SetIntegrationIdRequest(keyValues + ("profile_id" to profileId))
    }
}