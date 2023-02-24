package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
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
    fun getPaywall(id: String, locale: String?) =
        authInteractor.runWhenAuthDataSynced {
            syncPurchasesIfNeeded()
                .map { synced -> cloudRepository.getPaywall(id, locale) to synced }
        }
            .flattenConcat()
            .map { (paywall, synced) ->
                val products = productMapper.map(paywall.products, Source.CLOUD, synced)
                paywallMapper.map(paywall, products)
            }
            .catch { error ->
                if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                    val purchasesHaveBeenSynced = cacheRepository.getPurchasesHaveBeenSynced()
                    val cachedPaywall = cacheRepository.getPaywall(id)
                    val fallbackPaywall = cacheRepository.getFallbackPaywalls()?.paywalls
                            ?.firstOrNull { it.developerId == id }
                    val chosenPaywall =
                        paywallPicker.pick(cachedPaywall, fallbackPaywall, locale) ?: throw error

                    val productsFromCachedPaywall =
                        productMapper.map(
                            cachedPaywall?.products.orEmpty(),
                            Source.CACHE,
                            purchasesHaveBeenSynced,
                        )
                    val productsFromFallbackPaywall =
                        productMapper.map(
                            fallbackPaywall?.products.orEmpty(),
                            Source.FALLBACK,
                            purchasesHaveBeenSynced,
                        )

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
            syncPurchasesIfNeeded()
                .map { synced ->
                    productMapper.map(cloudRepository.getProducts(), Source.CLOUD, synced)
                }
        }.flattenConcat().map { allProducts ->
            findProductsFromPaywallOrdered(paywall, allProducts)
        }.flatMapConcat { products ->
            getProductStoreData(products, maxAttemptCount = DEFAULT_RETRY_COUNT)
                .map { storeData ->
                    productMapper.map(products, storeData, paywall)
                }
        }.catch { error ->
            if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                val purchasesHaveBeenSynced = cacheRepository.getPurchasesHaveBeenSynced()
                val products = productPicker.pick(
                    productMapper.map(
                        cacheRepository.getProducts().orEmpty(),
                        Source.CACHE,
                        purchasesHaveBeenSynced,
                    ),
                    productMapper.map(
                        cacheRepository.getFallbackPaywalls()?.products.orEmpty(),
                        Source.FALLBACK,
                        purchasesHaveBeenSynced,
                    ),
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

    private fun findProductsFromPaywallOrdered(
        paywall: AdaptyPaywall,
        allProducts: Collection<Product>,
    ): List<Product> {
        val productIdIndices =
            paywall.vendorProductIds.mapIndexed { i: Int, id: String -> id to i }.toMap()

        val foundProducts = arrayOfNulls<Product?>(productIdIndices.size)
        var productsFound = 0
        for (product in allProducts) {
            productIdIndices[product.vendorProductId]?.let { index ->
                foundProducts[index] = product
                productsFound++
            }
            if (productsFound == productIdIndices.size)
                break
        }

        return foundProducts.filterNotNull()
    }

    private suspend fun syncPurchasesIfNeeded() =
        purchasesInteractor
            .syncPurchasesIfNeeded()
            .map { true }
            .catch { emit(false) }
}