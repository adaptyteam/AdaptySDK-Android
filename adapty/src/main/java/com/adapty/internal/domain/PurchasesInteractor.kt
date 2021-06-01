package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.ProductDto
import com.adapty.internal.data.models.RestoreProductInfo
import com.adapty.internal.data.models.responses.PaywallsResponse
import com.adapty.internal.data.models.responses.RestoreReceiptResponse
import com.adapty.internal.utils.*
import com.adapty.models.ProductModel
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchasesInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
) {

    @JvmSynthetic
    fun validatePurchase(purchaseType: String, purchase: Purchase, product: ProductModel?) =
        cloudRepository.validatePurchase(
            purchaseType,
            purchase,
            product?.let(ProductMapper::mapToValidate)
        ).map { validationResponse ->
            validationResponse.data?.attributes
                ?.let(PurchaserInfoMapper::map)
                ?.let { purchaserInfo ->
                    cacheRepository.savePurchaserInfo(purchaserInfo)
                    Pair(
                        purchaserInfo,
                        validationResponse.data.attributes.googleValidationResult
                    )
                } ?: Pair(null, validationResponse.data?.attributes?.googleValidationResult)
        }

    @JvmSynthetic
    fun restorePurchases() =
        syncPurchasesInternal(maxAttemptCount = 3) { notSynced ->
            cloudRepository.restorePurchases(notSynced)
        }
            .map { response ->
                response?.data?.attributes
                    ?.let(PurchaserInfoMapper::map)
                    ?.let { purchaserInfo ->
                        cacheRepository.savePurchaserInfo(purchaserInfo)
                        Pair(
                            purchaserInfo,
                            response.data.attributes.googleValidationResult
                        )
                    } ?: Pair(null, response?.data?.attributes?.googleValidationResult)
            }
            .flowOnIO()

    @JvmSynthetic
    fun syncPurchasesOnStart(): Flow<RestoreReceiptResponse?> {
        return syncPurchasesInternal { notSynced ->
            flow {
                emit(cloudRepository.restorePurchasesForced(notSynced))
            }.retryIfNecessary()
        }
    }

    private fun syncPurchasesInternal(
        maxAttemptCount: Long = -1,
        sendToBackend: (List<RestoreProductInfo>) -> Flow<RestoreReceiptResponse>
    ): Flow<RestoreReceiptResponse?> {
        return storeManager.queryPurchaseHistory(maxAttemptCount)
            .zip(flowOf(cacheRepository.getSyncedPurchases())) { historyPurchases, savedPurchases ->
                Pair(historyPurchases, savedPurchases)
            }
            .flatMapConcat { (historyPurchases, savedPurchases) ->
                fillProductInfoFromCache(historyPurchases).filterNot(savedPurchases::contains)
                    .takeIf { it.isNotEmpty() }?.let { notSynced ->
                        sendToBackend(notSynced)
                            .map { response ->
                                cacheRepository.saveSyncedPurchases(historyPurchases)
                                response
                            }
                    } ?: flowOf(null)
            }
            .flowOnIO()
    }

    private fun fillProductInfoFromCache(historyPurchases: ArrayList<RestoreProductInfo>): ArrayList<RestoreProductInfo> {

        val containers = cacheRepository.getContainers()
        val products = cacheRepository.getProducts()
        return historyPurchases.onEach { purchase ->
            purchase.productId?.let { productId ->
                val product = getElementFromContainers(containers, products, productId)

                product?.let { product ->
                    purchase.setDetails(product.skuDetails)
                    purchase.localizedTitle = product.localizedTitle
                }
            }
        }
    }

    private fun getElementFromContainers(
        containers: ArrayList<PaywallsResponse.Data>?,
        prods: ArrayList<ProductDto>?,
        id: String
    ): ProductDto? {
        containers?.forEach { container ->
            container.attributes?.products?.forEach { product ->
                if (product.vendorProductId == id) {
                    return product
                }
            }
        }
        return prods?.firstOrNull { it.vendorProductId == id }
    }

    @JvmSynthetic
    fun setTransactionVariationId(transactionId: String, variationId: String) =
        cloudRepository.setTransactionVariationId(transactionId, variationId)

    @JvmSynthetic
    fun consumeAndAcknowledgeTheUnprocessed() {
        val inapps = storeManager.queryInapps()?.takeIf { it.isNotEmpty() }
        val subs = storeManager.queryUnacknowledgedSubs()?.takeIf { it.isNotEmpty() }

        if (inapps != null || subs != null) {
            val products = cacheRepository.getProducts()

            inapps?.forEach { purchase ->
                products?.firstOrNull { it.vendorProductId == purchase.skus.firstOrNull() }?.let { product ->
                    execute {
                        storeManager.consumePurchase(purchase)
                            .flatMapConcat {
                                cloudRepository.validatePurchase(
                                    BillingClient.SkuType.INAPP,
                                    purchase,
                                    ProductMapper.mapToValidate(product)
                                )
                            }
                            .catch { }
                            .collect()
                    }
                }
            }

            subs?.forEach { purchase ->
                products?.firstOrNull { it.vendorProductId == purchase.skus.firstOrNull() }?.let { product ->
                    execute {
                        storeManager.acknowledgePurchase(purchase)
                            .flatMapConcat {
                                cloudRepository.validatePurchase(
                                    BillingClient.SkuType.SUBS,
                                    purchase,
                                    ProductMapper.mapToValidate(product)
                                )
                            }
                            .catch { }
                            .collect()
                    }
                }
            }
        }
    }
}