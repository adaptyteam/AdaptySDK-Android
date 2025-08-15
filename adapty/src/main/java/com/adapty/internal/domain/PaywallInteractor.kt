@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PRODUCT_IDS_FOUND
import com.adapty.errors.AdaptyErrorCode.WRONG_PARAMETER
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.DEFAULT_RETRY_COUNT
import com.adapty.internal.utils.INFINITE_RETRY
import com.adapty.internal.utils.INF_PAYWALL_TIMEOUT_MILLIS
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.LifecycleManager
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.PAYWALL_TIMEOUT_MILLIS_SHIFT
import com.adapty.internal.utils.PaywallMapper
import com.adapty.internal.utils.ProductMapper
import com.adapty.internal.utils.retryIfNecessary
import com.adapty.internal.utils.timeout
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPlacementFetchPolicy
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.FileLocation
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallInteractor(
    private val paywallFetcher: BasePlacementFetcher,
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val lifecycleManager: LifecycleManager,
    private val storeManager: StoreManager,
    private val paywallMapper: PaywallMapper,
    private val productMapper: ProductMapper,
) {

    @JvmSynthetic
    fun getPaywall(placementId: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<AdaptyPaywall> {
        return paywallFetcher.fetchPaywall(placementId, locale, fetchPolicy, loadTimeout)
            .map { paywall -> paywallMapper.map(paywall, productMapper.map(paywall.products)) }
    }

    @JvmSynthetic
    fun getPaywallUntargeted(placementId: String, locale: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<AdaptyPaywall> {
        return paywallFetcher.fetchPaywallUntargeted(placementId, locale, fetchPolicy)
            .map { paywall -> paywallMapper.map(paywall, productMapper.map(paywall.products)) }
    }

    @JvmSynthetic
    fun getViewConfiguration(paywall: AdaptyPaywall, loadTimeout: Int) : Flow<Map<String, Any>> {
        val locale = paywall.viewConfig?.get("lang") as? String ?: kotlin.run {
            val errorMessage = "lang in paywall builder should not be null"
            Logger.log(ERROR) { errorMessage }
            return flow {
                throw AdaptyError(message = errorMessage, adaptyErrorCode = WRONG_PARAMETER)
            }
        }
        val localViewConfig = (paywall.viewConfig.takeIf { config -> config["paywall_builder_config"] != null })
            ?: (cacheRepository.getPaywall(paywall.placement.id, locale)
                ?.takeIf { cachedPaywall ->
                    cachedPaywall.variationId == paywall.variationId
                            && cachedPaywall.id == paywall.id
                            && cachedPaywall.placement.revision == paywall.placement.revision
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
                viewConfig ?: cloudRepository.getViewConfigurationFallback(paywall.id, locale)
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
    fun setFallback(source: FileLocation) =
        flow {
            emit(cacheRepository.saveFallback(source))
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

    private fun throwNoProductIdsFoundError(): Nothing {
        val message = "No In-App Purchase product identifiers were found."
        Logger.log(ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = NO_PRODUCT_IDS_FOUND
        )
    }
}
