package com.adapty.models

import com.adapty.utils.ImmutableList
import com.adapty.utils.ImmutableMap

public class AdaptyProfile(
    public val profileId: String,
    public val customerUserId: String?,
    public val accessLevels: ImmutableMap<String, AccessLevel>,
    public val subscriptions: ImmutableMap<String, Subscription>,
    public val nonSubscriptions: ImmutableMap<String, ImmutableList<NonSubscription>>,
    public val customAttributes: ImmutableMap<String, Any>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyProfile

        if (profileId != other.profileId) return false
        if (customerUserId != other.customerUserId) return false
        if (accessLevels != other.accessLevels) return false
        if (subscriptions != other.subscriptions) return false
        if (nonSubscriptions != other.nonSubscriptions) return false
        if (customAttributes != other.customAttributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profileId.hashCode()
        result = 31 * result + (customerUserId?.hashCode() ?: 0)
        result = 31 * result + accessLevels.hashCode()
        result = 31 * result + subscriptions.hashCode()
        result = 31 * result + nonSubscriptions.hashCode()
        result = 31 * result + customAttributes.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyProfile(profileId=$profileId, customerUserId=$customerUserId, accessLevels=$accessLevels, subscriptions=$subscriptions, nonSubscriptions=$nonSubscriptions, customAttributes=$customAttributes)"
    }

    public class AccessLevel(
        public val id: String,
        public val isActive: Boolean,
        public val vendorProductId: String,
        public val store: String,
        public val activatedAt: String,
        public val startsAt: String?,
        public val renewedAt: String?,
        public val expiresAt: String?,
        public val isLifetime: Boolean,
        public val cancellationReason: String?,
        public val isRefund: Boolean,
        public val activeIntroductoryOfferType: String?,
        public val activePromotionalOfferType: String?,
        public val activePromotionalOfferId: String?,
        public val willRenew: Boolean,
        public val isInGracePeriod: Boolean,
        public val unsubscribedAt: String?,
        public val billingIssueDetectedAt: String?
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AccessLevel

            if (id != other.id) return false
            if (isActive != other.isActive) return false
            if (vendorProductId != other.vendorProductId) return false
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
            if (activePromotionalOfferId != other.activePromotionalOfferId) return false
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
            result = 31 * result + store.hashCode()
            result = 31 * result + activatedAt.hashCode()
            result = 31 * result + (startsAt?.hashCode() ?: 0)
            result = 31 * result + (renewedAt?.hashCode() ?: 0)
            result = 31 * result + (expiresAt?.hashCode() ?: 0)
            result = 31 * result + isLifetime.hashCode()
            result = 31 * result + (cancellationReason?.hashCode() ?: 0)
            result = 31 * result + isRefund.hashCode()
            result = 31 * result + (activeIntroductoryOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferId?.hashCode() ?: 0)
            result = 31 * result + willRenew.hashCode()
            result = 31 * result + isInGracePeriod.hashCode()
            result = 31 * result + (unsubscribedAt?.hashCode() ?: 0)
            result = 31 * result + (billingIssueDetectedAt?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "AccessLevel(id=$id, isActive=$isActive, vendorProductId=$vendorProductId, store=$store, activatedAt=$activatedAt, startsAt=$startsAt, renewedAt=$renewedAt, expiresAt=$expiresAt, isLifetime=$isLifetime, cancellationReason=$cancellationReason, isRefund=$isRefund, activeIntroductoryOfferType=$activeIntroductoryOfferType, activePromotionalOfferType=$activePromotionalOfferType, activePromotionalOfferId=$activePromotionalOfferId, willRenew=$willRenew, isInGracePeriod=$isInGracePeriod, unsubscribedAt=$unsubscribedAt, billingIssueDetectedAt=$billingIssueDetectedAt)"
        }
    }

    public class Subscription(
        public val isActive: Boolean,
        public val vendorProductId: String,
        public val vendorTransactionId: String?,
        public val vendorOriginalTransactionId: String?,
        public val store: String,
        public val activatedAt: String,
        public val renewedAt: String?,
        public val expiresAt: String?,
        public val startsAt: String?,
        public val isLifetime: Boolean,
        public val activeIntroductoryOfferType: String?,
        public val activePromotionalOfferType: String?,
        public val activePromotionalOfferId: String?,
        public val willRenew: Boolean,
        public val isInGracePeriod: Boolean,
        public val unsubscribedAt: String?,
        public val billingIssueDetectedAt: String?,
        public val isSandbox: Boolean,
        public val isRefund: Boolean,
        public val cancellationReason: String?
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Subscription

            if (isActive != other.isActive) return false
            if (vendorProductId != other.vendorProductId) return false
            if (vendorTransactionId != other.vendorTransactionId) return false
            if (vendorOriginalTransactionId != other.vendorOriginalTransactionId) return false
            if (store != other.store) return false
            if (activatedAt != other.activatedAt) return false
            if (renewedAt != other.renewedAt) return false
            if (expiresAt != other.expiresAt) return false
            if (startsAt != other.startsAt) return false
            if (isLifetime != other.isLifetime) return false
            if (activeIntroductoryOfferType != other.activeIntroductoryOfferType) return false
            if (activePromotionalOfferType != other.activePromotionalOfferType) return false
            if (activePromotionalOfferId != other.activePromotionalOfferId) return false
            if (willRenew != other.willRenew) return false
            if (isInGracePeriod != other.isInGracePeriod) return false
            if (unsubscribedAt != other.unsubscribedAt) return false
            if (billingIssueDetectedAt != other.billingIssueDetectedAt) return false
            if (isSandbox != other.isSandbox) return false
            if (isRefund != other.isRefund) return false
            if (cancellationReason != other.cancellationReason) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isActive.hashCode()
            result = 31 * result + vendorProductId.hashCode()
            result = 31 * result + (vendorTransactionId?.hashCode() ?: 0)
            result = 31 * result + (vendorOriginalTransactionId?.hashCode() ?: 0)
            result = 31 * result + store.hashCode()
            result = 31 * result + activatedAt.hashCode()
            result = 31 * result + (renewedAt?.hashCode() ?: 0)
            result = 31 * result + (expiresAt?.hashCode() ?: 0)
            result = 31 * result + (startsAt?.hashCode() ?: 0)
            result = 31 * result + isLifetime.hashCode()
            result = 31 * result + (activeIntroductoryOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferId?.hashCode() ?: 0)
            result = 31 * result + willRenew.hashCode()
            result = 31 * result + isInGracePeriod.hashCode()
            result = 31 * result + (unsubscribedAt?.hashCode() ?: 0)
            result = 31 * result + (billingIssueDetectedAt?.hashCode() ?: 0)
            result = 31 * result + isSandbox.hashCode()
            result = 31 * result + isRefund.hashCode()
            result = 31 * result + (cancellationReason?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Subscription(isActive=$isActive, vendorProductId='$vendorProductId', vendorTransactionId=$vendorTransactionId, vendorOriginalTransactionId=$vendorOriginalTransactionId, store='$store', activatedAt=$activatedAt, renewedAt=$renewedAt, expiresAt=$expiresAt, startsAt=$startsAt, isLifetime=$isLifetime, activeIntroductoryOfferType=$activeIntroductoryOfferType, activePromotionalOfferType=$activePromotionalOfferType, activePromotionalOfferId=$activePromotionalOfferId, willRenew=$willRenew, isInGracePeriod=$isInGracePeriod, unsubscribedAt=$unsubscribedAt, billingIssueDetectedAt=$billingIssueDetectedAt, isSandbox=$isSandbox, isRefund=$isRefund, cancellationReason=$cancellationReason)"
        }
    }

    public class NonSubscription(
        public val purchaseId: String,
        public val vendorProductId: String,
        public val vendorTransactionId: String?,
        public val store: String,
        public val purchasedAt: String,
        public val isOneTime: Boolean,
        public val isSandbox: Boolean,
        public val isRefund: Boolean
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NonSubscription

            if (purchaseId != other.purchaseId) return false
            if (vendorProductId != other.vendorProductId) return false
            if (vendorTransactionId != other.vendorTransactionId) return false
            if (store != other.store) return false
            if (purchasedAt != other.purchasedAt) return false
            if (isOneTime != other.isOneTime) return false
            if (isSandbox != other.isSandbox) return false
            if (isRefund != other.isRefund) return false

            return true
        }

        override fun hashCode(): Int {
            var result = purchaseId.hashCode()
            result = 31 * result + vendorProductId.hashCode()
            result = 31 * result + (vendorTransactionId?.hashCode() ?: 0)
            result = 31 * result + store.hashCode()
            result = 31 * result + purchasedAt.hashCode()
            result = 31 * result + isOneTime.hashCode()
            result = 31 * result + isSandbox.hashCode()
            result = 31 * result + isRefund.hashCode()
            return result
        }

        override fun toString(): String {
            return "NonSubscription(purchaseId='$purchaseId', vendorProductId='$vendorProductId', vendorTransactionId=$vendorTransactionId, store='$store', purchasedAt=$purchasedAt, isOneTime=$isOneTime, isSandbox=$isSandbox, isRefund=$isRefund)"
        }
    }

    public enum class Gender {
        MALE, FEMALE, OTHER;

        override fun toString(): String {
            return this.name[0].toLowerCase().toString()
        }
    }

    /**
     * @param year,
     * @param month,
     * @param date correspond to their ordinal numbers in a regular calendar,
     * month and date numbers start with 1.
     * For example, Date(year = 1970, month = 1, date = 3) represents January 3, 1970.
     */
    public class Date(
        private val year: Int,
        private val month: Int,
        private val date: Int
    ) {
        override fun toString(): String {
            return "$year-${String.format("%02d", month)}-${String.format("%02d", date)}"
        }
    }
}