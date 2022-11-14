package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class InstallationMeta(
    @SerializedName("device_id")
    private val deviceId: String,
    @SerializedName("adapty_sdk_version")
    private val adaptySdkVersion: String,
    @SerializedName("app_build")
    private val appBuild: String,
    @SerializedName("app_version")
    private val appVersion: String,
    @SerializedName("device")
    private val device: String,
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
) {
    internal companion object {
        fun create(
            deviceId: String,
            adaptySdkVersion: String,
            appBuild: String,
            appVersion: String,
            device: String,
            locale: String?,
            os: String,
            platform: String,
            timezone: String,
            advertisingId: String?,
        ) = InstallationMeta(
            deviceId,
            adaptySdkVersion,
            appBuild,
            appVersion,
            device,
            locale,
            os,
            platform,
            timezone,
            advertisingId,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstallationMeta

        if (deviceId != other.deviceId) return false
        if (adaptySdkVersion != other.adaptySdkVersion) return false
        if (appBuild != other.appBuild) return false
        if (appVersion != other.appVersion) return false
        if (device != other.device) return false
        if (locale != other.locale) return false
        if (os != other.os) return false
        if (platform != other.platform) return false
        if (timezone != other.timezone) return false
        if (advertisingId != other.advertisingId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + adaptySdkVersion.hashCode()
        result = 31 * result + appBuild.hashCode()
        result = 31 * result + appVersion.hashCode()
        result = 31 * result + device.hashCode()
        result = 31 * result + (locale?.hashCode() ?: 0)
        result = 31 * result + os.hashCode()
        result = 31 * result + platform.hashCode()
        result = 31 * result + timezone.hashCode()
        result = 31 * result + (advertisingId?.hashCode() ?: 0)
        return result
    }
}