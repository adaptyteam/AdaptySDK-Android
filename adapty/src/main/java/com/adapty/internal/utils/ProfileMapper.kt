package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.ProfileDto
import com.adapty.models.AdaptyProfile

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProfileMapper {

    @JvmSynthetic
    fun map(dto: ProfileDto) =
        AdaptyProfile(
            profileId = dto.profileId.orEmpty(),
            customerUserId = dto.customerUserId,
            nonSubscriptions = dto.nonSubscriptions?.mapValues { entry ->
                entry.value.map { nonSub ->
                    AdaptyProfile.NonSubscription(
                        purchaseId = nonSub.purchaseId ?: throw AdaptyError(
                            message = "purchaseId in NonSubscription should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        vendorProductId = nonSub.vendorProductId ?: throw AdaptyError(
                            message = "vendorProductId in NonSubscription should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        vendorTransactionId = nonSub.vendorTransactionId,
                        store = nonSub.store.orEmpty(),
                        purchasedAt = nonSub.purchasedAt ?: throw AdaptyError(
                            message = "purchasedAt in NonSubscription should not be null",
                            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                        ),
                        isOneTime = nonSub.isOneTime ?: false,
                        isSandbox = nonSub.isSandbox ?: false,
                        isRefund = nonSub.isRefund ?: false
                    )
                }.immutableWithInterop()
            }.orEmpty().immutableWithInterop(),
            accessLevels = dto.accessLevels?.mapValues { (key, value) ->
                AdaptyProfile.AccessLevel(
                    id = key,
                    isActive = value.isActive ?: throw AdaptyError(
                        message = "isActive in AccessLevel should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    vendorProductId = value.vendorProductId.orEmpty(),
                    store = value.store.orEmpty(),
                    activatedAt = value.activatedAt ?: throw AdaptyError(
                        message = "activatedAt in AccessLevel should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    startsAt = value.startsAt,
                    renewedAt = value.renewedAt,
                    expiresAt = value.expiresAt,
                    isLifetime = value.isLifetime ?: false,
                    cancellationReason = value.cancellationReason,
                    isRefund = value.isRefund ?: false,
                    activeIntroductoryOfferType = value.activeIntroductoryOfferType,
                    activePromotionalOfferType = value.activePromotionalOfferType,
                    activePromotionalOfferId = value.activePromotionalOfferId,
                    willRenew = value.willRenew ?: false,
                    isInGracePeriod = value.isInGracePeriod ?: false,
                    unsubscribedAt = value.unsubscribedAt,
                    billingIssueDetectedAt = value.billingIssueDetectedAt
                )
            }.orEmpty().immutableWithInterop(),
            subscriptions = dto.subscriptions?.mapValues { (_, sub) ->
                AdaptyProfile.Subscription(
                    isActive = sub.isActive ?: throw AdaptyError(
                        message = "isActive in Subscription should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    vendorProductId = sub.vendorProductId ?: throw AdaptyError(
                        message = "vendorProductId in Subscription should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    vendorTransactionId = sub.vendorTransactionId,
                    vendorOriginalTransactionId = sub.vendorOriginalTransactionId,
                    store = sub.store.orEmpty(),
                    activatedAt = sub.activatedAt ?: throw AdaptyError(
                        message = "activatedAt in Subscription should not be null",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    ),
                    renewedAt = sub.renewedAt,
                    expiresAt = sub.expiresAt,
                    startsAt = sub.startsAt,
                    isLifetime = sub.isLifetime ?: false,
                    activeIntroductoryOfferType = sub.activeIntroductoryOfferType,
                    activePromotionalOfferType = sub.activePromotionalOfferType,
                    activePromotionalOfferId = sub.activePromotionalOfferId,
                    willRenew = sub.willRenew ?: false,
                    isInGracePeriod = sub.isInGracePeriod ?: false,
                    unsubscribedAt = sub.unsubscribedAt,
                    billingIssueDetectedAt = sub.billingIssueDetectedAt,
                    isSandbox = sub.isSandbox ?: false,
                    isRefund = sub.isRefund ?: false,
                    cancellationReason = sub.cancellationReason
                )
            }.orEmpty().immutableWithInterop(),
            customAttributes = dto.customAttributes.orEmpty().immutableWithInterop()
        )
}