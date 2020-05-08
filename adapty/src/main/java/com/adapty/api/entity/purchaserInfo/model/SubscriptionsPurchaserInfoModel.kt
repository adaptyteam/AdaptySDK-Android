package com.adapty.api.entity.purchaserInfo.model

import com.google.gson.annotations.SerializedName

data class SubscriptionsPurchaserInfoModel(
    var isActive: Boolean?,
    var vendorProductId: String?,
    var store: String?,
    var referenceName: String?,
    var purchasedAt: String?,
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
    var isSandbox: Boolean?
)