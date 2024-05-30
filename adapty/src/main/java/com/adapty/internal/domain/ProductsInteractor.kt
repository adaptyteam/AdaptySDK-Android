package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.errors.AdaptyErrorCode.NO_PRODUCT_IDS_FOUND
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.BackendError.Companion.INCORRECT_SEGMENT_HASH_ERROR
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.ProfileDto
import com.adapty.internal.data.models.Variations
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.FileLocation
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.*
import java.io.IOException

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
) {

    @JvmSynthetic
    fun getPaywall(id: String, locale: String, fetchPolicy: AdaptyPaywall.FetchPolicy, loadTimeout: Int): Flow<AdaptyPaywall> {
        return when (fetchPolicy) {
            is AdaptyPaywall.FetchPolicy.ReloadRevalidatingCacheData -> getPaywallFromCloud(id, locale, loadTimeout)
            else -> {
                val maxAgeMillis = (fetchPolicy as? AdaptyPaywall.FetchPolicy.ReturnCacheDataIfNotExpiredElseLoad)?.maxAgeMillis
                getPaywallFromCache(id, locale, maxAgeMillis)
                    .flatMapConcat { paywall ->
                        if (paywall != null) {
                            flowOf(paywall)
                        } else {
                            getPaywallFromCloud(id, locale, loadTimeout)
                        }
                    }
            }
        }
    }

    private fun getPaywallFromCloud(id: String, locale: String, loadTimeout: Int): Flow<AdaptyPaywall> {
        val baseFlow = authInteractor.runWhenAuthDataSynced(
            call = {
                syncPurchasesIfNeeded()
                    .map {
                        val (variations, profile) =
                            getPaywallVariationsForProfile(id, locale, cacheRepository.getProfile() ?: cloudRepository.getProfile().first)
                        val cachedPaywall = cacheRepository.getPaywall(id, locale)
                        if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt.orDefault()) {
                            cachedPaywall
                        } else {
                            val profileId = profile.profileId ?: throw AdaptyError(
                                message = "profileId in Profile should not be null",
                                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                            )
                            val variation = extractSingleVariation(variations.data, profileId)
                            paywallMapper.mapToCache(variation, variations.snapshotAt)
                                .also { paywall -> cacheRepository.savePaywall(id, paywall) }
                        }
                    }
            },
            switchIfProfileCreationFailed = {
                cacheRepository.getPaywallVariationsFallback(id)?.let { variations ->
                    val profileId = cacheRepository.getProfileId()
                    runCatching { extractSingleVariation(variations.data, profileId) }.getOrNull()?.let { fallbackPaywall ->
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
                    if (result == null) {
                        val variations = cloudRepository.getPaywallVariationsFallback(id, locale)
                        val cachedPaywall = cacheRepository.getPaywall(id, locale)
                        if (cachedPaywall != null && variations.snapshotAt < cachedPaywall.snapshotAt.orDefault()) {
                            cachedPaywall
                        } else {
                            val profileId = cacheRepository.getProfileId()
                            val variation = extractSingleVariation(variations.data, profileId)
                            paywallMapper.mapToCache(variation, variations.snapshotAt)
                                .also { paywall -> cacheRepository.savePaywall(id, paywall) }
                        }
                    } else {
                        result
                    }
                }
        }
            .map { paywall -> paywallMapper.map(paywall, productMapper.map(paywall.products)) }
            .catch { error ->
                if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                    val cachedPaywall = cacheRepository.getPaywall(id, setOf(locale, DEFAULT_PAYWALL_LOCALE))
                    val chosenPaywall =
                        if (cachedPaywall == null) {
                            val fallbackVariations = cacheRepository.getPaywallVariationsFallback(id) ?: throw error
                            val profileId = cacheRepository.getProfileId()
                            runCatching { extractSingleVariation(fallbackVariations.data, profileId) }.getOrNull()
                        } else {
                            if (cachedPaywall.snapshotAt.orDefault() >= cacheRepository.getFallbackPaywallsSnapshotAt().orDefault())
                                cachedPaywall
                            else {
                                val fallbackPaywall = cacheRepository.getPaywallVariationsFallback(id)
                                    ?.let { variations ->
                                        val profileId = cacheRepository.getProfileId()
                                        runCatching { extractSingleVariation(variations.data, profileId) }.getOrNull()
                                    }

                                fallbackPaywall ?: cachedPaywall
                            }
                        } ?: throw error
                    emit(paywallMapper.map(chosenPaywall, productMapper.map(chosenPaywall.products)))
                } else {
                    throw error
                }
            }
            .flowOnIO()
    }

    private fun getPaywallVariationsForProfile(id: String, locale: String, profile: ProfileDto): Pair<Variations, ProfileDto> {
        val segmentId = profile.segmentId
            ?: throw AdaptyError(
                message = "segmentId in Profile should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        try {
            return cloudRepository.getPaywallVariations(id, locale, segmentId) to profile
        } catch (error: Throwable) {
            val isIncorrectSegmentHash = error is AdaptyError && error.backendError != null
                    && error.backendError.containsErrorCode(INCORRECT_SEGMENT_HASH_ERROR)
            if (!isIncorrectSegmentHash)
                throw error
            val cachedProfile = cacheRepository.getProfile()
            if (cachedProfile != null && segmentId != cachedProfile.segmentId)
                return getPaywallVariationsForProfile(id, locale, cachedProfile)
            val updatedProfile = cloudRepository.getProfile().first
            if (segmentId == updatedProfile.segmentId)
                throw error
            return getPaywallVariationsForProfile(id, locale, updatedProfile)
        }
    }

    private fun extractSingleVariation(paywalls: Collection<PaywallDto>, profileId: String): PaywallDto {
        if (paywalls.isEmpty()) {
            val message = "Paywall couldn't be found: empty list"
            Logger.log(ERROR) { message }
            throw AdaptyError(
                message = message,
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            )
        }

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

    private fun sendVariationAssignedEvent(paywall: PaywallDto) {
        analyticsTracker.trackEvent(
            "paywall_variation_assigned",
            mutableMapOf<String, Any>(
                "placement_audience_version_id" to paywall.placementAudienceVersionId.orEmpty(),
                "variation_id" to paywall.variationId.orEmpty(),
            ),
        )
    }

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
            .flowOnIO()
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
        }.flowOnIO()

    @JvmSynthetic
    fun getProductsOnStart() =
        lifecycleManager.onActivateAllowed()
            .mapLatest { cloudRepository.getProductIds() }
            .retryIfNecessary(INFINITE_RETRY)
            .flatMapConcat { productIds -> storeManager.queryProductDetails(productIds, INFINITE_RETRY) }
            .flowOnIO()

    @JvmSynthetic
    fun setFallbackPaywalls(source: FileLocation) =
        flow {
            emit(cacheRepository.saveFallbackPaywalls(source))
        }.flowOnIO()

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
}