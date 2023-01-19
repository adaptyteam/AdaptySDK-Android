package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FallbackPaywalls(
    @SerializedName("paywalls")
    val paywalls: ArrayList<PaywallDto>,
    @SerializedName("products")
    val products: ArrayList<ProductDto>,
    @SerializedName("version")
    val version: Int,
)