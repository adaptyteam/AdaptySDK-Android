package com.adapty.api.entity.syncmeta

import com.google.gson.annotations.SerializedName

open class AttributeSyncMetaReq {
    @SerializedName("adapty_sdk_version")
    var adaptySdkVersion: String? = null

    @SerializedName("adapty_sdk_version_build")
    var adaptySdkVersionBuild: Int? = null

    @SerializedName("app_build")
    var appBuild: String? = null

    @SerializedName("app_version")
    var appVersion: String? = null

    @SerializedName("device")
    var device: String? = null

    @SerializedName("device_token")
    var deviceToken: String? = null

    @SerializedName("locale")
    var locale: String? = null

    @SerializedName("os")
    var os: String? = null

    @SerializedName("platform")
    var platform: String? = null

    @SerializedName("timezone")
    var timezone: String? = null

    @SerializedName("attribution_network")
    var attributionNetwork: String? = null

    @SerializedName("attribution_campaign")
    var attributionCampaign: String? = null

    @SerializedName("attribution_tracker_token")
    var attributionTrackerToken: String? = null

    @SerializedName("attribution_tracker_name")
    var attributionTrackerName: String? = null

    @SerializedName("attribution_adgroup")
    var attributionAdgroup: String? = null

    @SerializedName("attribution_creative")
    var attributionCreative: String? = null

    @SerializedName("attribution_click_label")
    var attributionClickLabel: String? = null

    @SerializedName("attribution_adid")
    var attributionAdid: String? = null

    @SerializedName("advertising_id")
    var advertisingId: String? = null
}