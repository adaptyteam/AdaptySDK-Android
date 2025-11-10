package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.ProductPALMappings
import com.adapty.internal.data.models.ProfileDto
import com.adapty.models.AdaptyProfile
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProfileMapper(
    private val metaInfoRetriever: MetaInfoRetriever,
) {

    @JvmSynthetic
    fun map(dto: ProfileDto) =
        AdaptyProfile(
            profileId = dto.profileId,
            customerUserId = dto.customerUserId,
            nonSubscriptions = dto.nonSubscriptions?.mapValues { entry ->
                entry.value.map { nonSub -> mapNonSub(nonSub) }.immutableWithInterop()
            }.orEmpty().immutableWithInterop(),
            accessLevels = dto.accessLevels?.mapValues { (key, value) ->
                mapPAL(key, value)
            }.orEmpty().immutableWithInterop(),
            subscriptions = dto.subscriptions?.mapValues { (_, sub) ->
                mapSub(sub)
            }.orEmpty().immutableWithInterop(),
            customAttributes = dto.customAttributes.orEmpty().immutableWithInterop(),
            isTestUser = dto.isTestUser ?: false,
        )

    fun map(dto: ProfileDto, localPALItem: ProductPALMappings.ItemExtended?) : AdaptyProfile {
        var localPAL: AdaptyProfile.AccessLevel? = null

        val accessLevels = dto.accessLevels?.mapValues { (key, value) ->
            mapPAL(key, value)
        }.run {
            localPALItem ?: return@run this

            val accessLevelId = localPALItem.accessLevelId
            val expiresAt = localPALItem.endTimestamp.takeIfNotInf()?.formatDate()
            val accessLevelIfExists = this?.get(accessLevelId)
            if (accessLevelIfExists?.isActive == true) {
                if (accessLevelIfExists.expiresAt == null)
                    return@run this
                if (expiresAt != null && expiresAt <= accessLevelIfExists.expiresAt)
                    return@run this
            }
            (this?.toMutableMap() ?: mutableMapOf())
                .apply {
                    put(
                        accessLevelId,
                        mapPAL(accessLevelId, localPALItem, expiresAt) .also { localPAL = it }
                    )
                }
        }

        val subscriptions = dto.subscriptions?.mapValues { (_, sub) ->
            mapSub(sub)
        }.run {
            val localPALItem = localPALItem ?: return@run this
            val localPAL = localPAL ?: return@run this

            (this?.toMutableMap() ?: mutableMapOf())
                .apply {
                    put(
                        localPAL.vendorProductId,
                        mapSub(localPAL, localPALItem)
                    )
                }
        }

        return AdaptyProfile(
            profileId = dto.profileId,
            customerUserId = dto.customerUserId,
            nonSubscriptions = dto.nonSubscriptions?.mapValues { entry ->
                entry.value.map { nonSub -> mapNonSub(nonSub) }.immutableWithInterop()
            }.orEmpty().immutableWithInterop(),
            accessLevels = accessLevels.orEmpty().immutableWithInterop(),
            subscriptions = subscriptions.orEmpty().immutableWithInterop(),
            customAttributes = dto.customAttributes.orEmpty().immutableWithInterop(),
            isTestUser = dto.isTestUser ?: false,
        )
    }

    private val dateFormatter: DateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000'Z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    private fun Long.takeIfNotInf() = takeIf { this != Long.MAX_VALUE }

    private fun Long.formatDate() = dateFormatter.format(Date(this))

    private fun mapPAL(id: String, value: ProfileDto.AccessLevelDto) =
        AdaptyProfile.AccessLevel(
            id = id,
            isActive = value.isActive ?: throw AdaptyError(
                message = "isActive in AccessLevel should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            vendorProductId = value.vendorProductId.orEmpty(),
            offerId = value.offerId,
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

    private fun mapPAL(id: String, localPALItem: ProductPALMappings.ItemExtended, expiresAt: String?) =
        AdaptyProfile.AccessLevel(
            id = id,
            isActive = true,
            vendorProductId = combinedProductId(localPALItem.vendorProductId, localPALItem.basePlanId),
            offerId = localPALItem.offerId,
            store = metaInfoRetriever.store,
            activatedAt = localPALItem.purchase.purchaseTime.formatDate(),
            startsAt = null,
            renewedAt = null,
            expiresAt = expiresAt,
            isLifetime = localPALItem.isLifetime,
            cancellationReason = null,
            isRefund = false,
            activeIntroductoryOfferType = null,
            activePromotionalOfferType = null,
            activePromotionalOfferId = null,
            willRenew = localPALItem.willRenew,
            isInGracePeriod = false,
            unsubscribedAt = null,
            billingIssueDetectedAt = null
        )

    private fun mapSub(sub: ProfileDto.SubscriptionDto) =
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
            offerId = sub.offerId,
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

    private fun mapSub(localPAL: AdaptyProfile.AccessLevel, localPALItem: ProductPALMappings.ItemExtended) =
        AdaptyProfile.Subscription(
            isActive = localPAL.isActive,
            vendorProductId = localPAL.vendorProductId,
            vendorTransactionId = localPALItem.transactionId,
            vendorOriginalTransactionId = localPALItem.transactionId,
            offerId = localPAL.offerId,
            store = localPAL.store,
            activatedAt = localPAL.activatedAt,
            startsAt = localPAL.startsAt,
            renewedAt = localPAL.renewedAt,
            expiresAt = localPAL.expiresAt,
            isLifetime = localPAL.isLifetime,
            cancellationReason = localPAL.cancellationReason,
            isSandbox = false,
            isRefund = localPAL.isRefund,
            activeIntroductoryOfferType = localPAL.activeIntroductoryOfferType,
            activePromotionalOfferType = localPAL.activePromotionalOfferType,
            activePromotionalOfferId = localPAL.activePromotionalOfferId,
            willRenew = localPAL.willRenew,
            isInGracePeriod = localPAL.isInGracePeriod,
            unsubscribedAt = localPAL.unsubscribedAt,
            billingIssueDetectedAt = localPAL.billingIssueDetectedAt,
        )

    private fun mapNonSub(nonSub: ProfileDto.NonSubscriptionDto) =
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
            isConsumable = nonSub.isConsumable ?: false,
            isSandbox = nonSub.isSandbox ?: false,
            isRefund = nonSub.isRefund ?: false
        )
}