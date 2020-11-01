package com.adapty.api.entity.paywalls

import com.google.gson.annotations.SerializedName

class PromoModel {
    @SerializedName("promo_type")
    var promoType: String? = null

    @SerializedName("variation_id")
    var variationId: String? = null

    @SerializedName("expires_at")
    var expiresAt: String? = null

    var paywall: PaywallModel? = null
}