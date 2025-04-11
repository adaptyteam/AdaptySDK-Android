@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.errors.AdaptyErrorCode.NO_PRODUCT_IDS_FOUND
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.Request
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.BackendError.Companion.INCORRECT_SEGMENT_HASH_ERROR
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.Variations
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.DEFAULT_PAYWALL_LOCALE
import com.adapty.internal.utils.DEFAULT_RETRY_COUNT
import com.adapty.internal.utils.INFINITE_RETRY
import com.adapty.internal.utils.INF_PAYWALL_TIMEOUT_MILLIS
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.LifecycleManager
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.PAYWALL_TIMEOUT_MILLIS_SHIFT
import com.adapty.internal.utils.PaywallMapper
import com.adapty.internal.utils.ProductMapper
import com.adapty.internal.utils.VariationPicker
import com.adapty.internal.utils.generateUuid
import com.adapty.internal.utils.getLocale
import com.adapty.internal.utils.orDefault
import com.adapty.internal.utils.retryIfNecessary
import com.adapty.internal.utils.timeout
import com.adapty.internal.utils.unlockQuietly
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.FileLocation
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductsInteractor(
    private val authInteractor: AuthInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val lifecycleManager: LifecycleManager,
    private val storeManager: StoreManager,
    private val paywallMapper: PaywallMapper,
    private val productMapper: ProductMapper,
    private val variationPicker: VariationPicker,
    private val analyticsTracker: AnalyticsTracker,
    private val crossPlacementInfoLock: ReentrantReadWriteLock,
) {

    @JvmSynthetic
    fun getPaywall(id: String, locale: String, fetchPolicy: AdaptyPaywall.FetchPolicy, loadTimeout: Int): Flow<AdaptyPaywall> {
        return getPaywallInternal(
            fetchPolicy = fetchPolicy,
            fetchFromCloud = { getPaywallFromCloud(id, locale, loadTimeout, generateUuid()) },
            fetchFromCache = {
                val maxAgeMillis = (fetchPolicy as? AdaptyPaywall.FetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, maxAgeMillis)
            }
        )
    }

    @JvmSynthetic
    fun getPaywallUntargeted(id: String, locale: String, fetchPolicy: AdaptyPaywall.FetchPolicy): Flow<AdaptyPaywall> {
        return getPaywallInternal(
            fetchPolicy = fetchPolicy,
            fetchFromCloud = { getPaywallUntargetedFromCloud(id, locale) },
            fetchFromCache = {
                val maxAgeMillis = (fetchPolicy as? AdaptyPaywall.FetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, maxAgeMillis)
            }
        )
    }

    private fun getPaywallInternal(
        fetchPolicy: AdaptyPaywall.FetchPolicy,
        fetchFromCloud: () -> Flow<AdaptyPaywall>,
        fetchFromCache: () -> Flow<AdaptyPaywall?>,
    ): Flow<AdaptyPaywall> {
        return when (fetchPolicy) {
            is AdaptyPaywall.FetchPolicy.ReloadRevalidatingCacheData -> fetchFromCloud()
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

    private fun getPaywallFromCloud(placementId: String, locale: String, loadTimeout: Int, placementRequestId: String): Flow<AdaptyPaywall> {
        val placementCloudSource = PlacementCloudSource.Regular(placementRequestId)

        val baseFlow = authInteractor.runWhenAuthDataSynced(
            call = {
                syncPurchasesIfNeeded()
                    .map {
                        getPaywallOrVariationsFromCloud(placementId, locale, placementCloudSource)
                    }
            },
            switchIfProfileCreationFailed = {
                cacheRepository.getPaywallVariationsFallback(placementId)?.let { variations ->
                    val profileId = cacheRepository.getProfileId()
                    runCatching { extractSingleVariation(variations.data, profileId, placementId, locale, PlacementCloudSource.Fallback) }.getOrNull()?.let { fallbackPaywall ->
                        flowOf(fallbackPaywall)
                    }
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
                            cloudRepository.getPaywallByVariationIdFallback(placementId, locale, prevCheckpoint.variationId)
                                .also { paywall -> cacheRepository.savePaywall(placementId, paywall) }
                        else
                            getPaywallOrVariationsFallbackFromCloud(placementId, locale)
                    }
                }
        }
            .map { paywall -> paywallMapper.map(paywall, productMapper.map(paywall.products)) }
            .handleFetchPaywallError(placementId, locale, placementCloudSource)
    }

    private fun getPaywallOrVariationsFromCloud(
        placementId: String,
        locale: String,
        placementCloudSource: PlacementCloudSource.Regular,
    ): PaywallDto {
        val crossPlacementVariationId =
            cacheRepository.getCrossPlacementInfo()?.placementWithVariationMap?.get(placementId)
        if (crossPlacementVariationId != null) {
            checkpointHolder.getAndUpdate(placementCloudSource.placementRequestId, CheckPoint.VariationAssigned(crossPlacementVariationId))
            return getPaywallByVariationId(placementId, locale, crossPlacementVariationId)
                .also { paywall ->
                    cacheRepository.savePaywall(placementId, paywall)
                    sendVariationAssignedEvent(paywall)
                }
        }

        var profile = cacheRepository.getProfile() ?: cloudRepository.getProfile().first
        val responseData: Pair<Variations, Request.CurrentDataWhenSent?>
        val segmentId = profile.segmentId
        try {
            responseData = cloudRepository.getPaywallVariations(placementId, locale, segmentId)
        } catch (error: Throwable) {
            val isIncorrectSegmentHash = error is AdaptyError && error.backendError != null
                    && error.backendError.containsErrorCode(INCORRECT_SEGMENT_HASH_ERROR)
            if (!isIncorrectSegmentHash)
                throw error
            val cachedProfile = cacheRepository.getProfile()
            if (cachedProfile != null && segmentId != cachedProfile.segmentId)
                return getPaywallOrVariationsFromCloud(placementId, locale, placementCloudSource)
            profile = cloudRepository.getProfile().first
            if (segmentId == profile.segmentId)
                throw error
            return getPaywallOrVariationsFromCloud(placementId, locale, placementCloudSource)
        }

        val (variations, requestDataWhenSent) = responseData

        if (requestDataWhenSent?.profileId != cacheRepository.getProfileId())
            throw AdaptyError(
                message = "Profile was changed!",
                adaptyErrorCode = AdaptyErrorCode.PROFILE_WAS_CHANGED
            )

        val cachedPaywall = cacheRepository.getPaywall(placementId, locale)
        return if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt) {
            cachedPaywall
        } else {
            val variation = extractSingleVariation(
                variations.data,
                profile.profileId,
                placementId,
                locale,
                placementCloudSource,
            )

            variation
                .also { paywall -> cacheRepository.savePaywall(placementId, paywall) }
        }
    }

    private fun getPaywallOrVariationsFallbackFromCloud(
        placementId: String,
        locale: String,
    ): PaywallDto {
        val variations = cloudRepository.getPaywallVariationsFallback(placementId, locale)
        val cachedPaywall = cacheRepository.getPaywall(placementId, locale)
        return if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt) {
            cachedPaywall
        } else {
            val profileId = cacheRepository.getProfileId()
            val variation = extractSingleVariation(variations.data, profileId, placementId, locale, PlacementCloudSource.Fallback)
            variation
                .also { paywall -> cacheRepository.savePaywall(placementId, paywall) }
        }
    }

    private fun getPaywallByVariationId(
        placementId: String,
        locale: String,
        variationId: String,
    ): PaywallDto {
        var profile = cacheRepository.getProfile() ?: cloudRepository.getProfile().first
        val segmentId = profile.segmentId
        try {
            return cloudRepository.getPaywallByVariationId(placementId, locale, segmentId, variationId)
        } catch (error: Throwable) {
            val isIncorrectSegmentHash = error is AdaptyError && error.backendError != null
                    && error.backendError.containsErrorCode(INCORRECT_SEGMENT_HASH_ERROR)
            if (!isIncorrectSegmentHash)
                throw error
            val cachedProfile = cacheRepository.getProfile()
            if (cachedProfile != null && segmentId != cachedProfile.segmentId)
                return getPaywallByVariationId(placementId, locale, variationId)
            profile = cloudRepository.getProfile().first
            if (segmentId == profile.segmentId)
                throw error
            return getPaywallByVariationId(placementId, locale, variationId)
        }
    }

    private fun extractSingleVariation(
        paywalls: Collection<PaywallDto>,
        profileId: String,
        placementId: String,
        locale: String,
        placementCloudSource: PlacementCloudSource,
    ): PaywallDto {
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
                        sendVariationAssignedEvent(paywall)
                    }
            }

            val paywall = variationPicker.pick(paywalls, profileId)

            if (paywall != null) {
                sendVariationAssignedEvent(paywall)
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
                        is PlacementCloudSource.Regular -> getPaywallByVariationId(placementId, locale, crossPlacementVariationId)
                        else -> cloudRepository.getPaywallByVariationIdFallback(placementId, locale, crossPlacementVariationId)
                    }
                }
            sendVariationAssignedEvent(paywall)
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
                            is PlacementCloudSource.Regular -> getPaywallByVariationId(placementId, locale, crossPlacementVariationId)
                            else -> cloudRepository.getPaywallByVariationIdFallback(placementId, locale, crossPlacementVariationId)
                        }
                    }
                sendVariationAssignedEvent(paywall)
                return paywall
            } else {
                val paywall = if (paywalls.size == 1) paywalls.first() else variationPicker.pick(paywalls, profileId)
                if (paywall != null) {
                    paywall.crossPlacementInfo?.placementWithVariationMap?.takeIf { it.isNotEmpty() }?.let { paywallCrossPlacementInfo ->
                        if (cachedCrossPlacementInfo != null && cachedCrossPlacementInfo.placementWithVariationMap.isEmpty())
                            cacheRepository.saveCrossPlacementInfoFromPaywall(paywall.crossPlacementInfo.copy(version = cachedCrossPlacementInfo.version))
                    }
                    val placementRequestId = (placementCloudSource as? PlacementCloudSource.Regular)?.placementRequestId
                    if (placementRequestId != null)
                        checkpointHolder.getAndUpdate(placementRequestId, CheckPoint.VariationAssigned(paywall.variationId))
                    crossPlacementInfoLock.writeLock().unlockQuietly()
                    sendVariationAssignedEvent(paywall)
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

    private fun sendVariationAssignedEvent(paywall: PaywallDto) {
        analyticsTracker.trackEvent(
            "paywall_variation_assigned",
            mutableMapOf<String, Any>(
                "placement_audience_version_id" to paywall.placementAudienceVersionId,
                "variation_id" to paywall.variationId,
            ),
        )
    }

    private fun Flow<AdaptyPaywall>.handleFetchPaywallError(id: String, locale: String, placementCloudSource: PlacementCloudSource) =
        catch { error ->
            if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                val cachedPaywall = cacheRepository.getPaywall(id, setOf(locale, DEFAULT_PAYWALL_LOCALE))
                val chosenPaywall =
                    if (cachedPaywall == null) {
                        val fallbackVariations = cacheRepository.getPaywallVariationsFallback(id) ?: throw error
                        val profileId = cacheRepository.getProfileId()
                        runCatching { extractSingleVariation(fallbackVariations.data, profileId, id, locale, placementCloudSource) }.getOrNull()
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
                                val fallbackVariations = cacheRepository.getPaywallVariationsFallback(id)

                                val desiredFallbackVariation = fallbackVariations?.data?.firstOrNull { it.variationId == desiredVariationIdIfExists }

                                if (desiredFallbackVariation != null) {
                                    desiredFallbackVariation
                                } else {
                                    val fallbackPaywall = fallbackVariations
                                        ?.let { variations ->
                                            val profileId = cacheRepository.getProfileId()
                                            runCatching { extractSingleVariation(variations.data, profileId, id, locale, placementCloudSource) }.getOrNull()
                                        }

                                    when {
                                        fallbackPaywall != null && fallbackPaywall.variationId == desiredVariationIdIfExists -> fallbackPaywall
                                        else -> cachedPaywall
                                    }
                                }
                            } else {
                                val fallbackPaywall = cacheRepository.getPaywallVariationsFallback(id)
                                    ?.let { variations ->
                                        val profileId = cacheRepository.getProfileId()
                                        runCatching { extractSingleVariation(variations.data, profileId, id, locale, placementCloudSource) }.getOrNull()
                                    }

                                fallbackPaywall ?: cachedPaywall
                            }
                        }
                    } ?: throw error
                emit(paywallMapper.map(chosenPaywall, productMapper.map(chosenPaywall.products)))
            } else {
                throw error
            }
        }

    private fun getPaywallUntargetedFromCloud(id: String, locale: String): Flow<AdaptyPaywall> =
        lifecycleManager
            .onActivateAllowed()
            .mapLatest {
                val variations = cloudRepository.getPaywallVariationsUntargeted(id, locale)
                val cachedPaywall = cacheRepository.getPaywall(id, locale)
                val paywall = if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt) {
                    cachedPaywall
                } else {
                    val profileId = cacheRepository.getProfileId()
                    val variation = extractSingleVariation(variations.data, profileId, id, locale, PlacementCloudSource.Untargeted)
                    variation
                        .also { paywall -> cacheRepository.savePaywall(id, paywall) }
                }
                paywallMapper.map(paywall, productMapper.map(paywall.products))
            }
            .retryIfNecessary(DEFAULT_RETRY_COUNT)
            .handleFetchPaywallError(id, locale, PlacementCloudSource.Untargeted)

    private fun getPaywallFromCache(id: String, locale: String, maxAgeMillis: Long?) =
        flow {
            val cachedPaywall = cacheRepository.getPaywall(id, setOf(locale, DEFAULT_PAYWALL_LOCALE), maxAgeMillis)
            emit(
                cachedPaywall?.let { paywall ->
                    val products = productMapper.map(paywall.products)
                    paywallMapper.map(paywall, products)
                }
            )
        }

    @JvmSynthetic
    fun getViewConfiguration(paywall: AdaptyPaywall, loadTimeout: Int) : Flow<Map<String, Any>> {
        val locale = paywall.getLocale()
        val localViewConfig = (paywall.viewConfig?.takeIf { config -> config["paywall_builder_config"] != null })
            ?: (cacheRepository.getPaywall(paywall.placementId, locale)
                ?.takeIf { cachedPaywall ->
                    cachedPaywall.variationId == paywall.variationId
                            && cachedPaywall.paywallId == paywall.paywallId
                            && cachedPaywall.revision == paywall.revision
                            && cachedPaywall.snapshotAt == paywall.snapshotAt
                }?.paywallBuilder)
        if (localViewConfig != null)
            return flowOf(localViewConfig)

        val baseFlow = authInteractor.runWhenAuthDataSynced {
            cloudRepository.getViewConfiguration(paywall.variationId, locale)
        }

        return if (loadTimeout == INF_PAYWALL_TIMEOUT_MILLIS) {
            baseFlow
        } else {
            timeout(baseFlow, loadTimeout - PAYWALL_TIMEOUT_MILLIS_SHIFT)
        }
            .map { viewConfig ->
                viewConfig ?: cloudRepository.getViewConfigurationFallback(paywall.paywallId, locale)
            }
    }

    @JvmSynthetic
    fun getPaywallProducts(paywall: AdaptyPaywall) : Flow<List<AdaptyPaywallProduct>> =
        flow {
            emit(paywall.products)
        }.flatMapConcat { products ->
            getBillingInfo(products, maxAttemptCount = DEFAULT_RETRY_COUNT)
                .map { billingInfo ->
                    productMapper.map(products, billingInfo, paywall)
                }
        }

    @JvmSynthetic
    fun getProductsOnStart() =
        lifecycleManager.onActivateAllowed()
            .mapLatest { cloudRepository.getProductIds() }
            .retryIfNecessary(INFINITE_RETRY)
            .flatMapConcat { productIds -> storeManager.queryProductDetails(productIds, INFINITE_RETRY) }

    @JvmSynthetic
    fun setFallbackPaywalls(source: FileLocation) =
        flow {
            emit(cacheRepository.saveFallbackPaywalls(source))
        }

    private fun getBillingInfo(
        products: List<BackendProduct>,
        maxAttemptCount: Long,
    ) : Flow<Map<String, ProductDetails>> {
        if (products.isEmpty())
            throwNoProductIdsFoundError()
        val productIds = products.map { it.vendorProductId }.distinct()
        return storeManager.queryProductDetails(productIds, maxAttemptCount)
            .map { productDetailsList ->
                if (productDetailsList.isEmpty())
                    throwNoProductIdsFoundError()
                productDetailsList.associateBy { productDetails -> productDetails.productId }
            }
    }

    private suspend fun syncPurchasesIfNeeded() =
        purchasesInteractor
            .syncPurchasesIfNeeded()
            .map { true }
            .catch { emit(false) }

    private fun throwNoProductIdsFoundError(): Nothing {
        val message = "No In-App Purchase product identifiers were found."
        Logger.log(ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = NO_PRODUCT_IDS_FOUND
        )
    }

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