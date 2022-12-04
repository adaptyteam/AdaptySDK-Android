package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.ProductStoreData
import com.adapty.internal.domain.models.Source
import com.adapty.internal.domain.models.Product
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct
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
    private val productMapper: ProductMapper,
    private val paywallPicker: PaywallPicker,
    private val productPicker: ProductPicker,
) {

    @JvmSynthetic
    fun getPaywall(id: String) =
        authInteractor.runWhenAuthDataSynced {
            purchasesInteractor
                .syncPurchasesIfNeeded()
                .map { cloudRepository.getPaywall(id) }
        }
            .flattenConcat()
            .map { paywall ->
                val products = productMapper.map(paywall.products, Source.CLOUD)
                paywallMapper.map(paywall, products)
            }
            .catch { error ->
                if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                    val cachedPaywall = cacheRepository.getPaywall(id)
                    val fallbackPaywall = cacheRepository.getFallbackPaywalls()?.first
                            ?.firstOrNull { it.attributes?.developerId == id }?.attributes
                    val chosenPaywall =
                        paywallPicker.pick(cachedPaywall, fallbackPaywall) ?: throw error

                    val productsFromCachedPaywall =
                        productMapper.map(cachedPaywall?.products.orEmpty(), Source.CACHE)
                    val productsFromFallbackPaywall =
                        productMapper.map(fallbackPaywall?.products.orEmpty(), Source.FALLBACK)

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
    fun getPaywallProducts(paywall: AdaptyPaywall) : Flow<List<AdaptyPaywallProduct>> =
        authInteractor.runWhenAuthDataSynced {
            purchasesInteractor
                .syncPurchasesIfNeeded()
                .map {
                    productMapper.map(cloudRepository.getProducts(), Source.CLOUD)
                }
        }.flattenConcat().map { allProducts ->
            allProducts.filter { product -> product.vendorProductId in paywall.vendorProductIds }
        }.flatMapConcat { products ->
            getProductStoreData(products, maxAttemptCount = DEFAULT_RETRY_COUNT)
                .map { storeData ->
                    productMapper.map(products, storeData, paywall)
                }
        }.catch { error ->
            if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                val products = productPicker.pick(
                    productMapper.map(cacheRepository.getProducts().orEmpty(), Source.CACHE),
                    productMapper.map(cacheRepository.getFallbackPaywalls()?.second.orEmpty(), Source.FALLBACK),
                    paywall.vendorProductIds.toSet(),
                )

                if (products.size != paywall.vendorProductIds.size) {
                    throw error
                }

                emitAll(
                    getProductStoreData(products, DEFAULT_RETRY_COUNT)
                        .map { storeData ->
                            productMapper.map(products, storeData, paywall)
                        }
                )
            } else {
                throw error
            }
        }.flowOnIO()

    @JvmSynthetic
    fun getProductsOnStart() =
        cloudRepository.onActivateAllowed()
            .mapLatest { cloudRepository.getProductIds() }
            .retryIfNecessary(INFINITE_RETRY)
            .flatMapConcat { productIds -> storeManager.querySkuDetails(productIds, INFINITE_RETRY) }
            .flowOnIO()

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String) =
        cacheRepository.saveFallbackPaywalls(paywalls)

    private fun getProductStoreData(
        products: List<Product>,
        maxAttemptCount: Long,
    ) : Flow<Map<String, ProductStoreData>> {
        val productIds = products.map { it.vendorProductId }.distinct()
        return storeManager.querySkuDetails(productIds, maxAttemptCount)
            .map { skuDetailsList ->
                skuDetailsList.associate { skuDetails ->
                    skuDetails.sku to productMapper.mapBillingInfoToProductStoreData(skuDetails)
                }
            }
    }
}