package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProfileResponseData(
    @SerializedName("id")
    val id: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("attributes")
    val attributes: Attributes?
) {
    internal class Attributes(
        @SerializedName("profile_id")
        override val profileId: String?,
        @SerializedName("customer_user_id")
        override val customerUserId: String?,
        @SerializedName("paid_access_levels")
        override val accessLevels: HashMap<String, AccessLevelInfo>?,
        @SerializedName("subscriptions")
        override val subscriptions: HashMap<String, SubscriptionsInfo>?,
        @SerializedName("non_subscriptions")
        override val nonSubscriptions: HashMap<String, ArrayList<NonSubscriptionsInfo>>?,
    ) : ContainsPurchaserInfo {

        override fun extractPurchaserInfo(): Attributes = this

        internal class AccessLevelInfo(
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
            @SerializedName("will_renew")
            val willRenew: Boolean?,
            @SerializedName("is_in_grace_period")
            val isInGracePeriod: Boolean?,
            @SerializedName("unsubscribed_at")
            val unsubscribedAt: String?,
            @SerializedName("billing_issue_detected_at")
            val billingIssueDetectedAt: String?,
        )

        internal class SubscriptionsInfo(
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
        )

        internal class NonSubscriptionsInfo(
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
            @SerializedName("is_one_time")
            val isOneTime: Boolean?,
            @SerializedName("is_sandbox")
            val isSandbox: Boolean?,
            @SerializedName("is_refund")
            val isRefund: Boolean?,
        )
    }
}