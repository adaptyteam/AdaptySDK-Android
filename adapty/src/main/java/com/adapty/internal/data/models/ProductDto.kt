package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductDto(
    @SerializedName("vendor_product_id")
    val vendorProductId: String?,
    @SerializedName("introductory_offer_eligibility")
    val introductoryOfferEligibility: Boolean?,
    @SerializedName("timestamp")
    val timestamp: Long?,
)