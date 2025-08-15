package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallDto(
    @SerializedName("paywall_name")
    val name: String,
    variationId: String,
    @SerializedName("paywall_id")
    val id: String,
    placement: Placement,
    @SerializedName("products")
    val products: ArrayList<ProductDto>,
    remoteConfig: RemoteConfigDto?,
    weight: Int,
    @SerializedName("paywall_builder")
    val paywallBuilder: Map<String, Any>?,
    crossPlacementInfo: CrossPlacementInfo?,
    snapshotAt: Long,
): Variation(variationId, placement, remoteConfig, weight, crossPlacementInfo, snapshotAt)