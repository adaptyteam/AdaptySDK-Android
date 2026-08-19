@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.models.FlowDto
import com.adapty.internal.data.models.Variation
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.VariationPicker
import com.adapty.internal.utils.unlockQuietly
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import java.util.concurrent.locks.ReentrantReadWriteLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class VariationDrawer(
    private val cacheRepository: CacheRepository,
    private val variationPicker: VariationPicker,
    private val analyticsTracker: AnalyticsTracker,
    private val crossPlacementInfoLock: ReentrantReadWriteLock,
    private val cloudGateway: PlacementCloudGateway,
) {

    private val checkpointHolder = FetchCheckpointHolder()

    fun extractSingleVariation(
        variations: Collection<Variation>,
        profileId: String,
        placementId: String,
        locale: String,
        placementSource: PlacementSource,
        variationType: VariationType,
    ): Variation {
        if (variations.isEmpty()) {
            val message = "Variation couldn't be found: empty list"
            Logger.log(ERROR) { message }
            throw AdaptyError(
                message = message,
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }

        val participatesInCrossPlacement = variations.any { !it.crossPlacementInfo?.placementWithVariationMap.isNullOrEmpty() }

        if (!participatesInCrossPlacement) {
            if (variations.size == 1) {
                return variations.first()
                    .also { variation ->
                        sendVariationAssignedEvent(variation, variationType)
                    }
            }

            val variation = pickVariation(variations, profileId)

            if (variation != null) {
                sendVariationAssignedEvent(variation, variationType)
                return variation
            } else {
                val message = "Variation couldn't be found"
                Logger.log(ERROR) { message }
                throw AdaptyError(
                    message = message,
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
        }

        cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)?.let { pinnedVariationId ->
            return resolvePinnedVariation(pinnedVariationId, variations, placementId, locale, placementSource, variationType)
        }

        crossPlacementInfoLock.writeLock().lock()
        val cachedCrossPlacementInfo = cacheRepository.getCrossPlacementInfo()
        cachedCrossPlacementInfo?.placementWithVariationMap?.get(placementId)?.let { pinnedVariationId ->
            crossPlacementInfoLock.writeLock().unlockQuietly()
            return resolvePinnedVariation(pinnedVariationId, variations, placementId, locale, placementSource, variationType)
        }

        val variation = if (variations.size == 1) variations.first() else pickVariation(variations, profileId)
        if (variation == null) {
            crossPlacementInfoLock.writeLock().unlockQuietly()
            val message = "Variation couldn't be found"
            Logger.log(ERROR) { message }
            throw AdaptyError(
                message = message,
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }
        val crossPlacementInfoFromVariation = variation.crossPlacementInfo
        crossPlacementInfoFromVariation?.placementWithVariationMap?.takeIf { it.isNotEmpty() }?.let {
            if (cachedCrossPlacementInfo != null && cachedCrossPlacementInfo.placementWithVariationMap.isEmpty())
                cacheRepository.saveCrossPlacementInfoFromPaywall(crossPlacementInfoFromVariation.copy(version = cachedCrossPlacementInfo.version))
        }
        if (placementSource is PlacementSource.Regular)
            checkpointHolder.getAndUpdate(placementSource.placementRequestId, CheckPoint.VariationAssigned(variation.variationId))
        crossPlacementInfoLock.writeLock().unlockQuietly()
        sendVariationAssignedEvent(variation, variationType)
        return variation
    }

    private fun resolvePinnedVariation(
        pinnedVariationId: String,
        variations: Collection<Variation>,
        placementId: String,
        locale: String,
        placementSource: PlacementSource,
        variationType: VariationType,
    ): Variation {
        if (placementSource is PlacementSource.Regular)
            checkpointHolder.getAndUpdate(placementSource.placementRequestId, CheckPoint.VariationAssigned(pinnedVariationId))
        val variation = variations.firstOrNull { it.variationId == pinnedVariationId }
            ?: when (placementSource) {
                is PlacementSource.Regular -> cloudGateway.fetchVariationById(placementId, locale, pinnedVariationId, variationType)
                else -> cloudGateway.fetchFallbackVariationById(placementId, locale, pinnedVariationId, variationType)
            }
        sendVariationAssignedEvent(variation, variationType)
        return variation
    }

    private fun pickVariation(
        variations: Collection<Variation>,
        profileId: String,
    ): Variation? {
        return variationPicker.pick(variations, profileId)
    }

    fun recordAssignedVariation(placementRequestId: String, variationId: String) {
        checkpointHolder.getAndUpdate(placementRequestId, CheckPoint.VariationAssigned(variationId))
    }

    fun markTimedOutAndGetAssigned(placementRequestId: String): String? =
        (checkpointHolder.getAndUpdate(placementRequestId, CheckPoint.TimeOut) as? CheckPoint.VariationAssigned)?.variationId

    fun clearCheckpoint(placementRequestId: String) {
        checkpointHolder.getAndUpdate(placementRequestId, CheckPoint.Unspecified)
    }

    fun sendVariationAssignedEvent(variation: Variation, variationType: VariationType) {
        analyticsTracker.trackEvent(
            when (variationType) {
                VariationType.Onboarding -> "onboarding_variation_assigned"
                VariationType.Flow -> "flow_variation_assigned"
            },
            mutableMapOf<String, Any>(
                "placement_audience_version_id" to variation.placement.placementAudienceVersionId,
                "variation_id" to variation.variationId,
            ).apply {
                (variation as? FlowDto)?.versionId?.let { versionId ->
                    put("flow_version_id", versionId)
                }
            },
        )
    }
}
