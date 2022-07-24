package com.adapty.models

public class AccessLevelInfoModel(
    public val id: String,
    public val isActive: Boolean,
    public val vendorProductId: String,
    public val vendorTransactionId: String?,
    public val vendorOriginalTransactionId: String?,
    public val store: String,
    public val activatedAt: String?,
    public val startsAt: String?,
    public val renewedAt: String?,
    public val expiresAt: String?,
    public val isLifetime: Boolean,
    public val cancellationReason: String?,
    public val isRefund: Boolean,
    public val activeIntroductoryOfferType: String?,
    public val activePromotionalOfferType: String?,
    public val willRenew: Boolean,
    public val isInGracePeriod: Boolean,
    public val unsubscribedAt: String?,
    public val billingIssueDetectedAt: String?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccessLevelInfoModel

        if (id != other.id) return false
        if (isActive != other.isActive) return false
        if (vendorProductId != other.vendorProductId) return false
        if (vendorTransactionId != other.vendorTransactionId) return false
        if (vendorOriginalTransactionId != other.vendorOriginalTransactionId) return false
        if (store != other.store) return false
        if (activatedAt != other.activatedAt) return false
        if (startsAt != other.startsAt) return false
        if (renewedAt != other.renewedAt) return false
        if (expiresAt != other.expiresAt) return false
        if (isLifetime != other.isLifetime) return false
        if (cancellationReason != other.cancellationReason) return false
        if (isRefund != other.isRefund) return false
        if (activeIntroductoryOfferType != other.activeIntroductoryOfferType) return false
        if (activePromotionalOfferType != other.activePromotionalOfferType) return false
        if (willRenew != other.willRenew) return false
        if (isInGracePeriod != other.isInGracePeriod) return false
        if (unsubscribedAt != other.unsubscribedAt) return false
        if (billingIssueDetectedAt != other.billingIssueDetectedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + vendorProductId.hashCode()
        result = 31 * result + (vendorTransactionId?.hashCode() ?: 0)
        result = 31 * result + (vendorOriginalTransactionId?.hashCode() ?: 0)
        result = 31 * result + store.hashCode()
        result = 31 * result + (activatedAt?.hashCode() ?: 0)
        result = 31 * result + (startsAt?.hashCode() ?: 0)
        result = 31 * result + (renewedAt?.hashCode() ?: 0)
        result = 31 * result + (expiresAt?.hashCode() ?: 0)
        result = 31 * result + isLifetime.hashCode()
        result = 31 * result + (cancellationReason?.hashCode() ?: 0)
        result = 31 * result + isRefund.hashCode()
        result = 31 * result + (activeIntroductoryOfferType?.hashCode() ?: 0)
        result = 31 * result + (activePromotionalOfferType?.hashCode() ?: 0)
        result = 31 * result + willRenew.hashCode()
        result = 31 * result + isInGracePeriod.hashCode()
        result = 31 * result + (unsubscribedAt?.hashCode() ?: 0)
        result = 31 * result + (billingIssueDetectedAt?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "AccessLevelInfoModel(id=$id, isActive=$isActive, vendorProductId=$vendorProductId, vendorTransactionId=$vendorTransactionId, vendorOriginalTransactionId=$vendorOriginalTransactionId, store=$store, activatedAt=$activatedAt, startsAt=$startsAt, renewedAt=$renewedAt, expiresAt=$expiresAt, isLifetime=$isLifetime, cancellationReason=$cancellationReason, isRefund=$isRefund, activeIntroductoryOfferType=$activeIntroductoryOfferType, activePromotionalOfferType=$activePromotionalOfferType, willRenew=$willRenew, isInGracePeriod=$isInGracePeriod, unsubscribedAt=$unsubscribedAt, billingIssueDetectedAt=$billingIssueDetectedAt)"
    }
}