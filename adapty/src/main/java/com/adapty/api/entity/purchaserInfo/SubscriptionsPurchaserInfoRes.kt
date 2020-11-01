package com.adapty.api.entity.purchaserInfo

import com.google.gson.annotations.SerializedName

class SubscriptionsPurchaserInfoRes {

    @SerializedName("is_active")
    var isActive: Boolean? = null

    @SerializedName("vendor_product_id")
    var vendorProductId: String? = null

    @SerializedName("vendor_transaction_id")
    var vendorTransactionId: String? = null

    @SerializedName("vendor_original_transaction_id")
    var vendorOriginalTransactionId: String? = null

    @SerializedName("store")
    var store: String? = null

    @SerializedName("activated_at")
    var activatedAt: String? = null

    @SerializedName("renewed_at")
    var renewedAt: String? = null

    @SerializedName("expires_at")
    var expiresAt: String? = null

    @SerializedName("starts_at")
    var startsAt: String? = null

    @SerializedName("is_lifetime")
    var isLifetime: Boolean? = null

    @SerializedName("active_introductory_offer_type")
    var activeIntroductoryOfferType: String? = null

    @SerializedName("active_promotional_offer_type")
    var activePromotionalOfferType: String? = null

    @SerializedName("will_renew")
    var willRenew: Boolean? = null

    @SerializedName("is_in_grace_period")
    var isInGracePeriod: Boolean? = null

    @SerializedName("unsubscribed_at")
    var unsubscribedAt: String? = null

    @SerializedName("billing_issue_detected_at")
    var billingIssueDetectedAt: String? = null

    @SerializedName("is_sandbox")
    var isSandbox: Boolean? = null

    @SerializedName("is_refund")
    var isRefund: Boolean? = null

    @SerializedName("cancellation_reason")
    var cancellationReason: String? = null
}