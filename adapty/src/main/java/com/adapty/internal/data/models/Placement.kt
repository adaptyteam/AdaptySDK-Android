package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class Placement(
    @SerializedName("developer_id")
    val id: String,
    @SerializedName("ab_test_name")
    val abTestName: String,
    @SerializedName("audience_name")
    val audienceName: String,
    @SerializedName("revision")
    val revision: Int,
    @SerializedName("placement_audience_version_id")
    val placementAudienceVersionId: String,
    @SerializedName("is_tracking_purchases")
    val isTrackingPurchases: Boolean?,
)