package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
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
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
) {

    @JvmSynthetic
    fun validatePurchase(purchaseType: String, purchase: Purchase, product: ProductModel?) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(
                purchaseType,
                purchase,
                product?.let(ProductMapper::mapToValidate)
            )
        }
            .map { response ->
                cacheRepository.updateOnPurchaserInfoReceived(response.data?.attributes)
                    .let { purchaserInfo ->
                        purchaserInfo to response.data?.attributes?.googleValidationResult
                    }
            }

    @JvmSynthetic
    fun restorePurchases() =
        syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT) { notSynced ->
            authInteractor.runWhenAuthDataSynced {
                cloudRepository.restorePurchases(notSynced)
            }
        }
            .map { response ->
                cacheRepository.updateOnPurchaserInfoReceived(response.data?.attributes)
                    .let { purchaserInfo ->
                        purchaserInfo to response.data?.attributes?.googleValidationResult
                    }
            }
            .flowOnIO()

    @JvmSynthetic
    fun syncPurchasesOnStart(): Flow<Unit> {
        return syncPurchasesInternal { notSynced ->
            authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                cloudRepository.restorePurchases(notSynced)
            }
        }
            .map { response ->
                cacheRepository.updateOnPurchaserInfoReceived(response.data?.attributes)
                Unit
            }
            .flowOnIO()
    }

    private fun syncPurchasesInternal(
        maxAttemptCount: Long = INFINITE_RETRY,
        sendToBackend: (List<RestoreProductInfo>) -> Flow<RestoreReceiptResponse>
    ): Flow<RestoreReceiptResponse> {
        return storeManager.getPurchaseHistoryDataToRestore(maxAttemptCount)
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
                    }
                    ?: "No purchases to restore".let { errorMessage ->
                        Logger.logError { errorMessage }
                        throw AdaptyError(
                            message = errorMessage,
                            adaptyErrorCode = NO_PURCHASES_TO_RESTORE
                        )
                    }
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
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.setTransactionVariationId(transactionId, variationId)
        }

    @JvmSynthetic
    fun consumeAndAcknowledgeTheUnprocessed() {
        val inapps = storeManager.queryInapps()?.takeIf { it.isNotEmpty() }
        val subs = storeManager.queryUnacknowledgedSubs()?.takeIf { it.isNotEmpty() }

        if (inapps != null || subs != null) {
            val products = cacheRepository.getProducts()

            inapps?.forEach { purchase ->
                products?.firstOrNull { it.vendorProductId == purchase.skus.firstOrNull() }
                    ?.let { product ->
                        execute {
                            storeManager.consumePurchase(purchase)
                                .flatMapConcat {
                                    authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                                        cloudRepository.validatePurchase(
                                            BillingClient.SkuType.INAPP,
                                            purchase,
                                            ProductMapper.mapToValidate(product)
                                        )
                                    }.map { response ->
                                        cacheRepository.updateOnPurchaserInfoReceived(response.data?.attributes)
                                    }
                                }
                                .catch { }
                                .collect()
                        }
                    }
            }

            subs?.forEach { purchase ->
                products?.firstOrNull { it.vendorProductId == purchase.skus.firstOrNull() }
                    ?.let { product ->
                        execute {
                            storeManager.acknowledgePurchase(purchase)
                                .flatMapConcat {
                                    authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                                        cloudRepository.validatePurchase(
                                            BillingClient.SkuType.SUBS,
                                            purchase,
                                            ProductMapper.mapToValidate(product)
                                        )
                                    }.map { response ->
                                        cacheRepository.updateOnPurchaserInfoReceived(response.data?.attributes)
                                    }
                                }
                                .catch { }
                                .collect()
                        }
                    }
            }
        }
    }
}