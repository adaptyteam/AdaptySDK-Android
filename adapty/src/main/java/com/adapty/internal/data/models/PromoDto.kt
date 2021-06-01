package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PromoDto(
    @SerializedName("promo_type")
    val promoType: String?,
    @SerializedName("variation_id")
    val variationId: String?,
    @SerializedName("expires_at")
    val expiresAt: String?,
)