@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.models.FlowDto
import com.adapty.internal.data.models.Onboarding
import com.adapty.internal.data.models.Variation
import com.adapty.internal.utils.DEFAULT_PLACEMENT_LOCALE
import com.adapty.internal.utils.INF_PLACEMENT_TIMEOUT_MILLIS
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.LifecycleManager
import com.adapty.internal.utils.PLACEMENT_TIMEOUT_MILLIS_SHIFT
import com.adapty.internal.utils.generateUuid
import com.adapty.internal.utils.isBackendUnavailable
import com.adapty.internal.utils.isProfileWasChanged
import com.adapty.internal.utils.recoverOnBackendUnavailable
import com.adapty.internal.utils.recoverUnlessProfileChanged
import com.adapty.internal.utils.switchEndpointOnHostError
import com.adapty.internal.utils.timeout
import com.adapty.models.AdaptyPlacementFetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BasePlacementFetcher(
    private val authInteractor: AuthInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val lifecycleManager: LifecycleManager,
    private val cloudGateway: PlacementCloudGateway,
    private val drawer: VariationDrawer,
    private val localReader: PlacementLocalReader,
) {

    fun fetchOnboarding(id: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<Onboarding> =
        resolveWithFetchPolicy(id, locale, fetchPolicy, VariationType.Onboarding) {
            resolveVariationTargeted(id, locale, loadTimeout, generateUuid(), VariationType.Onboarding)
        }.filterVariationByTypeOrError { "current variation is not an onboarding" }

    fun fetchFlow(id: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<FlowDto> =
        resolveWithFetchPolicy(id, DEFAULT_PLACEMENT_LOCALE, fetchPolicy, VariationType.Flow) {
            resolveVariationTargeted(id, DEFAULT_PLACEMENT_LOCALE, loadTimeout, generateUuid(), VariationType.Flow)
        }.filterVariationByTypeOrError { "current variation is not a flow" }

    fun fetchFlowUntargeted(id: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<FlowDto> =
        resolveWithFetchPolicy(id, DEFAULT_PLACEMENT_LOCALE, fetchPolicy, VariationType.Flow) {
            resolveVariationUntargeted(id, DEFAULT_PLACEMENT_LOCALE, VariationType.Flow)
        }.filterVariationByTypeOrError { "current variation is not a flow" }

    fun fetchOnboardingUntargeted(id: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<Onboarding> =
        resolveWithFetchPolicy(id, locale, fetchPolicy, VariationType.Onboarding) {
            resolveVariationUntargeted(id, locale, VariationType.Onboarding)
        }.filterVariationByTypeOrError { "current variation is not an onboarding" }

    fun preloadFlow(id: String, loadTimeout: Int): Flow<Unit> =
        preloadFromCloud(id, DEFAULT_PLACEMENT_LOCALE, VariationType.Flow, loadTimeout)

    fun preloadOnboarding(id: String, locale: String, loadTimeout: Int): Flow<Unit> =
        preloadFromCloud(id, locale, VariationType.Onboarding, loadTimeout)

    fun preloadFlowUntargeted(id: String): Flow<Unit> =
        preloadUntargetedFromCloud(id, DEFAULT_PLACEMENT_LOCALE, VariationType.Flow)

    fun preloadOnboardingUntargeted(id: String, locale: String): Flow<Unit> =
        preloadUntargetedFromCloud(id, locale, VariationType.Onboarding)

    private fun preloadFromCloud(placementId: String, locale: String, variationType: VariationType, loadTimeout: Int): Flow<Unit> {
        val baseFlow = authInteractor.runWhenAuthDataSynced(
            call = {
                syncPurchasesIfNeeded()
                    .map {
                        persistVariationsFromCloud(placementId, locale, variationType)
                    }
            },
        ).flattenConcat()
            .switchEndpointOnHostError()

        return if (loadTimeout == INF_PLACEMENT_TIMEOUT_MILLIS) {
            baseFlow
        } else {
            timeout(baseFlow, loadTimeout - PLACEMENT_TIMEOUT_MILLIS_SHIFT)
        }
            .recoverOnBackendUnavailable {
                cloudGateway.fetchFallbackVariations(placementId, locale, variationType)
            }
    }

    private fun persistVariationsFromCloud(placementId: String, locale: String, variationType: VariationType) {
        val crossPlacementVariationId =
            cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
        if (crossPlacementVariationId != null) {
            cloudGateway.fetchVariationById(placementId, locale, crossPlacementVariationId, variationType)
            return
        }

        val (response, profile) = cloudGateway.fetchVariations(placementId, locale, variationType)

        if (response.request.currentDataWhenSent?.profileId != cacheRepository.getProfileId())
            throw AdaptyError(
                message = "Profile was changed!",
                adaptyErrorCode = AdaptyErrorCode.PROFILE_WAS_CHANGED
            )

        val variations = response.data
        response.rawBody?.let { rawBody ->
            cacheRepository.saveRawVariations(placementId, variationType, rawBody, locale, profile.segmentId, variations.snapshotAt)
        }
    }

    private fun preloadUntargetedFromCloud(id: String, locale: String, variationType: VariationType): Flow<Unit> =
        lifecycleManager
            .onActivateAllowed()
            .mapLatest {
                val response = cloudRepository.getVariationsUntargeted(id, locale, variationType)
                val rawBody = response.rawBody ?: return@mapLatest
                cacheRepository.saveRawVariations(id, variationType, rawBody, locale, null, response.data.snapshotAt)
            }

    private inline fun <reified T: Variation> Flow<Variation>.filterVariationByTypeOrError(crossinline errorMessage: () -> String): Flow<T> =
        map { variation ->
            variation as? T ?: throw AdaptyError(
                message = errorMessage(),
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED,
            )
        }
            .filterIsInstance()

    private fun resolveWithFetchPolicy(
        placementId: String,
        locale: String,
        fetchPolicy: AdaptyPlacementFetchPolicy,
        variationType: VariationType,
        fetchFromCloud: () -> Flow<Variation>,
    ): Flow<Variation> {
        val cacheFlow = when (fetchPolicy) {
            is AdaptyPlacementFetchPolicy.ReloadRevalidatingCacheData ->
                getPinnedVariationFromCache(placementId, locale, variationType)
            else -> {
                val maxAgeMillis = (fetchPolicy as? AdaptyPlacementFetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getVariationFromCache(placementId, locale, variationType, maxAgeMillis)
            }
        }
        return cacheFlow
            .flatMapConcat { variation ->
                if (variation != null) {
                    flowOf(variation)
                } else {
                    fetchFromCloud()
                }
            }
    }

    private fun resolveVariationTargeted(placementId: String, locale: String, loadTimeout: Int, placementRequestId: String, variationType: VariationType): Flow<Variation> {
        val placementSource = PlacementSource.Regular(placementRequestId)

        val baseFlow = authInteractor.runWhenAuthDataSynced(
            call = {
                syncPurchasesIfNeeded()
                    .map {
                        getVariationFromBackend(placementId, locale, placementSource, variationType)
                    }
            },
            switchIfProfileCreationFailed = {
                localReader.drawFromFallbackFileOrNull(placementId, locale, PlacementSource.Fallback.Local, variationType)
                    ?.let { fallbackVariation -> upgradeLocalFallbackFromServer(fallbackVariation, placementId, locale, variationType) }
            }
        ).flattenConcat()
            .switchEndpointOnHostError()

        return if (loadTimeout == INF_PLACEMENT_TIMEOUT_MILLIS) {
            baseFlow
        } else {
            timeout(baseFlow, loadTimeout - PLACEMENT_TIMEOUT_MILLIS_SHIFT)
        }
            .recoverUnlessProfileChanged { error ->
                resolveOnBackendError(placementId, locale, variationType, placementSource, error)
            }
            .onCompletion { drawer.clearCheckpoint(placementRequestId) }
    }

    private fun resolveOnBackendError(placementId: String, locale: String, variationType: VariationType, placementSource: PlacementSource.Regular, mainError: Throwable): Variation {
        val canUseFallbackServer = mainError.isBackendUnavailable()
        val assignedVariationId = drawer.markTimedOutAndGetAssigned(placementSource.placementRequestId)
        if (assignedVariationId != null) {
            return resolveVariationByIdOnBackendError(placementId, locale, assignedVariationId, variationType, canUseFallbackServer, mainError)
        }

        localReader.readVariationFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType)
            ?.let { return it }

        val fallbackServerResult = if (canUseFallbackServer) {
            runCatching { getVariationFromFallbackServer(placementId, locale, variationType) }
                .onSuccess { return it }
                .also { result -> result.exceptionOrNull()?.takeIf { it.isProfileWasChanged }?.let { throw it } }
        } else null

        localReader.drawFromFallbackFileOrNull(placementId, locale, PlacementSource.Fallback.Local, variationType)
            ?.let { return it }

        throw fallbackServerResult?.exceptionOrNull() ?: mainError
    }

    private fun resolveVariationByIdOnBackendError(placementId: String, locale: String, variationId: String, variationType: VariationType, canUseFallbackServer: Boolean, mainError: Throwable): Variation {
        localReader.readVariationByIdFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationId, variationType)
            ?.let { return it }

        val fallbackServerResult = if (canUseFallbackServer) {
            runCatching { cloudGateway.fetchFallbackVariationById(placementId, locale, variationId, variationType) }
                .onSuccess { variation ->
                    drawer.sendVariationAssignedEvent(variation, variationType)
                    return variation
                }
                .also { result -> result.exceptionOrNull()?.takeIf { it.isProfileWasChanged }?.let { throw it } }
        } else null

        localReader.getFallbackFileVariations(placementId)
            ?.firstOrNull { variation -> variation.variationId == variationId }
            ?.let { variation ->
                drawer.sendVariationAssignedEvent(variation, variationType)
                return variation
            }

        throw fallbackServerResult?.exceptionOrNull() ?: mainError
    }

    private fun upgradeLocalFallbackFromServer(candidate: Variation, placementId: String, locale: String, variationType: VariationType): Flow<Variation> =
        flow { emit(getVariationFromFallbackServer(placementId, locale, variationType)) }
            .recoverUnlessProfileChanged { candidate }

    private fun getVariationFromBackend(
        placementId: String,
        locale: String,
        placementSource: PlacementSource.Regular,
        variationType: VariationType,
    ): Variation {
        val crossPlacementVariationId =
            cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
        if (crossPlacementVariationId != null) {
            drawer.recordAssignedVariation(placementSource.placementRequestId, crossPlacementVariationId)
            return cloudGateway.fetchVariationById(placementId, locale, crossPlacementVariationId, variationType)
                .also { variation ->
                    drawer.sendVariationAssignedEvent(variation, variationType)
                }
        }

        val (response, profile) = cloudGateway.fetchVariations(placementId, locale, variationType)

        val (variations, request) = response

        if (request.currentDataWhenSent?.profileId != cacheRepository.getProfileId())
            throw AdaptyError(
                message = "Profile was changed!",
                adaptyErrorCode = AdaptyErrorCode.PROFILE_WAS_CHANGED
            )

        val cachedSnapshotAt = localReader.getCachedVariationsSnapshotAt(placementId, locale, variationType)
        if (cachedSnapshotAt != null && variations.snapshotAt < cachedSnapshotAt) {
            localReader.readVariationFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType)?.let { cached -> return cached }
        }

        response.rawBody?.let { rawBody ->
            cacheRepository.saveRawVariations(placementId, variationType, rawBody, locale, profile.segmentId, variations.snapshotAt)
        }
        return drawer.extractSingleVariation(
            variations.data,
            profile.profileId,
            placementId,
            locale,
            placementSource,
            variationType,
        )
    }

    private fun getVariationFromFallbackServer(
        placementId: String,
        locale: String,
        variationType: VariationType,
    ): Variation {
        val variations = cloudGateway.fetchFallbackVariations(placementId, locale, variationType)
        val profileId = cacheRepository.getProfileId()
        return drawer.extractSingleVariation(variations.data, profileId, placementId, locale, PlacementSource.Fallback.Remote, variationType)
    }

    private fun Flow<Variation>.recoverFromLocalSources(id: String, locale: String, placementSource: PlacementSource, variationType: VariationType) =
        recoverUnlessProfileChanged { error ->
            localReader.selectFromCacheOrFallbackFile(id, locale, variationType, placementSource)
                ?: throw error
        }

    private fun resolveVariationUntargeted(id: String, locale: String, variationType: VariationType): Flow<Variation> =
        lifecycleManager
            .onActivateAllowed()
            .mapLatest {
                val response = cloudRepository.getVariationsUntargeted(id, locale, variationType)
                val variations = response.data
                val cachedSnapshotAt = localReader.getCachedVariationsSnapshotAt(id, locale, variationType)
                if (cachedSnapshotAt != null && variations.snapshotAt < cachedSnapshotAt) {
                    localReader.readVariationFromCache(id, locale, variationType)?.let { cached -> return@mapLatest cached }
                }
                response.rawBody?.let { rawBody ->
                    cacheRepository.saveRawVariations(id, variationType, rawBody, locale, null, variations.snapshotAt)
                }
                val profileId = cacheRepository.getProfileId()
                drawer.extractSingleVariation(variations.data, profileId, id, locale, PlacementSource.Untargeted, variationType)
            }
            .recoverFromLocalSources(id, locale, PlacementSource.Untargeted, variationType)

    private fun getPinnedVariationFromCache(placementId: String, locale: String, variationType: VariationType) =
        flow {
            if (cacheRepository.getProfile()?.isTestUser == true) {
                emit(null)
                return@flow
            }
            val pinnedVariationId = cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
            emit(pinnedVariationId?.let { localReader.readVariationByIdFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), it, variationType) })
        }

    private fun getVariationFromCache(placementId: String, locale: String, variationType: VariationType, maxAgeMillis: Long?) =
        flow {
            if (cacheRepository.getProfile()?.isTestUser == true) {
                emit(null)
                return@flow
            }
            val cachedVariation = localReader.readVariationFromCache(placementId, setOf(locale, DEFAULT_PLACEMENT_LOCALE), variationType, maxAgeMillis)
            emit(cachedVariation)
        }

    private suspend fun syncPurchasesIfNeeded() =
        purchasesInteractor
            .syncPurchasesIfNeeded()
            .map { true }
            .catch { emit(false) }

}

