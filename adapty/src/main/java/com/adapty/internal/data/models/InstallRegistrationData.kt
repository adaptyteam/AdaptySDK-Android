package com.adapty.internal.data.models

import com.google.gson.annotations.SerializedName

internal data class InstallRegistrationData(
    @SerializedName("bundle_id")
    val bundleId: String,
    @SerializedName("referrer")
    val referrer: String?,
    @SerializedName("gaid")
    val gaid: String?,
    @SerializedName("app_set_id")
    val appSetId: String?,
    @SerializedName("android_id")
    val androidId: String?,
    @SerializedName("os")
    val os: String,
    @SerializedName("os_major")
    val osMajor: String,
    @SerializedName("device_brand")
    val deviceBrand: String,
    @SerializedName("device_model")
    val deviceModel: String,
    @SerializedName("screen_w")
    val screenW: Int,
    @SerializedName("screen_h")
    val screenH: Int,
    @SerializedName("screen_dpr")
    val screenDpr: Float,
    @SerializedName("timezone")
    val timezone: String,
    @SerializedName("locale")
    val locale: String,
    @SerializedName("client_time")
    val clientTime: String,
    @SerializedName("install_time")
    val installTime: String,
)
