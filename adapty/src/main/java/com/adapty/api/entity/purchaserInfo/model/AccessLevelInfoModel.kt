package com.adapty.api.entity.purchaserInfo.model

data class AccessLevelInfoModel(
    var id: String?,
    var isActive: Boolean?,
    var vendorProductId: String?,
    var vendorTransactionId: String?,
    var vendorOriginalTransactionId: String?,
    var store: String?,
    var activatedAt: String?,
    var startsAt: String?,
    var renewedAt: String?,
    var expiresAt: String?,
    var isLifetime: Boolean?,
    var cancellationReason: String?,
    var isRefund: Boolean?,
    var activeIntroductoryOfferType: String?,
    var activePromotionalOfferType: String?,
    var willRenew: Boolean?,
    var isInGracePeriod: Boolean?,
    var unsubscribedAt: String?,
    var billingIssueDetectedAt: String?
)