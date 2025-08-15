package com.adapty.internal.domain

import android.app.Activity
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.*
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.PurchaseResult
import com.adapty.internal.data.models.PurchaseResult.Success.State
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.models.AdaptyPurchaseResult.Pending
import com.adapty.models.AdaptyPurchaseResult.Success
import com.adapty.models.AdaptyPurchaseResult.UserCanceled
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.adapty.utils.TransactionInfo
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchasesInteractor(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
    private val productMapper: ProductMapper,
    private val profileMapper: ProfileMapper,
) {

    init {
        execute {
            profileInteractor.subscribeOnEventsForStartRequests()
                .onEach { (newProfileIdDuringThisSession, newCustomerUserIdDuringThisSession) ->
                    if (newProfileIdDuringThisSession || newCustomerUserIdDuringThisSession) {
                        syncPurchasesSemaphore.releaseQuietly()
                    }
                }
                .catch { }
                .collect()
        }
    }

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        product: AdaptyPaywallProduct,
        params: AdaptyPurchaseParameters,
    ) : Flow<AdaptyPurchaseResult> {
        return storeManager.queryInfoForProduct(product.vendorProductId, product.payloadData.type)
            .flatMapConcat { productDetails ->
                val purchaseableProduct = productMapper.mapToPurchaseableProduct(
                    product,
                    productDetails,
                    params.isOfferPersonalized,
                )
                flow {
                    emit(
                        makePurchase(
                            activity,
                            purchaseableProduct,
                            params,
                        )
                    )
                }
                    .flatMapConcat { purchaseResult ->
                        when(purchaseResult) {
                            is PurchaseResult.Success -> {
                                when {
                                    purchaseResult.state == State.PENDING -> flowOf(Pending)
                                    purchaseResult.purchase != null -> {
                                        validatePurchase(purchaseResult.purchase, purchaseableProduct)
                                    }
                                    else -> {
                                        profileInteractor.getProfile()
                                            .map { profile -> Success(profile, null) }
                                    }
                                }
                            }
                            is PurchaseResult.Canceled -> flowOf(UserCanceled)
                            is PurchaseResult.Error -> {
                                val error = purchaseResult.error
                                if (error.adaptyErrorCode == ITEM_ALREADY_OWNED) {
                                    storeManager.findActivePurchaseForProduct(
                                        product.vendorProductId,
                                        product.payloadData.type,
                                    ).flatMapConcat { purchase ->
                                        if (purchase == null) throw error

                                        validatePurchase(purchase, purchaseableProduct)
                                    }
                                } else {
                                    throw error
                                }
                            }
                        }
                    }
            }
    }

    private fun validatePurchase(
        purchase: Purchase,
        product: PurchaseableProduct,
    ): Flow<AdaptyPurchaseResult> =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(purchase, product)
        }
            .catch { e ->
                if (e is AdaptyError && e.adaptyErrorCode in listOf(BAD_REQUEST, SERVER_ERROR)) {
                    storeManager.acknowledgeOrConsume(purchase, product).catch { }.collect()
                }
                throw e
            }
            .map { (profile, currentDataWhenRequestSent) ->
                cacheRepository.updateOnProfileReceived(
                    profile,
                    currentDataWhenRequestSent?.profileId,
                ).let { profile ->
                    Success(profileMapper.map(profile), purchase)
                }
            }

    private suspend fun makePurchase(
        activity: Activity,
        purchaseableProduct: PurchaseableProduct,
        params: AdaptyPurchaseParameters,
    ) = suspendCancellableCoroutine<PurchaseResult> { continuation ->
        storeManager.makePurchase(
            activity,
            purchaseableProduct,
            params,
        ) { purchaseResult ->
            continuation.resume(purchaseResult) {}
        }
    }

    @JvmSynthetic
    fun restorePurchases() =
        syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT, byUser = true)

    private val syncPurchasesSemaphore = Semaphore(1)

    @JvmSynthetic
    suspend fun syncPurchasesIfNeeded(): Flow<AdaptyProfile?> {
        if (cacheRepository.getPurchasesHaveBeenSynced()) {
            return flowOf(null)
        }

        syncPurchasesSemaphore.acquire()
        return if (cacheRepository.getPurchasesHaveBeenSynced()) {
            syncPurchasesSemaphore.releaseQuietly()
            flowOf(null)
        } else {
            syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT)
                .onEach { syncPurchasesSemaphore.releaseQuietly() }
                .catch { error -> syncPurchasesSemaphore.releaseQuietly(); throw error }
        }
    }

    @JvmSynthetic
    suspend fun syncPurchasesOnStart(): Flow<AdaptyProfile> {
        syncPurchasesSemaphore.acquire()
        return syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT)
            .onEach { syncPurchasesSemaphore.releaseQuietly() }
            .catch { error -> syncPurchasesSemaphore.releaseQuietly(); throw error }
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
                    storeManager.queryProductDetails(
                        dataToSync.mapNotNull { it.products.firstOrNull() },
                        maxAttemptCount,
                    ).flatMapConcat { productDetailsList ->
                        authInteractor.runWhenAuthDataSynced(maxAttemptCount) {
                            cloudRepository.restorePurchases(
                                dataToSync.map { historyRecord ->
                                    productMapper.mapToRestore(
                                        historyRecord,
                                        productDetailsList
                                            .firstOrNull { it.productId == historyRecord.products.firstOrNull() }
                                    )
                                }
                            )
                        }.map { (profile, currentDataWhenRequestSent) ->
                            if (cacheRepository.getProfileId() == currentDataWhenRequestSent?.profileId && cacheRepository.getCustomerUserId() == currentDataWhenRequestSent.customerUserId) {
                                cacheRepository.saveSyncedPurchases(
                                    dataToSync.map(productMapper::mapToSyncedPurchase)
                                        .union(syncedPurchases.filter { it.purchaseToken != null && it.purchaseTime != null })
                                )
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
    }

    @JvmSynthetic
    fun reportTransaction(transactionInfo: TransactionInfo, variationId: String?) =
        if (variationId != null) {
            (when (transactionInfo) {
                is TransactionInfo.Purchase -> flowOf(transactionInfo.purchase)
                is TransactionInfo.Id -> {
                    val transactionId = transactionInfo.transactionId
                    storeManager.findPurchaseForTransactionId(transactionId, DEFAULT_RETRY_COUNT)
                }
            })
                .flatMapConcat findPurchase@{ purchase ->
                    val transactionId =
                        (transactionInfo as? TransactionInfo.Id)?.transactionId ?: purchase?.orderId
                    if (transactionId == null) {
                        val errorMessage = "orderId in Purchase should not be null"
                        Logger.log(ERROR) { errorMessage }
                        throw AdaptyError(
                            message = errorMessage,
                            adaptyErrorCode = WRONG_PARAMETER
                        )
                    }

                    if (purchase == null) {
                        Logger.log(WARN) { "Purchase $transactionId was not found in active purchases" }
                        return@findPurchase restorePurchases()
                            .map { profile ->
                                cloudRepository.setVariationId(transactionId, variationId)
                                profile
                            }
                    }

                    storeManager.findProductDetailsForPurchase(
                        purchase,
                        DEFAULT_RETRY_COUNT,
                    )
                        .flatMapConcat findProduct@{ product ->
                            if (product == null) {
                                Logger.log(WARN) { "Product was not found for purchase (${purchase.products})" }
                                return@findProduct restorePurchases()
                                    .map { profile ->
                                        cloudRepository.setVariationId(
                                            transactionId,
                                            variationId
                                        )
                                        profile
                                    }
                            }

                            authInteractor.runWhenAuthDataSynced {
                                cloudRepository.reportTransactionWithVariation(
                                    transactionId,
                                    variationId,
                                    purchase,
                                    product,
                                )
                            }
                                .map { (profile, currentDataWhenRequestSent) ->
                                    cacheRepository.updateOnProfileReceived(
                                        profile,
                                        currentDataWhenRequestSent?.profileId,
                                    ).let(profileMapper::map)
                                }
                        }
                }
        } else {
            restorePurchases()
        }
}