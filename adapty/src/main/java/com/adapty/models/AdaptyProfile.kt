package com.adapty.models

import com.adapty.utils.ImmutableList
import com.adapty.utils.ImmutableMap

/**
 * @property[accessLevels] The keys are access level identifiers configured by you in Adapty Dashboard.
 * The values are Can be null if the customer has no access levels.
 * @property[customAttributes] Previously set user custom attributes with [.updateProfile][com.adapty.Adapty.updateProfile] method.
 * @property[customerUserId] An identifier of a user in your system.
 * @property[nonSubscriptions] The keys are product ids from the store. The values are lists of
 * information about consumables.
 * @property[profileId] An identifier of a user in Adapty.
 * @property[subscriptions] The keys are product ids from a store. The values are information about subscriptions.
 */
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

    /**
     * @property[activatedAt] ISO 8601 datetime when this access level was activated.
     * @property[activeIntroductoryOfferType] A type of an active introductory offer.
     * If the value is not null, it means that the offer was applied during the current
     * subscription period.
     * @property[activePromotionalOfferId] An id of active promotional offer.
     * @property[activePromotionalOfferType] A type of an active promotional offer. If the value is
     * not null, it means that the offer was applied during the current subscription period.
     * @property[cancellationReason] A reason why a subscription was cancelled.
     * @property[expiresAt] ISO 8601 datetime when the access level will expire (could be in the past and could be null for lifetime access).
     * @property[id] Unique identifier of the access level configured by you in Adapty Dashboard.
     * @property[isActive] `true` if this access level is active. Generally, you can check this property
     * to determine whether a user has an access to premium features.
     * @property[isInGracePeriod] `true` if this auto-renewable subscription is in the grace period.
     * @property[isLifetime] `true` if this access level is active for a lifetime (no expiration date).
     * @property[isRefund] `true` if this purchase was refunded.
     * @property[offerId] An [identifier][AdaptyProductSubscriptionDetails.offerId] of a discount offer in Google Play that unlocked this access level.
     * @property[renewedAt] ISO 8601 datetime when the access level was renewed. It can be null if the purchase was
     * first in chain or it is non-renewing subscription / non-consumable (e.g. lifetime).
     * @property[startsAt] ISO 8601 datetime when this access level has started (could be in the future).
     * @property[store] A store of the purchase that unlocked this access level.
     * @property[unsubscribedAt] ISO 8601 datetime when the auto-renewable subscription was cancelled.
     * Subscription can still be active, it just means that auto-renewal turned off.
     * Will be set to null if the user reactivates the subscription.
     * @property[vendorProductId] An identifier of a product in a store that unlocked this access level.
     * It may contain either [product_id][AdaptyPaywallProduct.vendorProductId] only or "[product_id][AdaptyPaywallProduct.vendorProductId]:[base_plan_id][AdaptyProductSubscriptionDetails.basePlanId]".
     * @property[willRenew] `true` if this auto-renewable subscription is set to renew.
     */
    public class AccessLevel(
        public val id: String,
        public val isActive: Boolean,
        public val vendorProductId: String,
        public val offerId: String?,
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
            if (offerId != other.offerId) return false
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
            result = 31 * result + (offerId?.hashCode() ?: 0)
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
            return "AccessLevel(id=$id, isActive=$isActive, vendorProductId=$vendorProductId, offerId=$offerId, store=$store, activatedAt=$activatedAt, startsAt=$startsAt, renewedAt=$renewedAt, expiresAt=$expiresAt, isLifetime=$isLifetime, cancellationReason=$cancellationReason, isRefund=$isRefund, activeIntroductoryOfferType=$activeIntroductoryOfferType, activePromotionalOfferType=$activePromotionalOfferType, activePromotionalOfferId=$activePromotionalOfferId, willRenew=$willRenew, isInGracePeriod=$isInGracePeriod, unsubscribedAt=$unsubscribedAt, billingIssueDetectedAt=$billingIssueDetectedAt)"
        }
    }

    /**
     * @property[activatedAt] ISO 8601 datetime when the subscription was activated.
     * @property[activeIntroductoryOfferType] A type of an active introductory offer. If the value is
     * not null, it means that the offer was applied during the current subscription period.
     * @property[activePromotionalOfferId] An id of active promotional offer.
     * @property[activePromotionalOfferType] A type of an active promotional offer. If the value is
     * not null, it means that the offer was applied during the current subscription period.
     * @property[billingIssueDetectedAt] ISO 8601 datetime when a billing issue was detected. Subscription can still be active.
     * @property[cancellationReason] A reason why a subscription was cancelled.
     * @property[expiresAt] ISO 8601 datetime when the access level will expire (could be in the past and could be null for lifetime access).
     * @property[isActive] `true` if the subscription is active.
     * @property[isInGracePeriod] Whether the auto-renewable subscription is in a grace period.
     * @property[isLifetime] `true` if the subscription is active for a lifetime (no expiration date).
     * @property[isRefund] `true` if the purchase was refunded.
     * @property[isSandbox] `true` if the product was purchased in a sandbox environment.
     * @property[offerId] An [identifier][AdaptyProductSubscriptionDetails.offerId] of a discount offer in Google Play that unlocked this subscription.
     * @property[renewedAt] ISO 8601 datetime when the subscription was renewed. It can be null if the purchase
     * was first in chain or it is non-renewing subscription.
     * @property[startsAt] ISO 8601 datetime when the subscription has started (could be in the future).
     * @property[store] A store of the purchase.
     * @property[unsubscribedAt] ISO 8601 datetime when the auto-renewable subscription was cancelled.
     * Subscription can still be active, it means that auto-renewal is turned off. Would be null
     * if a user reactivates the subscription.
     * @property[vendorOriginalTransactionId] An original transaction id of the purchase in a store
     * that unlocked this subscription. For auto-renewable subscription, this will be an id of the
     * first transaction in this subscription.
     * @property[vendorProductId] An identifier of a product in a store that unlocked this subscription.
     * It may contain either [product_id][AdaptyPaywallProduct.vendorProductId] only or "[product_id][AdaptyPaywallProduct.vendorProductId]:[base_plan_id][AdaptyProductSubscriptionDetails.basePlanId]".
     * @property[vendorTransactionId] A transaction id of a purchase in a store that unlocked this subscription.
     * @property[willRenew] `true` if the auto-renewable subscription is set to renew.
     */
    public class Subscription(
        public val isActive: Boolean,
        public val vendorProductId: String,
        public val vendorTransactionId: String?,
        public val vendorOriginalTransactionId: String?,
        public val offerId: String?,
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
            if (offerId != other.offerId) return false
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
            result = 31 * result + (offerId?.hashCode() ?: 0)
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
            return "Subscription(isActive=$isActive, vendorProductId='$vendorProductId', vendorTransactionId=$vendorTransactionId, vendorOriginalTransactionId=$vendorOriginalTransactionId, offerId=$offerId, store='$store', activatedAt=$activatedAt, renewedAt=$renewedAt, expiresAt=$expiresAt, startsAt=$startsAt, isLifetime=$isLifetime, activeIntroductoryOfferType=$activeIntroductoryOfferType, activePromotionalOfferType=$activePromotionalOfferType, activePromotionalOfferId=$activePromotionalOfferId, willRenew=$willRenew, isInGracePeriod=$isInGracePeriod, unsubscribedAt=$unsubscribedAt, billingIssueDetectedAt=$billingIssueDetectedAt, isSandbox=$isSandbox, isRefund=$isRefund, cancellationReason=$cancellationReason)"
        }
    }

    /**
     * @property[isConsumable] `true` if the product is consumable.
     * @property[isRefund] `true` if the purchase was refunded.
     * @property[isSandbox] `true` if the product was purchased in a sandbox environment.
     * @property[purchaseId] An identifier of the purchase in Adapty. You can use it to ensure that
     * you’ve already processed this purchase (for example tracking one time products).
     * @property[purchasedAt] ISO 8601 datetime when the product was purchased.
     * @property[store] A store of the purchase.
     * @property[vendorProductId] An identifier of a product in a store that unlocked this subscription.
     * @property[vendorTransactionId] A transaction id of a purchase in a store that unlocked this subscription.
     */
    public class NonSubscription(
        public val purchaseId: String,
        public val vendorProductId: String,
        public val vendorTransactionId: String?,
        public val store: String,
        public val purchasedAt: String,
        public val isConsumable: Boolean,
        public val isSandbox: Boolean,
        public val isRefund: Boolean
    ) {

        @Deprecated(
            message = "This property is deprecated and will be removed in future releases",
            replaceWith = ReplaceWith("isConsumable"),
            level = DeprecationLevel.WARNING,
        )
        public val isOneTime: Boolean get() = isConsumable

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NonSubscription

            if (purchaseId != other.purchaseId) return false
            if (vendorProductId != other.vendorProductId) return false
            if (vendorTransactionId != other.vendorTransactionId) return false
            if (store != other.store) return false
            if (purchasedAt != other.purchasedAt) return false
            if (isConsumable != other.isConsumable) return false
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
            result = 31 * result + isConsumable.hashCode()
            result = 31 * result + isSandbox.hashCode()
            result = 31 * result + isRefund.hashCode()
            return result
        }

        override fun toString(): String {
            return "NonSubscription(purchaseId='$purchaseId', vendorProductId='$vendorProductId', vendorTransactionId=$vendorTransactionId, store='$store', purchasedAt=$purchasedAt, isConsumable=$isConsumable, isSandbox=$isSandbox, isRefund=$isRefund)"
        }
    }

    public enum class Gender {
        MALE, FEMALE, OTHER;

        override fun toString(): String {
            return this.name[0].lowercaseChar().toString()
        }
    }

    /**
     * The numbers are as in real life.
     * For example, `Date(year = 1970, month = 1, date = 3)` represents January 3, 1970.
     *
     * @param[year] Corresponds to its ordinal number in a regular calendar.
     * @param month Corresponds to its ordinal number in a regular calendar, i.e. starts with 1.
     * @param date Corresponds to its ordinal number in a regular calendar, i.e. starts with 1.
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