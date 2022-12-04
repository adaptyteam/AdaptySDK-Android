package com.adapty.internal.data.cloud

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PurchaseHistoryRecordModel
import com.adapty.internal.utils.*
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClient.SkuType.SUBS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StoreManager(
    context: Context,
    private val prorationModeMapper: ProrationModeMapper,
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient
        .newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    private val storeHelper = StoreHelper(billingClient)

    private var makePurchaseCallback: MakePurchaseCallback? = null

    @JvmSynthetic
    fun getPurchaseHistoryDataToRestore(maxAttemptCount: Long): Flow<List<PurchaseHistoryRecordModel>> =
        getPurchaseHistoryDataToRestoreForType(SUBS, maxAttemptCount)
            .flatMapConcat { subsHistoryList ->
                getPurchaseHistoryDataToRestoreForType(INAPP, maxAttemptCount)
                    .map { inAppHistoryList -> concatResults(subsHistoryList, inAppHistoryList) }
            }

    private fun getPurchaseHistoryDataToRestoreForType(
        @BillingClient.SkuType type: String,
        maxAttemptCount: Long,
    ): Flow<List<PurchaseHistoryRecordModel>> {
        return onConnected {
            storeHelper.queryPurchaseHistoryForType(type)
                .map { purchaseHistoryRecordList ->
                    purchaseHistoryRecordList.map { purchase ->
                        PurchaseHistoryRecordModel(
                            purchase,
                            type,
                            type.takeIf { it == SUBS }?.let {
                                billingClient.queryPurchases(SUBS).purchasesList
                                    ?.firstOrNull { it.purchaseToken == purchase.purchaseToken }
                                    ?.orderId
                            }
                        )
                    }
                }
        }.retryOnConnectionError(maxAttemptCount)
    }

    @JvmSynthetic
    fun querySkuDetails(
        skuList: List<String>,
        maxAttemptCount: Long,
    ): Flow<List<SkuDetails>> =
        querySkuDetailsForType(
            SkuDetailsParams.newBuilder().setSkusList(skuList).setType(SUBS).build(),
            maxAttemptCount
        )
            .flatMapConcat { subsList ->
                querySkuDetailsForType(
                    SkuDetailsParams.newBuilder().setSkusList(skuList).setType(INAPP).build(),
                    maxAttemptCount
                ).map { inAppList -> concatResults(subsList, inAppList) }
            }

    private fun querySkuDetailsForType(
        params: SkuDetailsParams,
        maxAttemptCount: Long,
    ): Flow<List<SkuDetails>> {
        return onConnected {
            storeHelper.querySkuDetailsForType(params)
        }.retryOnConnectionError(maxAttemptCount)
    }

    private fun <T> concatResults(list1: List<T>, list2: List<T>): List<T> =
        ArrayList(list1).apply { addAll(list2) }

    private fun onError(
        billingResult: BillingResult,
        callback: MakePurchaseCallback?
    ) {
        val message = storeHelper.errorMessageFromBillingResult(billingResult, "on purchases updated")
        Logger.log(ERROR) { message }
        callback?.invoke(
            null, AdaptyError(
                message = message,
                adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)
            )
        )
    }

    private fun onError(
        error: Throwable,
        callback: MakePurchaseCallback?
    ) {
        val message = error.message ?: error.localizedMessage ?: "Unknown billing error occured"
        Logger.log(ERROR) { message }
        callback?.invoke(
            null,
            (error as? AdaptyError) ?: AdaptyError(
                originalError = error,
                message = message,
                adaptyErrorCode = AdaptyErrorCode.UNKNOWN
            )
        )
    }

    private val productsToPurchaseSkuType = hashMapOf<String, String>()

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            OK -> {
                if (purchases == null) {
                    makePurchaseCallback?.invoke(null, null)
                    return
                }

                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        makePurchaseCallback?.invoke(purchase, null)
                    } else {
                        makePurchaseCallback?.invoke(purchase, AdaptyError(
                            message = "Purchase: PENDING_PURCHASE",
                            adaptyErrorCode = AdaptyErrorCode.PENDING_PURCHASE
                        ))
                    }
                }
            }
            USER_CANCELED -> {
                makePurchaseCallback?.invoke(
                    null, AdaptyError(
                        message = "Purchase: USER_CANCELED",
                        adaptyErrorCode = AdaptyErrorCode.USER_CANCELED
                    )
                )
            }
            else -> {
                onError(billingResult, makePurchaseCallback)
            }
        }
    }

    @JvmSynthetic
    fun postProcess(purchase: Purchase) =
        when {
            purchase.isAcknowledged -> flowOf(Unit)
            else -> when (productsToPurchaseSkuType[purchase.skus.firstOrNull().orEmpty()]) {
                INAPP -> consumePurchase(purchase, maxAttemptCount = DEFAULT_RETRY_COUNT)
                else -> acknowledgePurchase(purchase, maxAttemptCount = DEFAULT_RETRY_COUNT)
            }
        }

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        productId: String,
        purchaseType: String,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters?,
        callback: MakePurchaseCallback
    ) {
        execute {
            onConnected {
                storeHelper.querySkuDetailsForType(
                    params = SkuDetailsParams.newBuilder().setSkusList(listOf(productId))
                        .setType(purchaseType).build(),
                ).flatMapConcat { skuDetailsForNewProduct ->
                    if (subscriptionUpdateParams != null) {
                        onConnected {
                            storeHelper.queryPurchaseHistoryForType(SUBS)
                                .map {
                                    buildSubscriptionUpdateParams(
                                        billingClient.queryPurchases(SUBS).purchasesList,
                                        subscriptionUpdateParams,
                                    ).let { updateParams -> skuDetailsForNewProduct to updateParams }
                                }
                        }
                    } else {
                        flowOf(skuDetailsForNewProduct to null)
                    }
                }
            }
                .flowOnIO()
                .catch { error -> onError(error, callback) }
                .onEach { (skuDetailsList, billingFlowSubUpdateParams) ->
                    skuDetailsList.firstOrNull { it.sku == productId }?.let { skuDetails ->
                        productsToPurchaseSkuType[productId] = purchaseType
                        makePurchaseCallback = MakePurchaseCallbackWrapper(
                            productId,
                            subscriptionUpdateParams?.oldSubVendorProductId,
                            callback,
                        )

                        billingClient.launchBillingFlow(
                            activity,
                            BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetails)
                                .apply { billingFlowSubUpdateParams?.let(::setSubscriptionUpdateParams) }
                                .build()
                        )
                    } ?: callback.invoke(
                        null,
                        AdaptyError(
                            message = "This product_id was not found with this purchase type",
                            adaptyErrorCode = AdaptyErrorCode.PRODUCT_NOT_FOUND
                        )
                    )
                }
                .flowOnMain()
                .collect()
        }
    }

    private fun buildSubscriptionUpdateParams(
        purchasesList: List<Purchase>?,
        subscriptionUpdateParams: AdaptySubscriptionUpdateParameters,
    ): BillingFlowParams.SubscriptionUpdateParams =
        purchasesList
            ?.firstOrNull { it.skus.firstOrNull() == subscriptionUpdateParams.oldSubVendorProductId }
            ?.let { subToBeReplaced ->
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldSkuPurchaseToken(subToBeReplaced.purchaseToken)
                    .setReplaceSkusProrationMode(
                        prorationModeMapper.map(subscriptionUpdateParams.prorationMode)
                    )
                    .build()
            }
            ?: "Can't launch flow to change subscription. Either subscription to change is inactive, or it was purchased from different Google account or from iOS".let { errorMessage ->
                Logger.log(ERROR) { errorMessage }
                throw AdaptyError(
                    message = errorMessage,
                    adaptyErrorCode = AdaptyErrorCode.CURRENT_SUBSCRIPTION_TO_UPDATE_NOT_FOUND_IN_HISTORY
                )
            }

    @JvmSynthetic
    fun acknowledgePurchase(purchase: Purchase, maxAttemptCount: Long = INFINITE_RETRY) =
        onConnected {
            storeHelper.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
        }
            .retryOnConnectionError(maxAttemptCount)
            .flowOnIO()

    @JvmSynthetic
    fun queryActiveSubsAndInApps(maxAttemptCount: Long) =
        onConnected {
            storeHelper.queryActivePurchasesForType(SUBS)
                .flatMapConcat { activeSubs ->
                    storeHelper.queryActivePurchasesForType(INAPP)
                        .map { inapps -> activeSubs to inapps }
                }
        }
            .retryOnConnectionError(maxAttemptCount)
            .flowOnIO()

    @JvmSynthetic
    fun findActivePurchaseForProduct(
        productId: String,
        @BillingClient.SkuType productType: String
    ) =
        billingClient.queryPurchases(productType).purchasesList
            ?.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.skus.firstOrNull() == productId }

    @JvmSynthetic
    fun consumePurchase(purchase: Purchase, maxAttemptCount: Long = INFINITE_RETRY) =
        onConnected {
            storeHelper.consumePurchase(
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
        }
            .retryOnConnectionError(maxAttemptCount)
            .flowOnIO()

    private fun <T> onConnected(call: () -> Flow<T>): Flow<T> =
        restoreConnection()
            .flatMapLatest { call() }

    private fun restoreConnection(): Flow<Unit> =
        flow {
            emit(billingClient.startConnectionSync())
        }
            .take(1)

    private val startConnectionSemaphore = Semaphore(1)

    private suspend fun BillingClient.startConnectionSync() {
        startConnectionSemaphore.acquire()
        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (!resumed) {
                        if (billingResult.responseCode == OK) {
                            continuation.resume(Unit) {}
                        } else {
                            continuation.resumeWithException(
                                AdaptyError(
                                    message = "Play Market request failed: ${billingResult.debugMessage}",
                                    adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)
                                )
                            )
                        }
                        resumed = true
                        startConnectionSemaphore.release()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (!resumed) {
                        continuation.resumeWithException(
                            AdaptyError(
                                message = "Play Market request failed: SERVICE_DISCONNECTED",
                                adaptyErrorCode = AdaptyErrorCode.fromBilling(SERVICE_DISCONNECTED)
                            )
                        )
                        resumed = true
                        startConnectionSemaphore.release()
                    }
                }
            })
        }
    }

    private fun <T> Flow<T>.retryOnConnectionError(maxAttemptCount: Long = INFINITE_RETRY): Flow<T> =
        this.retryWhen { error, attempt ->
            if (canRetry(error, attempt, maxAttemptCount)) {
                delay(2000)
                return@retryWhen true
            } else {
                Logger.log(ERROR) {
                    error.message ?: error.localizedMessage ?: "Unknown billing error occured"
                }
                return@retryWhen false
            }
        }

    private fun canRetry(error: Throwable, attempt: Long, maxAttemptCount: Long): Boolean {
        return when {
            maxAttemptCount in 0..attempt -> false
            error !is AdaptyError || error.originalError is IOException || error.adaptyErrorCode in arrayOf(
                AdaptyErrorCode.BILLING_SERVICE_DISCONNECTED,
                AdaptyErrorCode.BILLING_SERVICE_UNAVAILABLE,
                AdaptyErrorCode.BILLING_SERVICE_TIMEOUT
            ) -> true
            error.adaptyErrorCode == AdaptyErrorCode.BILLING_ERROR
                    && ((maxAttemptCount.takeIf { it in 0..DEFAULT_RETRY_COUNT } ?: DEFAULT_RETRY_COUNT) > attempt) -> true
            else -> false
        }
    }
}

private class StoreHelper(private val billingClient: BillingClient) {

    @JvmSynthetic
    fun querySkuDetailsForType(params: SkuDetailsParams) =
        flow {
            val skuDetailsResult = billingClient.querySkuDetails(params)
            if (skuDetailsResult.billingResult.responseCode == OK) {
                emit(
                    skuDetailsResult.skuDetailsList.orEmpty()
                )
            } else {
                throwException(skuDetailsResult.billingResult, "on query product details")
            }
        }

    @JvmSynthetic
    fun queryPurchaseHistoryForType(@BillingClient.SkuType type: String) =
        flow {
            val purchaseHistoryResult = billingClient.queryPurchaseHistory(type)
            if (purchaseHistoryResult.billingResult.responseCode == OK) {
                emit(purchaseHistoryResult.purchaseHistoryRecordList.orEmpty())
            } else {
                throwException(purchaseHistoryResult.billingResult, "on query history")
            }
        }

    @JvmSynthetic
    fun queryActivePurchasesForType(@BillingClient.SkuType type: String) =
        queryPurchaseHistoryForType(type)
            .map {
                billingClient.queryPurchases(type).purchasesList
                    ?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.orEmpty()
            }

    @JvmSynthetic
    fun acknowledgePurchase(params: AcknowledgePurchaseParams) =
        flow {
            val result = billingClient.acknowledgePurchase(params)
            if (result.responseCode == OK) {
                emit(Unit)
            } else {
                throwException(result, "on acknowledge")
            }
        }

    @JvmSynthetic
    fun consumePurchase(params: ConsumeParams) =
        flow {
            val result = billingClient.consumePurchase(params).billingResult
            if (result.responseCode == OK) {
                emit(Unit)
            } else {
                throwException(result, "on consume")
            }
        }

    @JvmSynthetic
    fun errorMessageFromBillingResult(billingResult: BillingResult, where: String) =
        "Play Market request failed $where: responseCode=${billingResult.responseCode}${
            billingResult.debugMessage.takeIf(String::isNotEmpty)?.let { msg -> ", debugMessage=$msg" }.orEmpty()
        }"

    private fun throwException(billingResult: BillingResult, where: String) {
        val message = errorMessageFromBillingResult(billingResult, where)
        throw AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)
        )
    }
}

private typealias MakePurchaseCallback = (purchase: Purchase?, error: AdaptyError?) -> Unit

private class MakePurchaseCallbackWrapper(
    private val productId: String,
    private val oldSubProductId: String?,
    private val callback: MakePurchaseCallback
) : MakePurchaseCallback {

    private val wasInvoked = AtomicBoolean(false)

    override operator fun invoke(purchase: Purchase?, error: AdaptyError?) {
        val purchaseSku = purchase?.skus?.firstOrNull()
        if (purchaseSku == null || listOfNotNull(productId, oldSubProductId).contains(purchaseSku)) {
            if (wasInvoked.compareAndSet(false, true)) {
                callback.invoke(if (error == null && productId == purchaseSku) purchase else null, error)
            }
        }
    }
}