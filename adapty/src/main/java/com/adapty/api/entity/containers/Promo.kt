package com.adapty.api.entity.containers

import com.google.gson.annotations.SerializedName

class Promo {
    @SerializedName("promo_type")
    var promoType: String? = null

    @SerializedName("variation_id")
    var variationId: String? = null

    @SerializedName("expires_at")
    var expiresAt: String? = null

    var paywall: Paywall? = null
}