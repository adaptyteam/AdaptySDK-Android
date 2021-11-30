package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.RestoreProductInfo
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
    private val productMapper: ProductMapper,
) {

    @JvmSynthetic
    fun validatePurchase(purchaseType: String, purchase: Purchase, product: ProductModel?) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(
                purchaseType,
                purchase,
                product?.let(productMapper::mapToValidate)
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
            .zip(flowOf(cacheRepository.getSyncedPurchases())) { historyData, savedPurchases ->
                historyData to savedPurchases
            }
            .flatMapConcat { (historyData, savedPurchases) ->
                historyData.filter { historyRecord ->
                    savedPurchases.firstOrNull { savedPurchase ->
                        savedPurchase.productId == historyRecord.purchase.skus.firstOrNull()
                    } == null
                }.takeIf { it.isNotEmpty() }?.let { notSyncedRecords ->
                    storeManager.querySkuDetails(
                        notSyncedRecords.mapNotNull { it.purchase.skus.firstOrNull() },
                        maxAttemptCount,
                    ).flatMapConcat { skuDetailsList ->
                        val notSynced =
                            notSyncedRecords.map { record ->
                                productMapper.mapToRestore(
                                    record,
                                    skuDetailsList
                                        .firstOrNull { it.sku == record.purchase.skus.firstOrNull() }
                                )
                            }

                        sendToBackend(notSynced)
                            .map { response ->
                                cacheRepository.saveSyncedPurchases(
                                    savedPurchases.apply { addAll(notSynced) }
                                )
                                response
                            }
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
                                            productMapper.mapToValidate(product)
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
                                            productMapper.mapToValidate(product)
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