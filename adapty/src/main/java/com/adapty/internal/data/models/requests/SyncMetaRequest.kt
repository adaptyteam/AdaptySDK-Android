package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SyncMetaRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String = "adapty_analytics_profile_installation_meta",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("adapty_sdk_version")
            private val adaptySdkVersion: String,
            @SerializedName("adapty_sdk_version_build")
            private val adaptySdkVersionBuild: Int,
            @SerializedName("app_build")
            private val appBuild: String,
            @SerializedName("app_version")
            private val appVersion: String,
            @SerializedName("device")
            private val device: String,
            @SerializedName("device_token")
            private val deviceToken: String?,
            @SerializedName("locale")
            private val locale: String?,
            @SerializedName("os")
            private val os: String,
            @SerializedName("platform")
            private val platform: String,
            @SerializedName("timezone")
            private val timezone: String,
            @SerializedName("advertising_id")
            private val advertisingId: String?,
        )
    }

    internal companion object {
        fun create(
            id: String,
            adaptySdkVersion: String,
            adaptySdkVersionBuild: Int,
            appBuild: String,
            appVersion: String,
            device: String,
            deviceToken: String?,
            locale: String?,
            os: String,
            platform: String,
            timezone: String,
            advertisingId: String?,
        ) = SyncMetaRequest(
            Data(
                id = id,
                attributes = Data.Attributes(
                    adaptySdkVersion,
                    adaptySdkVersionBuild,
                    appBuild,
                    appVersion,
                    device,
                    deviceToken,
                    locale,
                    os,
                    platform,
                    timezone,
                    advertisingId
                )
            )
        )
    }
}