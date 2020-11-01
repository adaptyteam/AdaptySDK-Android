package com.adapty.api.entity.purchaserInfo.model

data class SubscriptionInfoModel(
    var isActive: Boolean?,
    var vendorProductId: String?,
    var store: String?,
    var activatedAt: String?,
    var renewedAt: String?,
    var expiresAt: String?,
    var startsAt: String?,
    var isLifetime: Boolean?,
    var activeIntroductoryOfferType: String?,
    var activePromotionalOfferType: String?,
    var willRenew: Boolean?,
    var isInGracePeriod: Boolean?,
    var unsubscribedAt: String?,
    var billingIssueDetectedAt: String?,
    var isSandbox: Boolean?,
    var isRefund: Boolean?,
    var cancellationReason: String?
)