package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallDto(
    @SerializedName("developer_id")
    val developerId: String?,
    @SerializedName("paywall_name")
    val name: String?,
    @SerializedName("ab_test_name")
    val abTestName: String?,
    @SerializedName("revision")
    val revision: Int?,
    @SerializedName("is_promo")
    val isPromo: Boolean?,
    @SerializedName("variation_id")
    val variationId: String?,
    @SerializedName("products")
    val products: ArrayList<ProductDto>?,
    @SerializedName("custom_payload")
    val customPayload: String?,
    @SerializedName("visual_paywall")
    val visualPaywall: String?,
)