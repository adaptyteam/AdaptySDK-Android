package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed class Variation(
    @SerializedName("variation_id")
    val variationId: String,
    @SerializedName("placement")
    val placement: Placement,
    @SerializedName("remote_config")
    val remoteConfig: RemoteConfigDto?,
    @SerializedName("weight")
    val weight: Int,
    @SerializedName("cross_placement_info")
    val crossPlacementInfo: CrossPlacementInfo?,
    @SerializedName("snapshot_at")
    val snapshotAt: Long,
)