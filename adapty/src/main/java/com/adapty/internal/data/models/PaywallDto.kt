package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class PaywallDto(
    @SerializedName("developer_id")
    val developerId: String?,
    @SerializedName("paywall_name")
    val name: String?,
    @SerializedName("ab_test_name")
    val abTestName: String?,
    @SerializedName("audience_name")
    val audienceName: String?,
    @SerializedName("revision")
    val revision: Int?,
    @SerializedName("variation_id")
    val variationId: String?,
    @SerializedName("paywall_id")
    val paywallId: String?,
    @SerializedName("products")
    val products: ArrayList<ProductDto>,
    @SerializedName("remote_config")
    val remoteConfig: RemoteConfigDto?,
    @SerializedName("placement_audience_version_id")
    val placementAudienceVersionId: String?,
    @SerializedName("weight")
    val weight: Int?,
    @SerializedName("paywall_builder")
    val paywallBuilder: Map<String, Any>?,
    val snapshotAt: Long?,
)