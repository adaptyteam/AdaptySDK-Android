package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductDto(
    @SerializedName("adapty_product_id")
    val id: String?,
    @SerializedName("vendor_product_id")
    val vendorProductId: String?,
    @SerializedName("paywall_product_index")
    val paywallProductIndex: Int,
    @SerializedName("is_consumable")
    val isConsumable: Boolean?,
    @SerializedName("base_plan_id")
    val basePlanId: String?,
    @SerializedName("offer_id")
    val offerId: String?,
    @SerializedName("timestamp")
    val timestamp: Long?,
)