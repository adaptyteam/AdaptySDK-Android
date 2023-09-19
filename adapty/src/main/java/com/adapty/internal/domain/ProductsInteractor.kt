package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.errors.AdaptyErrorCode.NO_PRODUCT_IDS_FOUND
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.*
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductsInteractor(
    private val authInteractor: AuthInteractor,
    private val purchasesInteractor: PurchasesInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
    private val paywallMapper: PaywallMapper,
    private val viewConfigurationMapper: ViewConfigurationMapper,
    private val productMapper: ProductMapper,
    private val paywallPicker: PaywallPicker,
    private val productPicker: ProductPicker,
) {

    @JvmSynthetic
    fun getPaywall(id: String, locale: String?) =
        authInteractor.runWhenAuthDataSynced(
            call = {
                syncPurchasesIfNeeded()
                    .map { synced ->
                        val paywall = cloudRepository.getPaywall(id, locale)
                        val products = productMapper.map(paywall.products)
                        paywall to products
                    }
            },
            switchIfProfileCreationFailed = {
                cacheRepository.getFallbackPaywalls()?.paywalls
                    ?.firstOrNull { it.developerId == id }?.let { fallbackPaywall ->
                        val products =
                            productMapper.map(fallbackPaywall.products)
                        flowOf(fallbackPaywall to products)
                    }
            }
        )
            .flattenConcat()
            .map { (paywall, products) -> paywallMapper.map(paywall, products) }
            .catch { error ->
                if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                    val cachedPaywall = cacheRepository.getPaywall(id)
                    val fallbackPaywall = cacheRepository.getFallbackPaywalls()?.paywalls
                            ?.firstOrNull { it.developerId == id }
                    val chosenPaywall =
                        paywallPicker.pick(cachedPaywall, fallbackPaywall, locale) ?: throw error

                    val productsFromCachedPaywall =
                        productMapper.map(cachedPaywall?.products.orEmpty())
                    val productsFromFallbackPaywall =
                        productMapper.map(fallbackPaywall?.products.orEmpty())

                    val chosenProducts = productPicker.pick(
                        productsFromCachedPaywall,
                        productsFromFallbackPaywall,
                        chosenPaywall.products.mapNotNullTo(mutableSetOf()) { it.vendorProductId },
                    )
                    emit(paywallMapper.map(chosenPaywall, chosenProducts))
                } else {
                    throw error
                }
            }
            .flowOnIO()

    @JvmSynthetic
    fun getViewConfiguration(paywall: AdaptyPaywall, locale: String) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.getViewConfiguration(paywall.variationId, locale)
        }
            .map { viewConfig -> viewConfigurationMapper.map(viewConfig) }
            .flowOnIO()

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
        cloudRepository.onActivateAllowed()
            .mapLatest { cloudRepository.getProductIds() }
            .retryIfNecessary(INFINITE_RETRY)
            .flatMapConcat { productIds -> storeManager.queryProductDetails(productIds, INFINITE_RETRY) }
            .flowOnIO()

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String) =
        cacheRepository.saveFallbackPaywalls(paywalls)

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