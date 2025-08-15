@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.Request
import com.adapty.internal.data.models.BackendError.Companion.INCORRECT_SEGMENT_HASH_ERROR
import com.adapty.internal.data.models.Onboarding
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.Variation
import com.adapty.internal.data.models.Variations
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
import com.adapty.internal.utils.DEFAULT_RETRY_COUNT
import com.adapty.internal.utils.INF_PAYWALL_TIMEOUT_MILLIS
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.LifecycleManager
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.PAYWALL_TIMEOUT_MILLIS_SHIFT
import com.adapty.internal.utils.VariationPicker
import com.adapty.internal.utils.generateUuid
import com.adapty.internal.utils.orDefault
import com.adapty.internal.utils.retryIfNecessary
import com.adapty.internal.utils.timeout
import com.adapty.internal.utils.unlockQuietly
import com.adapty.models.AdaptyPlacementFetchPolicy
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BasePlacementFetcher(
    private val authInteractor: AuthInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val lifecycleManager: LifecycleManager,
    private val variationPicker: VariationPicker,
    private val analyticsTracker: AnalyticsTracker,
    private val crossPlacementInfoLock: ReentrantReadWriteLock,
) {

    fun fetchPaywall(id: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<PaywallDto> {
        val variationType = VariationType.Paywall
        return getPaywallInternal(
            fetchPolicy = fetchPolicy,
            fetchFromCloud = { getPaywallFromCloud(id, locale, loadTimeout, generateUuid(), variationType) },
            fetchFromCache = {
                val maxAgeMillis = (fetchPolicy as? AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, variationType, maxAgeMillis)
            }
        ).filterVariationByTypeOrError { "current variation is not a paywall" }
    }

    fun fetchPaywallUntargeted(id: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<PaywallDto> {
        val variationType = VariationType.Paywall
        return getPaywallInternal(
            fetchPolicy = fetchPolicy,
            fetchFromCloud = { getPaywallUntargetedFromCloud(id, locale, variationType) },
            fetchFromCache = {
                val maxAgeMillis = (fetchPolicy as? AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, variationType, maxAgeMillis)
            }
        )
            .filterVariationByTypeOrError { "current variation is not a paywall" }
    }

    fun fetchOnboarding(id: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<Onboarding> {
        val variationType = VariationType.Onboarding
        return getPaywallInternal(
            fetchPolicy = fetchPolicy,
            fetchFromCloud = { getPaywallFromCloud(id, locale, loadTimeout, generateUuid(), variationType) },
            fetchFromCache = {
                val maxAgeMillis = (fetchPolicy as? AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, variationType, maxAgeMillis)
            }
        ).filterVariationByTypeOrError { "current variation is not an onboarding" }
    }

    fun fetchOnboardingUntargeted(id: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<Onboarding> {
        val variationType = VariationType.Onboarding
        return getPaywallInternal(
            fetchPolicy = fetchPolicy,
            fetchFromCloud = { getPaywallUntargetedFromCloud(id, locale, variationType) },
            fetchFromCache = {
                val maxAgeMillis = (fetchPolicy as? AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, variationType, maxAgeMillis)
            }
        )
            .filterVariationByTypeOrError { "current variation is not an onboarding" }
    }

    private inline fun <reified T: Variation> Flow<Variation>.filterVariationByTypeOrError(crossinline errorMessage: () -> String): Flow<T> =
        map { variation ->
            variation as? T ?: throw AdaptyError(
                message = errorMessage(),
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )
        }
            .filterIsInstance()

    private fun getPaywallInternal(
        fetchPolicy: AdaptyPlacementFetchPolicy,
        fetchFromCloud: () -> Flow<Variation>,
        fetchFromCache: () -> Flow<Variation?>,
    ): Flow<Variation> {
        return when (fetchPolicy) {
            is AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData -> fetchFromCloud()
            else -> {
                fetchFromCache()
                    .flatMapConcat { paywall ->
                        if (paywall != null) {
                            flowOf(paywall)
                        } else {
                            fetchFromCloud()
                        }
                    }
            }
        }
    }

    private fun getPaywallFromCloud(placementId: String, locale: String, loadTimeout: Int, placementRequestId: String, variationType: VariationType): Flow<Variation> {
        val placementCloudSource = PlacementCloudSource.Regular(placementRequestId)

        val baseFlow = authInteractor.runWhenAuthDataSynced(
            call = {
                syncPurchasesIfNeeded()
                    .map {
                        getPaywallOrVariationsFromCloud(placementId, locale, placementCloudSource, variationType)
                    }
            },
            switchIfProfileCreationFailed = {
                getLocalFallbackEntities(placementId)?.let { entities ->
                    val profileId = cacheRepository.getProfileId()
                    runCatching { extractSingleVariation(entities, profileId, placementId, locale, PlacementCloudSource.Fallback, variationType) }.getOrNull()
                        ?.let { fallbackPaywall -> flowOf(fallbackPaywall) }
                }
            }
        ).flattenConcat()

        return if (loadTimeout == INF_PAYWALL_TIMEOUT_MILLIS) {
            baseFlow
        } else {
            timeout(baseFlow, loadTimeout - PAYWALL_TIMEOUT_MILLIS_SHIFT)
                .map { result ->
                    result ?: kotlin.run {
                        val prevCheckpoint = checkpointHolder.getAndUpdate(placementCloudSource.placementRequestId, CheckPoint.TimeOut)
                        if (prevCheckpoint is CheckPoint.VariationAssigned)
                            getRemoteFallbackEntityByVariationId(placementId, locale, prevCheckpoint.variationId, variationType)
                                .also { paywall -> saveEntityToCache(placementId, paywall) }
                        else
                            getPaywallOrVariationsFallbackFromCloud(placementId, locale, variationType)
                    }
                }
        }
            .handleFetchPaywallError(placementId, locale, placementCloudSource, variationType)
    }

    private fun getPaywallOrVariationsFromCloud(
        placementId: String,
        locale: String,
        placementCloudSource: PlacementCloudSource.Regular,
        variationType: VariationType,
    ): Variation {
        val crossPlacementVariationId =
            cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
        if (crossPlacementVariationId != null) {
            checkpointHolder.getAndUpdate(placementCloudSource.placementRequestId, CheckPoint.VariationAssigned(crossPlacementVariationId))
            return getPaywallByVariationId(placementId, locale, crossPlacementVariationId, variationType)
                .also { paywall ->
                    saveEntityToCache(placementId, paywall)
                    sendVariationAssignedEvent(paywall, variationType)
                }
        }

        var profile = cacheRepository.getProfile() ?: cloudRepository.getProfile().first
        val responseData: Pair<Variations, Request.CurrentDataWhenSent?>
        val segmentId = profile.segmentId
        try {
            responseData = cloudRepository.getVariations(placementId, locale, segmentId, variationType)
        } catch (error: Throwable) {
            val isIncorrectSegmentHash = error is AdaptyError && error.backendError != null
                    && error.backendError.containsErrorCode(INCORRECT_SEGMENT_HASH_ERROR)
            if (!isIncorrectSegmentHash)
                throw error
            val cachedProfile = cacheRepository.getProfile()
            if (cachedProfile != null && segmentId != cachedProfile.segmentId)
                return getPaywallOrVariationsFromCloud(placementId, locale, placementCloudSource, variationType)
            profile = cloudRepository.getProfile().first
            if (segmentId == profile.segmentId)
                throw error
            return getPaywallOrVariationsFromCloud(placementId, locale, placementCloudSource, variationType)
        }

        val (variations, requestDataWhenSent) = responseData

        if (requestDataWhenSent?.profileId != cacheRepository.getProfileId())
            throw AdaptyError(
                message = "Profile was changed!",
                adaptyErrorCode = AdaptyErrorCode.PROFILE_WAS_CHANGED
            )

        val cachedPaywall = getEntityFromCache(placementId, locale, variationType)
        return if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt) {
            cachedPaywall
        } else {
            val variation = extractSingleVariation(
                variations.data,
                profile.profileId,
                placementId,
                locale,
                placementCloudSource,
                variationType,
            )

            variation
                .also { paywall -> saveEntityToCache(placementId, paywall) }
        }
    }

    private fun getPaywallOrVariationsFallbackFromCloud(
        placementId: String,
        locale: String,
        variationType: VariationType,
    ): Variation {
        val variations = cloudRepository.getVariationsFallback(placementId, locale, variationType)
        val cachedPaywall = getEntityFromCache(placementId, locale, variationType)
        return if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt) {
            cachedPaywall
        } else {
            val profileId = cacheRepository.getProfileId()
            val variation = extractSingleVariation(variations.data, profileId, placementId, locale, PlacementCloudSource.Fallback, variationType)
            variation
                .also { paywall -> saveEntityToCache(placementId, paywall) }
        }
    }

    private fun getPaywallByVariationId(
        placementId: String,
        locale: String,
        variationId: String,
        variationType: VariationType,
    ): Variation {
        var profile = cacheRepository.getProfile() ?: cloudRepository.getProfile().first
        val segmentId = profile.segmentId
        try {
            return cloudRepository.getVariationById(placementId, locale, segmentId, variationId, variationType)
        } catch (error: Throwable) {
            val isIncorrectSegmentHash = error is AdaptyError && error.backendError != null
                    && error.backendError.containsErrorCode(INCORRECT_SEGMENT_HASH_ERROR)
            if (!isIncorrectSegmentHash)
                throw error
            val cachedProfile = cacheRepository.getProfile()
            if (cachedProfile != null && segmentId != cachedProfile.segmentId)
                return getPaywallByVariationId(placementId, locale, variationId, variationType)
            profile = cloudRepository.getProfile().first
            if (segmentId == profile.segmentId)
                throw error
            return getPaywallByVariationId(placementId, locale, variationId, variationType)
        }
    }

    private fun getRemoteFallbackEntityByVariationId(placementId: String, locale: String, variationId: String, variationType: VariationType): Variation {
        return cloudRepository.getVariationByIdFallback(placementId, locale, variationId, variationType)
    }

    private fun pickVariation(
        variations: Collection<Variation>,
        profileId: String,
    ): Variation? {
        return variationPicker.pick(variations, profileId)
    }

    private fun extractSingleVariation(
        paywalls: Collection<Variation>,
        profileId: String,
        placementId: String,
        locale: String,
        placementCloudSource: PlacementCloudSource,
        variationType: VariationType,
    ): Variation {
        if (paywalls.isEmpty()) {
            val message = "Paywall couldn't be found: empty list"
            Logger.log(ERROR) { message }
            throw AdaptyError(
                message = message,
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }

        val participatesInCrossPlacement = paywalls.any { !it.crossPlacementInfo?.placementWithVariationMap.isNullOrEmpty() }

        if (!participatesInCrossPlacement) {
            if (paywalls.size == 1) {
                return paywalls.first()
                    .also { paywall ->
                        sendVariationAssignedEvent(paywall, variationType)
                    }
            }

            val paywall = pickVariation(paywalls, profileId)

            if (paywall != null) {
                sendVariationAssignedEvent(paywall, variationType)
                return paywall
            } else {
                val message = "Paywall couldn't be found"
                Logger.log(ERROR) { message }
                throw AdaptyError(
                    message = message,
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                )
            }
        }

        val crossPlacementVariationId = cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
        if (crossPlacementVariationId != null) {
            if (placementCloudSource is PlacementCloudSource.Regular)
                checkpointHolder.getAndUpdate(placementCloudSource.placementRequestId, CheckPoint.VariationAssigned(crossPlacementVariationId))
            val paywall = paywalls.firstOrNull { it.variationId == crossPlacementVariationId }
                ?: kotlin.run {
                    when (placementCloudSource) {
                        is PlacementCloudSource.Regular -> getPaywallByVariationId(placementId, locale, crossPlacementVariationId, variationType)
                        else -> getRemoteFallbackEntityByVariationId(placementId, locale, crossPlacementVariationId, variationType)
                    }
                }
            sendVariationAssignedEvent(paywall, variationType)
            return paywall
        } else {
            crossPlacementInfoLock.writeLock().lock()
            val cachedCrossPlacementInfo = cacheRepository.getCrossPlacementInfo()
            val crossPlacementVariationMap = cachedCrossPlacementInfo?.placementWithVariationMap
            val crossPlacementVariationId = crossPlacementVariationMap?.get(placementId)
            if (crossPlacementVariationId != null) {
                crossPlacementInfoLock.writeLock().unlockQuietly()
                if (placementCloudSource is PlacementCloudSource.Regular)
                    checkpointHolder.getAndUpdate(placementCloudSource.placementRequestId, CheckPoint.VariationAssigned(crossPlacementVariationId))
                val paywall = paywalls.firstOrNull { it.variationId == crossPlacementVariationId }
                    ?: kotlin.run {
                        when (placementCloudSource) {
                            is PlacementCloudSource.Regular -> getPaywallByVariationId(placementId, locale, crossPlacementVariationId, variationType)
                            else -> getRemoteFallbackEntityByVariationId(placementId, locale, crossPlacementVariationId, variationType)
                        }
                    }
                sendVariationAssignedEvent(paywall, variationType)
                return paywall
            } else {
                val paywall = if (paywalls.size == 1) paywalls.first() else pickVariation(paywalls, profileId)
                if (paywall != null) {
                    val crossPlacementInfoFroPaywall = paywall.crossPlacementInfo
                    crossPlacementInfoFroPaywall?.placementWithVariationMap?.takeIf { it.isNotEmpty() }?.let { paywallCrossPlacementInfo ->
                        if (cachedCrossPlacementInfo != null && cachedCrossPlacementInfo.placementWithVariationMap.isEmpty())
                            cacheRepository.saveCrossPlacementInfoFromPaywall(crossPlacementInfoFroPaywall.copy(version = cachedCrossPlacementInfo.version))
                    }
                    val placementRequestId = (placementCloudSource as? PlacementCloudSource.Regular)?.placementRequestId
                    if (placementRequestId != null)
                        checkpointHolder.getAndUpdate(placementRequestId, CheckPoint.VariationAssigned(paywall.variationId))
                    crossPlacementInfoLock.writeLock().unlockQuietly()
                    sendVariationAssignedEvent(paywall, variationType)
                    return paywall
                } else {
                    crossPlacementInfoLock.writeLock().unlockQuietly()
                    val message = "Paywall couldn't be found"
                    Logger.log(ERROR) { message }
                    throw AdaptyError(
                        message = message,
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }
            }
        }
    }

    private fun sendVariationAssignedEvent(paywall: Variation, variationType: VariationType) {
        analyticsTracker.trackEvent(
            when (variationType) {
                VariationType.Paywall -> "paywall_variation_assigned"
                VariationType.Onboarding -> "onboarding_variation_assigned"
            },
            mutableMapOf<String, Any>(
                "placement_audience_version_id" to paywall.placement.placementAudienceVersionId,
                "variation_id" to paywall.variationId,
            ),
        )
    }

    private fun Flow<Variation>.handleFetchPaywallError(id: String, locale: String, placementCloudSource: PlacementCloudSource, variationType: VariationType) =
        catch { error ->
            if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                val cachedPaywall = getEntityFromCache(id, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType)
                val chosenPaywall =
                    if (cachedPaywall == null) {
                        val fallbackEntities = getLocalFallbackEntities(id) ?: throw error
                        val profileId = cacheRepository.getProfileId()
                        runCatching { extractSingleVariation(fallbackEntities, profileId, id, locale, placementCloudSource, variationType) }.getOrNull()
                    } else {
                        val desiredVariationIdIfExists = (placementCloudSource as? PlacementCloudSource.Regular)?.placementRequestId?.let { placementRequestId ->
                            (checkpointHolder.get(placementRequestId) as? CheckPoint.VariationAssigned)?.variationId
                        }
                        val cacheVariationIsNotWrong = desiredVariationIdIfExists?.let { it == cachedPaywall.variationId } ?: true
                        val cacheSnapshotAtIsNotOlder = cachedPaywall.snapshotAt >= cacheRepository.getFallbackPaywallsSnapshotAt().orDefault()

                        if (cacheVariationIsNotWrong && cacheSnapshotAtIsNotOlder)
                            cachedPaywall
                        else {
                            if (desiredVariationIdIfExists != null) {
                                val fallbackEntities = getLocalFallbackEntities(id)

                                val desiredFallbackVariation = fallbackEntities?.firstOrNull { it.variationId == desiredVariationIdIfExists }

                                if (desiredFallbackVariation != null) {
                                    desiredFallbackVariation
                                } else {
                                    val fallbackPaywall = fallbackEntities
                                        ?.let { entities ->
                                            val profileId = cacheRepository.getProfileId()
                                            runCatching { extractSingleVariation(entities, profileId, id, locale, placementCloudSource, variationType) }.getOrNull()
                                        }

                                    when {
                                        fallbackPaywall != null && fallbackPaywall.variationId == desiredVariationIdIfExists -> fallbackPaywall
                                        else -> cachedPaywall
                                    }
                                }
                            } else {
                                val fallbackPaywall = getLocalFallbackEntities(id)
                                    ?.let { entities ->
                                        val profileId = cacheRepository.getProfileId()
                                        runCatching { extractSingleVariation(entities, profileId, id, locale, placementCloudSource, variationType) }.getOrNull()
                                    }

                                fallbackPaywall ?: cachedPaywall
                            }
                        }
                    } ?: throw error
                emit(chosenPaywall)
            } else {
                throw error
            }
        }

    private fun getLocalFallbackEntities(placementId: String): List<Variation>? {
        return cacheRepository.getPaywallVariationsFallback(placementId)?.data
    }

    private fun getPaywallUntargetedFromCloud(id: String, locale: String, variationType: VariationType): Flow<Variation> =
        lifecycleManager
            .onActivateAllowed()
            .mapLatest {
                val variations = cloudRepository.getVariationsUntargeted(id, locale, variationType)
                val cachedPaywall = getEntityFromCache(id, locale, variationType)
                val paywall = if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt) {
                    cachedPaywall
                } else {
                    val profileId = cacheRepository.getProfileId()
                    val variation = extractSingleVariation(variations.data, profileId, id, locale, PlacementCloudSource.Untargeted, variationType)
                    variation
                        .also { paywall -> saveEntityToCache(id, paywall) }
                }
                paywall
            }
            .retryIfNecessary(DEFAULT_RETRY_COUNT)
            .handleFetchPaywallError(id, locale, PlacementCloudSource.Untargeted, variationType)

    private fun getPaywallFromCache(placementId: String, locale: String, variationType: VariationType, maxAgeMillis: Long?) =
        flow {
            val cachedPaywall = getEntityFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType, maxAgeMillis)
            emit(cachedPaywall)
        }

    private fun saveEntityToCache(placementId: String, entity: Variation) {
        cacheRepository.saveVariation(placementId, entity)
    }
    private fun getEntityFromCache(placementId: String, locales: Set<String>, variationType: VariationType, maxAgeMillis: Long? = null): Variation? {
        return cacheRepository.getVariation(placementId, locales, variationType, maxAgeMillis)
    }
    private fun getEntityFromCache(placementId: String, locale: String, variationType: VariationType, maxAgeMillis: Long? = null): Variation? =
        getEntityFromCache(placementId, setOf(locale), variationType, maxAgeMillis)

    private suspend fun syncPurchasesIfNeeded() =
        purchasesInteractor
            .syncPurchasesIfNeeded()
            .map { true }
            .catch { emit(false) }

    private val checkpointHolder = FetchPaywallCheckpointHolder()
}

private class FetchPaywallCheckpointHolder {
    private val checkpoints = HashMap<String, CheckPoint>()

    private val lock = ReentrantReadWriteLock()

    fun getAndUpdate(requestId: String, checkPoint: CheckPoint): CheckPoint {
        return try {
            lock.writeLock().lock()
            val prevCheckpoint = checkpoints[requestId] ?: CheckPoint.Unspecified
            when (checkPoint) {
                is CheckPoint.Unspecified -> checkpoints.remove(requestId)
                is CheckPoint.TimeOut -> checkpoints[requestId] = checkPoint
                is CheckPoint.VariationAssigned -> checkpoints[requestId] = checkPoint
            }
            prevCheckpoint
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun get(requestId: String): CheckPoint {
        return try {
            lock.readLock().lock()
            checkpoints[requestId] ?: CheckPoint.Unspecified
        } finally {
            lock.readLock().unlock()
        }
    }
}

private sealed class CheckPoint {
    object Unspecified: CheckPoint()
    class VariationAssigned(val variationId: String): CheckPoint()
    object TimeOut: CheckPoint()
}

private sealed class PlacementCloudSource {
    class Regular(val placementRequestId: String): PlacementCloudSource()
    object Fallback: PlacementCloudSource()
    object Untargeted: PlacementCloudSource()
}

internal enum class VariationType {
    Paywall, Onboarding
}