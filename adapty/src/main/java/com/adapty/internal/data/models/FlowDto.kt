package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlowDto(
    @SerializedName("flow_name")
    val name: String,
    variationId: String,
    @SerializedName("flow_id")
    val id: String,
    placement: Placement,
    @SerializedName("remote_configs")
    val remoteConfigs: List<RemoteConfigDto>?,
    weight: Int,
    @SerializedName("flow_version_id")
    val viewConfigurationId: String?,
    @SerializedName("variations")
    val paywalls: List<FlowPaywallDto>?,
    crossPlacementInfo: CrossPlacementInfo?,
    snapshotAt: Long,
): Variation(variationId, placement, null, weight, crossPlacementInfo, snapshotAt)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlowPaywallDto(
    @SerializedName("paywall_name")
    val name: String,
    @SerializedName("variation_id")
    val variationId: String,
    @SerializedName("paywall_id")
    val id: String,
    @SerializedName("products")
    val products: ArrayList<ProductDto>,
    @SerializedName("web_purchase_url")
    val webPurchaseUrl: String?,
)
