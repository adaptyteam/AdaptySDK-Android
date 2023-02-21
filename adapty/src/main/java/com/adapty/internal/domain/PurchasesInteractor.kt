package com.adapty.internal.domain

import android.app.Activity
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.*
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.ValidateProductInfo
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.resumeWithException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchasesInteractor(
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
    private val productMapper: ProductMapper,
    private val profileMapper: ProfileMapper,
) {

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
    ) : Flow<AdaptyProfile?> {
        val purchaseProductInfo = productMapper.mapToMakePurchase(product)
        val validateProductInfo = productMapper.mapToValidate(purchaseProductInfo)

        return flow {
            emit(saveValidateProductInfo(validateProductInfo))
        }
            .map {
                makePurchase(
                    activity,
                    purchaseProductInfo.vendorProductId,
                    purchaseProductInfo.type,
                    subscriptionUpdateParams,
                )
            }
            .catch { error ->
                when {
                    error !is AdaptyError || error.adaptyErrorCode !in listOf(ITEM_ALREADY_OWNED, PENDING_PURCHASE) -> {
                        deleteValidateProductInfo(validateProductInfo)
                    }
                }
                throw error
            }
            .flatMapConcat { purchase ->
                if (purchase != null) {
                    storeManager.postProcess(purchase)
                        .flatMapConcat {
                            validatePurchase(purchase, purchaseProductInfo.type, validateProductInfo)
                        }
                } else {
                    flowOf(null)
                }
            }
            .catch { error ->
                if (error is AdaptyError && error.adaptyErrorCode == ITEM_ALREADY_OWNED) {
                    val purchase = storeManager.findActivePurchaseForProduct(
                        purchaseProductInfo.vendorProductId,
                        purchaseProductInfo.type,
                    )
                    if (purchase != null) {
                        emitAll(
                            if (!purchase.isAcknowledged) {
                                storeManager.acknowledgePurchase(purchase, DEFAULT_RETRY_COUNT)
                                    .flatMapConcat {
                                        validatePurchase(purchase, purchaseProductInfo.type, validateProductInfo)
                                    }
                            } else {
                                validatePurchase(purchase, purchaseProductInfo.type, validateProductInfo)
                            }
                        )
                    } else {
                        throw error
                    }
                } else {
                    throw error
                }
            }
            .flowOnIO()
    }

    private fun validatePurchase(
        purchase: Purchase,
        type: String,
        validateProductInfo: ValidateProductInfo,
    ): Flow<AdaptyProfile> =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(
                type,
                purchase,
                validateProductInfo
            )
        }
            .map { (profile, currentDataWhenRequestSent) ->
                deleteValidateProductInfo(validateProductInfo)

                cacheRepository.updateOnProfileReceived(
                    profile,
                    currentDataWhenRequestSent?.profileId,
                ).let(profileMapper::map)
            }

    private suspend fun makePurchase(
        activity: Activity,
        productId: String,
        purchaseType: String,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
    ) = suspendCancellableCoroutine<Purchase?> { continuation ->
        storeManager.makePurchase(
            activity,
            productId,
            purchaseType,
            subscriptionUpdateParams,
        ) { purchase, error ->
            if (error == null) {
                continuation.resume(purchase) {}
            } else {
                continuation.resumeWithException(error)
            }
        }
    }

    private fun saveValidateProductInfo(validateProductInfo: ValidateProductInfo) {
        cacheRepository.saveValidateProductInfo(validateProductInfo)
    }

    private fun deleteValidateProductInfo(validateProductInfo: ValidateProductInfo) {
        cacheRepository.deleteValidateProductInfo(validateProductInfo)
    }

    @JvmSynthetic
    fun restorePurchases() =
        syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT, byUser = true)
            .flowOnIO()

    private val syncPurchasesSemaphore = Semaphore(1)

    @JvmSynthetic
    suspend fun syncPurchasesIfNeeded(): Flow<*> {
        if (cacheRepository.getPurchasesHaveBeenSynced()) {
            return flowOf(Unit)
        }

        syncPurchasesSemaphore.acquire()
        return if (cacheRepository.getPurchasesHaveBeenSynced()) {
            syncPurchasesSemaphore.release()
            flowOf(Unit)
        } else {
            syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT)
                .flowOnIO()
                .onEach { syncPurchasesSemaphore.release() }
                .catch { error -> syncPurchasesSemaphore.release(); throw error }
        }
    }

    @JvmSynthetic
    suspend fun syncPurchasesOnStart(): Flow<*> {
        syncPurchasesSemaphore.acquire()
        return syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT)
            .flowOnIO()
            .onEach { syncPurchasesSemaphore.release() }
            .catch { error -> syncPurchasesSemaphore.release(); throw error }
    }

    private fun syncPurchasesInternal(
        maxAttemptCount: Long = INFINITE_RETRY,
        byUser: Boolean = false,
    ): Flow<AdaptyProfile> {
        return storeManager.getPurchaseHistoryDataToRestore(maxAttemptCount)
            .zip(flowOf(cacheRepository.getSyncedPurchases())) { historyData, syncedPurchases ->
                historyData to syncedPurchases
            }
            .flatMapConcat { (historyData, syncedPurchases) ->
                val dataToSync = when {
                    byUser -> historyData
                    else -> {
                        historyData.filter { historyRecord ->
                            syncedPurchases.firstOrNull { purchase ->
                                purchase.purchaseToken == historyRecord.purchaseToken && purchase.purchaseTime == historyRecord.purchaseTime
                            } == null
                        }
                    }
                }
                if (dataToSync.isNotEmpty()) {
                    storeManager.querySkuDetails(
                        dataToSync.mapNotNull { it.skus.firstOrNull() },
                        maxAttemptCount,
                    ).flatMapConcat { skuDetailsList ->
                        authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
                            cloudRepository.restorePurchases(
                                dataToSync.map { historyRecord ->
                                    productMapper.mapToRestore(
                                        historyRecord,
                                        skuDetailsList
                                            .firstOrNull { it.sku == historyRecord.skus.firstOrNull() }
                                    )
                                }
                            )
                        }.map { (profile, currentDataWhenRequestSent) ->
                            cacheRepository.saveSyncedPurchases(
                                dataToSync.map(productMapper::mapToSyncedPurchase)
                                    .union(syncedPurchases.filter { it.purchaseToken != null && it.purchaseTime != null })
                            )
                            if (cacheRepository.getProfileId() == currentDataWhenRequestSent?.profileId) {
                                cacheRepository.setPurchasesHaveBeenSynced(true)
                            }
                            cacheRepository.updateOnProfileReceived(
                                profile,
                                currentDataWhenRequestSent?.profileId,
                            ).let(profileMapper::map)
                        }
                    }
                } else {
                    cacheRepository.setPurchasesHaveBeenSynced(true)
                    "No purchases to restore".let { message ->
                        Logger.log(INFO) { message }
                        throw AdaptyError(
                            message = message,
                            adaptyErrorCode = NO_PURCHASES_TO_RESTORE
                        )
                    }
                }
            }
            .flowOnIO()
    }

    @JvmSynthetic
    fun setVariationId(transactionId: String, variationId: String) =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.setVariationId(transactionId, variationId)
        }

    @JvmSynthetic
    suspend fun consumeAndAcknowledgeTheUnprocessed() {
        storeManager.queryActiveSubsAndInApps(INFINITE_RETRY)
            .map { (subs, inapps) ->
                if (inapps.isEmpty() && subs.isEmpty()) {
                    return@map emptyList<Flow<*>>()
                }

                val validateProductInfos = cacheRepository.getValidateProductInfos()

                val inappsFlows = inapps.mapNotNull { purchase ->
                    val validateProductInfo =
                        validateProductInfos.firstOrNull { it.vendorProductId == purchase.skus.firstOrNull() }
                            ?: return@mapNotNull null

                    storeManager.consumePurchase(purchase)
                        .flatMapConcat {
                            authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                                cloudRepository.validatePurchase(
                                    BillingClient.SkuType.INAPP,
                                    purchase,
                                    validateProductInfo,
                                )
                            }
                        }
                        .map { (profile, currentDataWhenRequestSent) ->
                            cacheRepository.updateOnProfileReceived(
                                profile,
                                currentDataWhenRequestSent?.profileId,
                            )
                        }
                        .catch { }
                }

                val subsFlows = subs.mapNotNull { purchase ->
                    val validateProductInfo =
                        validateProductInfos.firstOrNull { it.vendorProductId == purchase.skus.firstOrNull() }
                            ?: return@mapNotNull null

                    (if (!purchase.isAcknowledged) {
                        storeManager.acknowledgePurchase(purchase)
                    } else {
                        flowOf(Unit)
                    }).flatMapConcat {
                        authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
                            cloudRepository.validatePurchase(
                                BillingClient.SkuType.SUBS,
                                purchase,
                                validateProductInfo
                            )
                        }
                    }
                        .map { (profile, currentDataWhenRequestSent) ->
                            cacheRepository.updateOnProfileReceived(
                                profile,
                                currentDataWhenRequestSent?.profileId,
                            )
                        }
                        .catch { }
                }

                inappsFlows.toMutableList().apply { addAll(subsFlows) }
            }
            .flatMapConcat { it.asFlow() }
            .flattenConcat()
            .catch { }
            .collect()
    }
}