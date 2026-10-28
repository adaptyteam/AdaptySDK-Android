package com.adapty.internal.domain

import android.app.Activity
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.*
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.PendingPurchase
import com.adapty.internal.data.models.PurchaseResult
import com.adapty.internal.data.models.PurchaseResult.Success.State
import com.adapty.internal.data.models.StoreMessagesOutcome
import com.adapty.internal.data.models.StoreMessagesTrigger
import com.adapty.internal.data.models.requests.ValidateReceiptRequest
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
import com.android.billingclient.api.InAppMessageResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchasesInteractor(
    private val authInteractor: AuthInteractor,
    private val profileInteractor: ProfileInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
    private val productMapper: ProductMapper,
    private val profileMapper: ProfileMapper,
    private val offlineProfileManager: OfflineProfileManager,
    private val allowLocalPAL: Boolean,
    private val observerMode: Boolean,
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
        storeManager.pendingPurchaseApprovedHandler = { purchase ->
            execute {
                validatePendingPurchase(purchase.purchaseToken, purchase)
                    .catch { }
                    .collect()
            }
        }
        if (!observerMode) {
            storeManager.unsolicitedPurchaseHandler = { requestUnsolicitedPurchasesSync() }
        }
    }

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
                                    purchaseResult.state == State.PENDING -> {
                                        purchaseResult.purchase?.let { purchase ->
                                            savePendingPurchase(purchase, purchaseableProduct)
                                        }
                                        flowOf(Pending)
                                    }
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
            .onStart { makePurchasesInProgress.incrementAndGet() }
            .onCompletion {
                if (makePurchasesInProgress.decrementAndGet() == 0)
                    syncUnsolicitedPurchasesIfRequested()
            }
    }

    private val makePurchasesInProgress = AtomicInteger(0)
    private val unsolicitedPurchasesSyncRequested = AtomicBoolean(false)

    private fun requestUnsolicitedPurchasesSync() {
        unsolicitedPurchasesSyncRequested.set(true)
        if (makePurchasesInProgress.get() == 0)
            syncUnsolicitedPurchasesIfRequested()
    }

    fun syncPurchasesOnForeground() {
        if (!observerMode)
            requestUnsolicitedPurchasesSync()
    }

    private fun syncUnsolicitedPurchasesIfRequested() {
        if (!unsolicitedPurchasesSyncRequested.compareAndSet(true, false))
            return
        execute {
            syncPurchasesSemaphore.acquire()
            syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT)
                .onEach { syncPurchasesSemaphore.releaseQuietly() }
                .catch { syncPurchasesSemaphore.releaseQuietly() }
                .collect()
        }
    }

    private fun validatePurchase(
        purchase: Purchase,
        product: PurchaseableProduct,
    ): Flow<AdaptyPurchaseResult> {
        val validateData = ValidateReceiptRequest.create(
            cacheRepository.getProfileId(),
            purchase,
            product,
            cacheRepository.getOnboardingVariationId(),
        )
        return authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(validateData, purchase)
        }
            .map { (validationResult, request) ->
                cacheRepository.updateOnProfileReceived(
                    validationResult.profile,
                    request.currentDataWhenSent?.profileId,
                ).let { profile ->
                    Success(profileMapper.map(profile), purchase)
                }
            }
            .catch { e ->
                val key = product.vendorProductId
                cacheRepository.saveUnsyncedValidateData(key, validateData)
                if (e is AdaptyError && e.adaptyErrorCode in listOf(BAD_REQUEST, SERVER_ERROR)) {
                    storeManager.acknowledgeOrConsume(purchase, product).catch { }.collect()
                }
                if (!allowLocalPAL)
                    throw e

                emitAll(
                    flow { emit(cacheRepository.getProfile()) }
                        .zip(offlineProfileManager.getLocalPAL()) { profile, localPALData ->
                            if (localPALData == null)
                                throw e
                            val profile = profile
                                ?: cacheRepository.getProfile()
                                ?: offlineProfileManager.constructProfile()
                            Success(profileMapper.map(profile, localPALData), purchase)
                        }
                )
            }
    }

    private fun savePendingPurchase(purchase: Purchase, product: PurchaseableProduct) {
        val validateData = ValidateReceiptRequest.create(
            cacheRepository.getProfileId(),
            purchase,
            product,
            cacheRepository.getOnboardingVariationId(),
        )
        cacheRepository.savePendingPurchase(
            purchase.purchaseToken,
            PendingPurchase(validateData, System.currentTimeMillis()),
        )
    }

    fun syncPendingPurchases(): Flow<AdaptyProfile?> {
        val pendingPurchases = cacheRepository.getPendingPurchases()?.takeIf { it.isNotEmpty() }
            ?: return flowOf(null)

        return storeManager.queryActivePurchasesIncludingPending(DEFAULT_RETRY_COUNT)
            .flatMapConcat { activePurchases ->
                val purchasesByToken = activePurchases.associateBy { it.purchaseToken }
                val now = System.currentTimeMillis()
                pendingPurchases
                    .mapNotNull { (token, pendingPurchase) ->
                        val purchase = purchasesByToken[token]
                        val action = resolvePendingPurchaseAction(purchase?.purchaseState, pendingPurchase.savedAt, now)
                        when (action) {
                            PendingPurchaseAction.VALIDATE -> purchase
                            PendingPurchaseAction.KEEP -> null
                            PendingPurchaseAction.DROP -> {
                                Logger.log(INFO) { "Pending purchase of ${pendingPurchase.validateData.vendorProductId} is no longer returned by Google Play, dropping it" }
                                cacheRepository.removePendingPurchase(token)
                                null
                            }
                        }
                    }
                    .asFlow()
            }
            .flatMapConcat { purchase ->
                validatePendingPurchase(purchase.purchaseToken, purchase)
                    .catch { }
            }
    }

    private val pendingPurchasesInProgress = ConcurrentHashMap.newKeySet<String>()

    private fun validatePendingPurchase(
        purchaseToken: String,
        purchase: Purchase?,
    ): Flow<AdaptyProfile?> {
        val pendingPurchase = cacheRepository.getPendingPurchase(purchaseToken)
            ?: return flowOf(null)
        if (!pendingPurchasesInProgress.add(purchaseToken))
            return flowOf(null)

        Logger.log(INFO) { "Pending purchase of ${pendingPurchase.validateData.vendorProductId} has been approved, validating it" }

        return authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(pendingPurchase.validateData, purchase)
        }
            .map { (validationResult, request) ->
                cacheRepository.removePendingPurchase(purchaseToken)
                cacheRepository.updateOnProfileReceived(
                    validationResult.profile,
                    request.currentDataWhenSent?.profileId,
                ).let(profileMapper::map)
            }
            .catch { e ->
                if (e is AdaptyError && e.adaptyErrorCode == BAD_REQUEST) {
                    Logger.log(WARN) { "Pending purchase of ${pendingPurchase.validateData.vendorProductId} was rejected on validation, dropping it" }
                    cacheRepository.removePendingPurchase(purchaseToken)
                }
                throw e
            }
            .onCompletion { pendingPurchasesInProgress.remove(purchaseToken) }
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

    suspend fun syncUnsyncedValidateData(): Flow<Any> {
        cacheRepository.getUnsyncedValidateData()?.takeIf { it.isNotEmpty() } ?: return flowOf(Unit)

        syncValidateDataSemaphore.acquire()
        val (key, validateData) = cacheRepository.getUnsyncedValidateData()?.entries?.firstOrNull() ?: run {
            syncValidateDataSemaphore.releaseQuietly()
            return flowOf(Unit)
        }

        return authInteractor.runWhenAuthDataSynced {
            cloudRepository.validatePurchase(validateData, null)
        }
            .onEach { (validationResult, request) ->
                cacheRepository.removeUnsyncedValidateData(key)
                if (validationResult.profile.profileId == cacheRepository.getProfileId()) {
                    cacheRepository.updateOnProfileReceived(
                        validationResult.profile,
                        request.currentDataWhenSent?.profileId,
                    )
                }
                syncValidateDataSemaphore.releaseQuietly()
            }
            .catch { error -> syncValidateDataSemaphore.releaseQuietly(); throw error }
    }

    fun restorePurchases() =
        syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT, byUser = true)

    private val syncPurchasesSemaphore = Semaphore(1)
    private val syncValidateDataSemaphore = Semaphore(1)

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

    suspend fun syncPurchasesOnStart(): Flow<AdaptyProfile> {
        syncPurchasesSemaphore.acquire()
        return syncPurchasesInternal(maxAttemptCount = DEFAULT_RETRY_COUNT)
            .onEach { syncPurchasesSemaphore.releaseQuietly() }
            .catch { error -> syncPurchasesSemaphore.releaseQuietly(); throw error }
    }

    @Suppress("DEPRECATION")
    private fun syncPurchasesInternal(
        maxAttemptCount: Long,
        byUser: Boolean = false,
    ): Flow<AdaptyProfile> {
        return storeManager.getPurchaseHistoryDataToRestore(maxAttemptCount)
            .zip(flowOf(cacheRepository.getSyncedPurchases())) { historyData, syncedPurchases ->
                historyData to syncedPurchases
            }
            .flatMapConcat { (historyData, syncedPurchases) ->
                val dataToSync = selectPurchasesToSync(
                    historyData,
                    syncedPurchases,
                    cacheRepository.getPendingPurchases()?.keys.orEmpty(),
                    byUser,
                )
                if (dataToSync.isNotEmpty()) {
                    storeManager.queryProductDetails(
                        dataToSync.mapNotNull { it.products.firstOrNull() },
                        maxAttemptCount,
                    ).flatMapConcat { (productDetailsList, _) ->
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
                        }.map { (profile, request) ->
                            if (cacheRepository.getProfileId() == request.currentDataWhenSent?.profileId && cacheRepository.getCustomerUserId() == request.currentDataWhenSent.customerUserId) {
                                cacheRepository.saveSyncedPurchases(
                                    dataToSync.map(productMapper::mapToSyncedPurchase)
                                        .union(syncedPurchases.filter { it.purchaseToken != null && it.purchaseTime != null })
                                )
                                cacheRepository.setPurchasesHaveBeenSynced(true)
                            }
                            cacheRepository.updateOnProfileReceived(
                                profile,
                                request.currentDataWhenSent?.profileId,
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

    fun showStoreMessages(
        hostActivity: WeakReference<Activity>,
        trigger: StoreMessagesTrigger,
    ): Flow<StoreMessagesOutcome> =
        storeManager.showInAppMessages(hostActivity, trigger)
            .flatMapConcat { outcome ->
                if (outcome.isSubscriptionStatusUpdated) {
                    syncPurchasesOnStart()
                        .map { outcome }
                        .catch { error ->
                            if ((error as? AdaptyError)?.adaptyErrorCode != NO_PURCHASES_TO_RESTORE) {
                                Logger.log(WARN) { "Purchase sync after store message failed: ${error.message}" }
                            }
                            emit(outcome)
                        }
                } else {
                    flowOf(outcome)
                }
            }

    private val StoreMessagesOutcome.isSubscriptionStatusUpdated: Boolean
        get() = this is StoreMessagesOutcome.Completed
                && responseCode == InAppMessageResult.InAppMessageResponseCode.SUBSCRIPTION_STATUS_UPDATED

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
                                .map { (validationResult, request) ->
                                    cacheRepository.updateOnProfileReceived(
                                        validationResult.profile,
                                        request.currentDataWhenSent?.profileId,
                                    ).let(profileMapper::map)
                                }
                        }
                }
        } else {
            restorePurchases()
        }
}