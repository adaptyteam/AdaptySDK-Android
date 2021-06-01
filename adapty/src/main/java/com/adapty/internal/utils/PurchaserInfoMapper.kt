package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.ProfileResponseData.Attributes
import com.adapty.internal.data.models.responses.PurchaserInfoResponse
import com.adapty.internal.data.models.responses.RestoreReceiptResponse
import com.adapty.internal.data.models.responses.ValidateReceiptResponse
import com.adapty.models.AccessLevelInfoModel
import com.adapty.models.NonSubscriptionInfoModel
import com.adapty.models.PurchaserInfoModel
import com.adapty.models.SubscriptionInfoModel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PurchaserInfoMapper {

    @JvmSynthetic
    fun map(res: PurchaserInfoResponse) = res.data?.attributes?.let { map(it) }

    @JvmSynthetic
    fun map(res: RestoreReceiptResponse.Data.Attributes) = map(res.extractPurchaserInfo())

    @JvmSynthetic
    fun map(res: ValidateReceiptResponse.Data.Attributes) = map(res.extractPurchaserInfo())

    @JvmSynthetic
    fun map(res: Attributes) = PurchaserInfoModel(
        customerUserId = res.customerUserId,
        nonSubscriptions = res.nonSubscriptions?.mapValues { entry ->
            entry.value.map { nonSub ->
                NonSubscriptionInfoModel(
                    purchaseId = nonSub.purchaseId ?: throw AdaptyError(
                        message = "purchaseId in NonSubscriptionInfoModel should not be null",
                        adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                    ),
                    vendorProductId = nonSub.vendorProductId ?: throw AdaptyError(
                        message = "vendorProductId in NonSubscriptionInfoModel should not be null",
                        adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                    ),
                    vendorTransactionId = nonSub.vendorTransactionId,
                    store = nonSub.store ?: "",
                    purchasedAt = nonSub.purchasedAt,
                    isOneTime = nonSub.isOneTime ?: false,
                    isSandbox = nonSub.isSandbox ?: false,
                    isRefund = nonSub.isRefund ?: false
                )
            }
        } ?: mapOf(),
        accessLevels = res.accessLevels?.mapValues { (key, value) ->
            AccessLevelInfoModel(
                id = key,
                isActive = value.isActive ?: throw AdaptyError(
                    message = "isActive in AccessLevelInfoModel should not be null",
                    adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                ),
                vendorProductId = value.vendorProductId ?: "",
                vendorTransactionId = value.vendorTransactionId,
                vendorOriginalTransactionId = value.vendorOriginalTransactionId,
                store = value.store ?: "",
                activatedAt = value.activatedAt,
                startsAt = value.startsAt,
                renewedAt = value.renewedAt,
                expiresAt = value.expiresAt,
                isLifetime = value.isLifetime ?: false,
                cancellationReason = value.cancellationReason,
                isRefund = value.isRefund ?: false,
                activeIntroductoryOfferType = value.activeIntroductoryOfferType,
                activePromotionalOfferType = value.activePromotionalOfferType,
                willRenew = value.willRenew ?: false,
                isInGracePeriod = value.isInGracePeriod ?: false,
                unsubscribedAt = value.unsubscribedAt,
                billingIssueDetectedAt = value.billingIssueDetectedAt
            )
        } ?: mapOf(),
        subscriptions = res.subscriptions?.mapValues { (_, sub) ->
            SubscriptionInfoModel(
                isActive = sub.isActive ?: throw AdaptyError(
                    message = "isActive in SubscriptionInfoModel should not be null",
                    adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                ),
                vendorProductId = sub.vendorProductId ?: throw AdaptyError(
                    message = "vendorProductId in SubscriptionInfoModel should not be null",
                    adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
                ),
                vendorTransactionId = sub.vendorTransactionId,
                vendorOriginalTransactionId = sub.vendorOriginalTransactionId,
                store = sub.store ?: "",
                activatedAt = sub.activatedAt,
                renewedAt = sub.renewedAt,
                expiresAt = sub.expiresAt,
                startsAt = sub.startsAt,
                isLifetime = sub.isLifetime ?: false,
                activeIntroductoryOfferType = sub.activeIntroductoryOfferType,
                activePromotionalOfferType = sub.activePromotionalOfferType,
                willRenew = sub.willRenew ?: false,
                isInGracePeriod = sub.isInGracePeriod ?: false,
                unsubscribedAt = sub.unsubscribedAt,
                billingIssueDetectedAt = sub.billingIssueDetectedAt,
                isSandbox = sub.isSandbox ?: false,
                isRefund = sub.isRefund ?: false,
                cancellationReason = sub.cancellationReason
            )
        } ?: mapOf(),
    )
}