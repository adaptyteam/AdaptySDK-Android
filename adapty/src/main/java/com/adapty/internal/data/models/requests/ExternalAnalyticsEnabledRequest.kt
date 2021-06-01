package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ExternalAnalyticsEnabledRequest(
    @SerializedName("data")
    val data: Data
) {
    internal class Data(
        @SerializedName("type")
        val type: String = "adapty_analytics_profile_analytics_enabled",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("enabled")
            val enabled: Boolean
        )
    }

    internal companion object {
        fun create(enabled: Boolean) = ExternalAnalyticsEnabledRequest(
            Data(attributes = Data.Attributes(enabled))
        )
    }
}