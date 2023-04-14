package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProfileDto(
    @SerializedName("profile_id")
    val profileId: String?,
    @SerializedName("customer_user_id")
    val customerUserId: String?,
    @SerializedName("paid_access_levels")
    val accessLevels: HashMap<String, AccessLevelDto>?,
    @SerializedName("subscriptions")
    val subscriptions: HashMap<String, SubscriptionDto>?,
    @SerializedName("non_subscriptions")
    val nonSubscriptions: HashMap<String, ArrayList<NonSubscriptionDto>>?,
    @SerializedName("custom_attributes")
    val customAttributes: HashMap<String, Any>?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileDto

        if (profileId != other.profileId) return false
        if (customerUserId != other.customerUserId) return false
        if (accessLevels != other.accessLevels) return false
        if (subscriptions != other.subscriptions) return false
        if (nonSubscriptions != other.nonSubscriptions) return false
        if (customAttributes != other.customAttributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profileId?.hashCode() ?: 0
        result = 31 * result + (customerUserId?.hashCode() ?: 0)
        result = 31 * result + (accessLevels?.hashCode() ?: 0)
        result = 31 * result + (subscriptions?.hashCode() ?: 0)
        result = 31 * result + (nonSubscriptions?.hashCode() ?: 0)
        result = 31 * result + (customAttributes?.hashCode() ?: 0)
        return result
    }

    internal class AccessLevelDto(
        @SerializedName("is_active")
        val isActive: Boolean?,
        @SerializedName("vendor_product_id")
        val vendorProductId: String?,
        @SerializedName("store")
        val store: String?,
        @SerializedName("activated_at")
        val activatedAt: String?,
        @SerializedName("starts_at")
        val startsAt: String?,
        @SerializedName("renewed_at")
        val renewedAt: String?,
        @SerializedName("expires_at")
        val expiresAt: String?,
        @SerializedName("is_lifetime")
        val isLifetime: Boolean?,
        @SerializedName("cancellation_reason")
        val cancellationReason: String?,
        @SerializedName("is_refund")
        val isRefund: Boolean?,
        @SerializedName("active_introductory_offer_type")
        val activeIntroductoryOfferType: String?,
        @SerializedName("active_promotional_offer_type")
        val activePromotionalOfferType: String?,
        @SerializedName("active_promotional_offer_id")
        val activePromotionalOfferId: String?,
        @SerializedName("will_renew")
        val willRenew: Boolean?,
        @SerializedName("is_in_grace_period")
        val isInGracePeriod: Boolean?,
        @SerializedName("unsubscribed_at")
        val unsubscribedAt: String?,
        @SerializedName("billing_issue_detected_at")
        val billingIssueDetectedAt: String?,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AccessLevelDto

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
            var result = isActive?.hashCode() ?: 0
            result = 31 * result + (vendorProductId?.hashCode() ?: 0)
            result = 31 * result + (store?.hashCode() ?: 0)
            result = 31 * result + (activatedAt?.hashCode() ?: 0)
            result = 31 * result + (startsAt?.hashCode() ?: 0)
            result = 31 * result + (renewedAt?.hashCode() ?: 0)
            result = 31 * result + (expiresAt?.hashCode() ?: 0)
            result = 31 * result + (isLifetime?.hashCode() ?: 0)
            result = 31 * result + (cancellationReason?.hashCode() ?: 0)
            result = 31 * result + (isRefund?.hashCode() ?: 0)
            result = 31 * result + (activeIntroductoryOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferId?.hashCode() ?: 0)
            result = 31 * result + (willRenew?.hashCode() ?: 0)
            result = 31 * result + (isInGracePeriod?.hashCode() ?: 0)
            result = 31 * result + (unsubscribedAt?.hashCode() ?: 0)
            result = 31 * result + (billingIssueDetectedAt?.hashCode() ?: 0)
            return result
        }
    }

    internal class SubscriptionDto(
        @SerializedName("is_active")
        val isActive: Boolean?,
        @SerializedName("vendor_product_id")
        val vendorProductId: String?,
        @SerializedName("vendor_transaction_id")
        val vendorTransactionId: String?,
        @SerializedName("vendor_original_transaction_id")
        val vendorOriginalTransactionId: String?,
        @SerializedName("store")
        val store: String?,
        @SerializedName("activated_at")
        val activatedAt: String?,
        @SerializedName("renewed_at")
        val renewedAt: String?,
        @SerializedName("expires_at")
        val expiresAt: String?,
        @SerializedName("starts_at")
        val startsAt: String?,
        @SerializedName("is_lifetime")
        val isLifetime: Boolean?,
        @SerializedName("active_introductory_offer_type")
        val activeIntroductoryOfferType: String?,
        @SerializedName("active_promotional_offer_type")
        val activePromotionalOfferType: String?,
        @SerializedName("active_promotional_offer_id")
        val activePromotionalOfferId: String?,
        @SerializedName("will_renew")
        val willRenew: Boolean?,
        @SerializedName("is_in_grace_period")
        val isInGracePeriod: Boolean?,
        @SerializedName("unsubscribed_at")
        val unsubscribedAt: String?,
        @SerializedName("billing_issue_detected_at")
        val billingIssueDetectedAt: String?,
        @SerializedName("is_sandbox")
        val isSandbox: Boolean?,
        @SerializedName("is_refund")
        val isRefund: Boolean?,
        @SerializedName("cancellation_reason")
        val cancellationReason: String?,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SubscriptionDto

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
            var result = isActive?.hashCode() ?: 0
            result = 31 * result + (vendorProductId?.hashCode() ?: 0)
            result = 31 * result + (vendorTransactionId?.hashCode() ?: 0)
            result = 31 * result + (vendorOriginalTransactionId?.hashCode() ?: 0)
            result = 31 * result + (store?.hashCode() ?: 0)
            result = 31 * result + (activatedAt?.hashCode() ?: 0)
            result = 31 * result + (renewedAt?.hashCode() ?: 0)
            result = 31 * result + (expiresAt?.hashCode() ?: 0)
            result = 31 * result + (startsAt?.hashCode() ?: 0)
            result = 31 * result + (isLifetime?.hashCode() ?: 0)
            result = 31 * result + (activeIntroductoryOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferType?.hashCode() ?: 0)
            result = 31 * result + (activePromotionalOfferId?.hashCode() ?: 0)
            result = 31 * result + (willRenew?.hashCode() ?: 0)
            result = 31 * result + (isInGracePeriod?.hashCode() ?: 0)
            result = 31 * result + (unsubscribedAt?.hashCode() ?: 0)
            result = 31 * result + (billingIssueDetectedAt?.hashCode() ?: 0)
            result = 31 * result + (isSandbox?.hashCode() ?: 0)
            result = 31 * result + (isRefund?.hashCode() ?: 0)
            result = 31 * result + (cancellationReason?.hashCode() ?: 0)
            return result
        }
    }

    internal class NonSubscriptionDto(
        @SerializedName("purchase_id")
        val purchaseId: String?,
        @SerializedName("vendor_product_id")
        val vendorProductId: String?,
        @SerializedName("vendor_transaction_id")
        val vendorTransactionId: String?,
        @SerializedName("store")
        val store: String?,
        @SerializedName("purchased_at")
        val purchasedAt: String?,
        @SerializedName("is_consumable")
        val isConsumable: Boolean?,
        @SerializedName("is_sandbox")
        val isSandbox: Boolean?,
        @SerializedName("is_refund")
        val isRefund: Boolean?,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NonSubscriptionDto

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
            var result = purchaseId?.hashCode() ?: 0
            result = 31 * result + (vendorProductId?.hashCode() ?: 0)
            result = 31 * result + (vendorTransactionId?.hashCode() ?: 0)
            result = 31 * result + (store?.hashCode() ?: 0)
            result = 31 * result + (purchasedAt?.hashCode() ?: 0)
            result = 31 * result + (isConsumable?.hashCode() ?: 0)
            result = 31 * result + (isSandbox?.hashCode() ?: 0)
            result = 31 * result + (isRefund?.hashCode() ?: 0)
            return result
        }
    }
}