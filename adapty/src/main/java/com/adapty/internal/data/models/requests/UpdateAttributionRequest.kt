package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.AttributionData
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class UpdateAttributionRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("type")
        val type: String = "adapty_analytics_profile_attribution",
        @SerializedName("attributes")
        val attributes: AttributionData
    )

    internal companion object {
        fun create(attributionData: AttributionData) = UpdateAttributionRequest(
            Data(attributes = attributionData)
        )
    }
}