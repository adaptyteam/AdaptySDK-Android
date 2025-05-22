package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Onboarding(
    @SerializedName("onboarding_name")
    val name: String,
    variationId: String,
    @SerializedName("onboarding_id")
    val id: String,
    placement: Placement,
    remoteConfig: RemoteConfigDto?,
    weight: Int,
    @SerializedName("onboarding_builder")
    val viewConfig: OnboardingBuilder,
    crossPlacementInfo: CrossPlacementInfo?,
    snapshotAt: Long,
): Variation(variationId, placement, remoteConfig, weight, crossPlacementInfo, snapshotAt)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class OnboardingBuilder(
    @SerializedName("config_url")
    val url: String,
    @SerializedName("lang")
    val lang: String,
)