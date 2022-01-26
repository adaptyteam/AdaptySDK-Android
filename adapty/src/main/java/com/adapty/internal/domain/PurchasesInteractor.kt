package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PURCHASES_TO_RESTORE
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.Request
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
            .map { (response, currentDataWhenRequestSent) ->
                cacheRepository.updateOnPurchaserInfoReceived(
                    response.data?.attributes,
                    currentDataWhenRequestSent?.profileId,
                ).let { purchaserInfo ->
                    purchaserInfo to response.data?.attributes?.googleValidationResult
                }
            }

    @JvmSynthetic
    fun restorePurchases() =
        syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT, byUser = true) { dataToSync ->
            authInteractor.runWhenAuthDataSynced {
                cloudRepository.restorePurchases(dataToSync)
            }
        }
            .map { (response, currentDataWhenRequestSent) ->
                cacheRepository.updateOnPurchaserInfoReceived(
                    response.data?.attributes,
                    currentDataWhenRequestSent?.profileId,
                ).let { purchaserInfo ->
                    purchaserInfo to response.data?.attributes?.googleValidationResult
                }
            }
            .flowOnIO()

    @JvmSynthetic
    fun syncPurchasesOnStart(): Flow<Unit> {
        return syncPurchasesInternal { dataToSync ->
            authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                cloudRepository.restorePurchases(dataToSync)
            }
        }
            .map { (response, currentDataWhenRequestSent) ->
                cacheRepository.updateOnPurchaserInfoReceived(
                    response.data?.attributes,
                    currentDataWhenRequestSent?.profileId,
                )
                Unit
            }
            .flowOnIO()
    }

    private fun syncPurchasesInternal(
        maxAttemptCount: Long = INFINITE_RETRY,
        byUser: Boolean = false,
        sendToBackend: (List<RestoreProductInfo>) -> Flow<Pair<RestoreReceiptResponse, Request.CurrentDataWhenSent?>>
    ): Flow<Pair<RestoreReceiptResponse, Request.CurrentDataWhenSent?>> {
        return storeManager.getPurchaseHistoryDataToRestore(maxAttemptCount)
            .zip(flowOf(cacheRepository.getSyncedPurchases())) { historyData, syncedPurchases ->
                historyData to syncedPurchases
            }
            .flatMapConcat { (historyData, syncedPurchases) ->
                when {
                    byUser -> historyData
                    else -> {
                        historyData.filter { historyRecord ->
                            syncedPurchases.firstOrNull { purchase ->
                                purchase.purchaseToken == historyRecord.purchase.purchaseToken && purchase.purchaseTime == historyRecord.purchase.purchaseTime
                            } == null
                        }
                    }
                }.takeIf { it.isNotEmpty() }?.let { dataToSync ->
                    storeManager.querySkuDetails(
                        dataToSync.mapNotNull { it.purchase.skus.firstOrNull() },
                        maxAttemptCount,
                    ).flatMapConcat { skuDetailsList ->
                        sendToBackend(
                            dataToSync.map { historyRecord ->
                                productMapper.mapToRestore(
                                    historyRecord,
                                    skuDetailsList
                                        .firstOrNull { it.sku == historyRecord.purchase.skus.firstOrNull() }
                                )
                            }
                        )
                            .map { response ->
                                cacheRepository.saveSyncedPurchases(
                                    dataToSync.map(productMapper::mapToSyncedPurchase)
                                        .union(syncedPurchases.filter { it.purchaseToken != null && it.purchaseTime != null }).toHashSet()
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
                                    }.map { (response, currentDataWhenRequestSent) ->
                                        cacheRepository.updateOnPurchaserInfoReceived(
                                            response.data?.attributes,
                                            currentDataWhenRequestSent?.profileId,
                                        )
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
                                    }.map { (response, currentDataWhenRequestSent) ->
                                        cacheRepository.updateOnPurchaserInfoReceived(
                                            response.data?.attributes,
                                            currentDataWhenRequestSent?.profileId,
                                        )
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