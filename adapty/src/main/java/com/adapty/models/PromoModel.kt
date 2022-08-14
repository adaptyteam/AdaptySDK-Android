package com.adapty.models

@Deprecated("The functionality is deprecated and will be removed in future releases.")
public class PromoModel(
    public val promoType: String,
    public val variationId: String,
    public val expiresAt: String?,
    public val paywall: PaywallModel?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PromoModel

        if (promoType != other.promoType) return false
        if (variationId != other.variationId) return false
        if (expiresAt != other.expiresAt) return false
        if (paywall != other.paywall) return false

        return true
    }

    override fun hashCode(): Int {
        var result = promoType.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        result = 31 * result + (paywall?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "PromoModel(promoType=$promoType, variationId=$variationId, expiresAt=$expiresAt, paywall=$paywall)"
    }
}