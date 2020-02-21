package com.adapty.api.entity.syncmeta

import com.google.gson.annotations.SerializedName

open class AttributeSyncMetaReq {
    @SerializedName("adaptySdkVersion")
    var adaptySdkVersion: String? = null

    @SerializedName("adaptySdkVersionBuild")
    var adaptySdkVersionBuild: Int? = null

    @SerializedName("appBuild")
    var appBuild: String? = null

    @SerializedName("appVersion")
    var appVersion: String? = null

    @SerializedName("device")
    var device: String? = null

    @SerializedName("locale")
    var locale: String? = null

    @SerializedName("os")
    var os: String? = null

    @SerializedName("platform")
    var platform: String? = null

    @SerializedName("timezone")
    var timezone: String? = null

    @SerializedName("attributionNetwork")
    var attributionNetwork: String? = null

    @SerializedName("attributionCampaign")
    var attributionCampaign: String? = null

    @SerializedName("attributionTrackerToken")
    var attributionTrackerToken: String? = null

    @SerializedName("attributionTrackerName")
    var attributionTrackerName: String? = null

    @SerializedName("attributionAdgroup")
    var attributionAdgroup: String? = null

    @SerializedName("attributionCreative")
    var attributionCreative: String? = null

    @SerializedName("attributionClickLabel")
    var attributionClickLabel: String? = null

    @SerializedName("attributionAdid")
    var attributionAdid: String? = null
}