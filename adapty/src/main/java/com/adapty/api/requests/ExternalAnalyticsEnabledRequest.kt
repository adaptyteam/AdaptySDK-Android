package com.adapty.api.requests

import com.google.gson.annotations.SerializedName

class ExternalAnalyticsEnabledRequest(
    @SerializedName("data")
    val data: Data
) {
    class Data(
        val type: String = "adapty_analytics_profile_analytics_enabled",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        class Attributes(
            @SerializedName("enabled")
            val enabled: Boolean
        )
    }

    companion object {
        fun create(enabled: Boolean) = ExternalAnalyticsEnabledRequest(
            Data(attributes = Data.Attributes(enabled))
        )
    }
}