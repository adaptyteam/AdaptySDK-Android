@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.data.cloud

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.AnalyticsEvent.GoogleAPIRequestData
import com.adapty.internal.data.models.AnalyticsEvent.GoogleAPIResponseData
import com.adapty.internal.data.models.PurchaseRecordModel
import com.adapty.internal.data.models.PurchaseResult
import com.adapty.internal.data.models.PurchaseResult.Success.State
import com.adapty.internal.domain.models.ProductType.Consumable
import com.adapty.internal.domain.models.ProductType.Subscription
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.internal.utils.*
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.android.billingclient.api.BillingClient.ProductType.INAPP
import com.android.billingclient.api.BillingClient.ProductType.SUBS
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
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
    private val replacementModeMapper: ReplacementModeMapper,
    private val analyticsTracker: AnalyticsTracker,
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient
        .newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .setListener(this)
        .build()

    private val storeHelper = StoreHelper(billingClient, analyticsTracker)

    private var makePurchaseCallback: MakePurchaseCallback? = null

    @JvmSynthetic
    fun getPurchaseHistoryDataToRestore(maxAttemptCount: Long): Flow<List<PurchaseRecordModel>> =
        getPurchaseHistoryDataToRestoreForType(SUBS, maxAttemptCount)
            .flatMapConcat { subsHistoryList ->
                getPurchaseHistoryDataToRestoreForType(INAPP, maxAttemptCount)
                    .map { inAppHistoryList -> concatResults(subsHistoryList, inAppHistoryList) }
            }

    fun findPurchaseForTransactionId(transactionId: String, maxAttemptCount: Long): Flow<Purchase?> =
        queryActivePurchasesForType(SUBS, maxAttemptCount)
            .flatMapConcat { subsList ->
                val sub = subsList.firstOrNull { it.orderId == transactionId }
                if (sub == null) {
                    queryActivePurchasesForType(INAPP, maxAttemptCount)
                        .map { inAppList -> inAppList.firstOrNull { it.orderId == transactionId } }
                } else {
                    flowOf(sub)
                }
            }

    fun findProductDetailsForPurchase(
        purchase: Purchase,
        maxAttemptCount: Long,
    ): Flow<ProductDetails?> {
        val productList = purchase.products
        val productId = productList.firstOrNull() ?: return flowOf(null)
        return queryProductDetailsForType(
            productList,
            SUBS,
            maxAttemptCount
        )
            .flatMapConcat { subsList ->
                val sub = subsList.firstOrNull { it.productId == productId }
                if (sub == null) {
                    queryProductDetailsForType(
                        productList,
                        INAPP,
                        maxAttemptCount
                    )
                        .map { inAppList -> inAppList.firstOrNull { it.productId == productId } }
                } else {
                    flowOf(sub)
                }
            }
    }

    private fun getPurchaseHistoryDataToRestoreForType(
        @BillingClient.ProductType type: String,
        maxAttemptCount: Long,
    ): Flow<List<PurchaseRecordModel>> {
        return onConnected {
            storeHelper.queryAllPurchasesForType(type)
                .map { (historyRecords, activePurchases) ->
                    val purchases = mutableSetOf<PurchaseRecordModel>()

                    activePurchases.forEach { purchase ->
                        purchases.add(
                            PurchaseRecordModel(
                                purchase.purchaseToken,
                                purchase.purchaseTime,
                                purchase.products,
                                type,
                            )
                        )
                    }

                    historyRecords.forEach { historyRecord ->
                        purchases.add(
                            PurchaseRecordModel(
                                historyRecord.purchaseToken,
                                historyRecord.purchaseTime,
                                historyRecord.products,
                                type,
                            )
                        )
                    }

                    purchases.toList()
                }
        }.retryOnConnectionError(maxAttemptCount)
    }

    @JvmSynthetic
    fun queryProductDetails(
        productList: List<String>,
        maxAttemptCount: Long,
    ): Flow<List<ProductDetails>> =
        queryProductDetailsForType(
            productList,
            SUBS,
            maxAttemptCount
        )
            .flatMapConcat { subsList ->
                queryProductDetailsForType(
                    productList,
                    INAPP,
                    maxAttemptCount
                ).map { inAppList -> concatResults(subsList, inAppList) }
            }

    private fun queryProductDetailsForType(
        productList: List<String>,
        @BillingClient.ProductType productType: String,
        maxAttemptCount: Long,
    ): Flow<List<ProductDetails>> {
        return onConnected {
            storeHelper.queryProductDetailsForType(productList, productType)
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
            PurchaseResult.Error(
                AdaptyError(
                    message = message,
                    adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)
                )
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
            PurchaseResult.Error(
                (error as? AdaptyError) ?: AdaptyError(
                    originalError = error,
                    message = message,
                    adaptyErrorCode = AdaptyErrorCode.UNKNOWN
                )
            )
        )
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            OK -> {
                if (purchases == null) {
                    makePurchaseCallback?.invoke(PurchaseResult.Success())
                    return
                }

                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        makePurchaseCallback?.invoke(PurchaseResult.Success(purchase))
                    } else {
                        makePurchaseCallback?.invoke(PurchaseResult.Success(purchase, State.PENDING))
                    }
                }
            }
            USER_CANCELED -> {
                makePurchaseCallback?.invoke(PurchaseResult.Canceled)
            }
            else -> {
                onError(billingResult, makePurchaseCallback)
            }
        }
    }

    @JvmSynthetic
    fun queryInfoForProduct(productId: String, type: String) =
        onConnected {
            storeHelper.queryProductDetailsForType(listOf(productId), extractGoogleType(type))
        }.map { productDetailsList ->
            productDetailsList.firstOrNull { it.productId == productId }
                ?: throw AdaptyError(
                    message = "This product_id was not found with this purchase type",
                    adaptyErrorCode = AdaptyErrorCode.PRODUCT_NOT_FOUND
                )
        }

    @JvmSynthetic
    fun makePurchase(
        activity: Activity,
        purchaseableProduct: PurchaseableProduct,
        purchaseParams: AdaptyPurchaseParameters,
        callback: MakePurchaseCallback
    ) {
        val subscriptionUpdateParams = purchaseParams.subscriptionUpdateParams
        val requestEvent = GoogleAPIRequestData.MakePurchase.create(purchaseableProduct, subscriptionUpdateParams)
        analyticsTracker.trackSystemEvent(requestEvent)
        execute {
            if (subscriptionUpdateParams != null) {
                onConnected {
                    storeHelper.queryActivePurchasesForTypeWithSync(SUBS)
                        .map { activeSubscriptions ->
                            buildSubscriptionUpdateParams(
                                activeSubscriptions,
                                subscriptionUpdateParams,
                            ).let { updateParams -> purchaseableProduct.productDetails to updateParams }
                        }
                }
            } else {
                flowOf(purchaseableProduct.productDetails to null)
            }
                .catch { error ->
                    analyticsTracker.trackSystemEvent(GoogleAPIResponseData.MakePurchase.create(requestEvent, PurchaseResult.Error(error.asAdaptyError())))
                    onError(error, callback)
                }
                .onEach { (productDetails, billingFlowSubUpdateParams) ->
                    makePurchaseCallback = MakePurchaseCallbackWrapper(
                        productDetails.productId,
                        subscriptionUpdateParams?.oldSubVendorProductId,
                        requestEvent,
                        analyticsTracker,
                        callback,
                    )

                    val params = ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .apply { purchaseableProduct.currentOfferDetails?.offerToken?.let(::setOfferToken) }
                        .build()

                    billingClient.launchBillingFlow(
                        activity,
                        BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(listOf(params))
                            .apply {
                                purchaseableProduct.isOfferPersonalized.takeIf { it }?.let(::setIsOfferPersonalized)
                                purchaseParams.obfuscatedAccountId?.let(::setObfuscatedAccountId)
                                purchaseParams.obfuscatedProfileId?.let(::setObfuscatedProfileId)
                                billingFlowSubUpdateParams?.let(::setSubscriptionUpdateParams)
                            }
                            .build()
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
            ?.firstOrNull { it.products.firstOrNull() == subscriptionUpdateParams.oldSubVendorProductId }
            ?.let { subToBeReplaced ->
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(subToBeReplaced.purchaseToken)
                    .setSubscriptionReplacementMode(
                        replacementModeMapper.map(subscriptionUpdateParams.replacementMode)
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
    fun acknowledgeOrConsume(purchase: Purchase, product: PurchaseableProduct) =
        onConnected {
            if (product.type == Consumable.NAME) {
                storeHelper.consumePurchase(purchase)
            } else {
                storeHelper.acknowledgePurchase(purchase)
            }
        }
            .retryOnConnectionError(DEFAULT_RETRY_COUNT)

    @JvmSynthetic
    fun getStoreCountry() =
        onConnected {
            val params = GetBillingConfigParams.newBuilder().build()
            storeHelper.getBillingConfig(params)
        }
            .catch { e ->
                Logger.log(WARN) { e.message ?: e.localizedMessage ?: "Unknown error occured on get billing config" }
                throw e
            }
            .map { config -> config?.countryCode }

    @JvmSynthetic
    fun findActivePurchaseForProduct(
        productId: String,
        type: String,
    ) =
        queryActivePurchasesForType(extractGoogleType(type), DEFAULT_RETRY_COUNT)
            .map { purchases ->
                purchases.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.firstOrNull() == productId
                }
            }

    private fun queryActivePurchasesForType(
        @BillingClient.ProductType type: String,
        maxAttemptCount: Long,
    ): Flow<List<Purchase>> {
        return onConnected {
            storeHelper.queryActivePurchasesForType(type)
        }
            .retryOnConnectionError(maxAttemptCount)
    }

    private fun extractGoogleType(type: String) =
        when (type) {
            Subscription.NAME -> SUBS
            else -> INAPP
        }

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
            startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (continuation.isActive) {
                        runCatching {
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
                            startConnectionSemaphore.release()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) {
                        runCatching {
                            continuation.resumeWithException(
                                AdaptyError(
                                    message = "Play Market request failed: SERVICE_DISCONNECTED",
                                    adaptyErrorCode = AdaptyErrorCode.fromBilling(SERVICE_DISCONNECTED)
                                )
                            )
                            startConnectionSemaphore.release()
                        }
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
                AdaptyErrorCode.BILLING_NETWORK_ERROR,
            ) -> true
            error.adaptyErrorCode == AdaptyErrorCode.BILLING_ERROR
                    && ((maxAttemptCount.takeIf { it in 0..DEFAULT_RETRY_COUNT } ?: DEFAULT_RETRY_COUNT) > attempt) -> true
            else -> false
        }
    }
}

private class StoreHelper(
    private val billingClient: BillingClient,
    private val analyticsTracker: AnalyticsTracker,
) {

    @JvmSynthetic
    fun queryProductDetailsForType(productList: List<String>, @BillingClient.ProductType productType: String) =
        flow {
            val requestEvent = GoogleAPIRequestData.QueryProductDetails.create(productList, productType)
            analyticsTracker.trackSystemEvent(requestEvent)
            val params = QueryProductDetailsParams.newBuilder().setProductList(
                productList.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(productType).build()
                }
            ).build()
            val productDetailsResult = billingClient.queryProductDetails(params)
            if (productDetailsResult.billingResult.responseCode == OK) {
                emit(
                    productDetailsResult.productDetailsList.orEmpty()
                )
                analyticsTracker.trackSystemEvent(
                    GoogleAPIResponseData.QueryProductDetails.create(productDetailsResult.productDetailsList, requestEvent)
                )
            } else {
                val e = createException(productDetailsResult.billingResult, "on query product details")
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.QueryProductDetails.create(e, requestEvent))
                throw e
            }
        }

    @JvmSynthetic
    fun queryActivePurchasesForType(@BillingClient.ProductType type: String) =
        flow {
            val requestEvent = GoogleAPIRequestData.QueryActivePurchases.create(type)
            analyticsTracker.trackSystemEvent(requestEvent)
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            val purchasesResult = billingClient.queryPurchasesAsync(params)
            if (purchasesResult.billingResult.responseCode == OK) {
                emit(purchasesResult.purchasesList)
                analyticsTracker.trackSystemEvent(
                    GoogleAPIResponseData.QueryActivePurchases.create(purchasesResult.purchasesList, requestEvent)
                )
            } else {
                val e = createException(purchasesResult.billingResult, "on query active purchases")
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.QueryActivePurchases.create(e, requestEvent))
                throw e
            }
        }

    @JvmSynthetic
    fun queryPurchaseHistoryForType(@BillingClient.ProductType type: String) =
        flow {
            val requestEvent = GoogleAPIRequestData.QueryPurchaseHistory.create(type)
            analyticsTracker.trackSystemEvent(requestEvent)
            val params = QueryPurchaseHistoryParams.newBuilder().setProductType(type).build()
            val purchaseHistoryResult = billingClient.queryPurchaseHistory(params)
            if (purchaseHistoryResult.billingResult.responseCode == OK) {
                emit(purchaseHistoryResult.purchaseHistoryRecordList.orEmpty())
                analyticsTracker.trackSystemEvent(
                    GoogleAPIResponseData.QueryPurchaseHistory.create(purchaseHistoryResult.purchaseHistoryRecordList, requestEvent)
                )
            } else {
                val e = createException(purchaseHistoryResult.billingResult, "on query history")
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.QueryPurchaseHistory.create(e, requestEvent))
                throw e
            }
        }

    @JvmSynthetic
    fun queryAllPurchasesForType(@BillingClient.ProductType type: String) =
        queryPurchaseHistoryForType(type)
            .flatMapConcat { historyRecords ->
                queryActivePurchasesForType(type)
                    .map { activePurchases ->
                        historyRecords to activePurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    }
            }

    @JvmSynthetic
    fun queryActivePurchasesForTypeWithSync(@BillingClient.ProductType type: String) =
        queryAllPurchasesForType(type)
            .map { (_, activePurchases) -> activePurchases }

    @JvmSynthetic
    fun acknowledgePurchase(purchase: Purchase) =
        flow {
            val requestEvent = GoogleAPIRequestData.AcknowledgePurchase.create(purchase)
            analyticsTracker.trackSystemEvent(requestEvent)
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val result = billingClient.acknowledgePurchase(params)
            if (result.responseCode == OK) {
                emit(Unit)
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.create(requestEvent))
            } else {
                val e = createException(result, "on acknowledge")
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.create(requestEvent, e))
                throw e
            }
        }

    @JvmSynthetic
    fun consumePurchase(purchase: Purchase) =
        flow {
            val requestEvent = GoogleAPIRequestData.ConsumePurchase.create(purchase)
            analyticsTracker.trackSystemEvent(requestEvent)
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val result = billingClient.consumePurchase(params).billingResult
            if (result.responseCode == OK) {
                emit(Unit)
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.create(requestEvent))
            } else {
                val e = createException(result, "on consume")
                analyticsTracker.trackSystemEvent(GoogleAPIResponseData.create(requestEvent, e))
                throw e
            }
        }

    @JvmSynthetic
    fun getBillingConfig(params: GetBillingConfigParams) =
        flow {
            val (result, config) = getBillingConfigSync(params)
            if (result.responseCode == OK) {
                emit(config)
            } else {
                throw createException(result, "on get billing config")
            }
        }

    private suspend fun getBillingConfigSync(params: GetBillingConfigParams): Pair<BillingResult, BillingConfig?> {
        return suspendCancellableCoroutine { continuation ->
            billingClient.getBillingConfigAsync(params) { billingResult, billingConfig ->
                if (continuation.isActive) {
                    continuation.resume(billingResult to billingConfig) {}
                }
            }
        }
    }

    @JvmSynthetic
    fun errorMessageFromBillingResult(billingResult: BillingResult, where: String) =
        "Play Market request failed $where: responseCode=${billingResult.responseCode}${
            billingResult.debugMessage.takeIf(String::isNotEmpty)?.let { msg -> ", debugMessage=$msg" }.orEmpty()
        }"

    private fun createException(billingResult: BillingResult, where: String): AdaptyError {
        val message = errorMessageFromBillingResult(billingResult, where)
        return AdaptyError(
            message = message,
            adaptyErrorCode = AdaptyErrorCode.fromBilling(billingResult.responseCode)
        )
    }
}

private typealias MakePurchaseCallback = (PurchaseResult) -> Unit

private class MakePurchaseCallbackWrapper(
    private val productId: String,
    private val oldSubProductId: String?,
    private val requestEvent: GoogleAPIRequestData.MakePurchase,
    private val analyticsTracker: AnalyticsTracker,
    private val callback: MakePurchaseCallback
) : MakePurchaseCallback {

    private val wasInvoked = AtomicBoolean(false)

    override operator fun invoke(purchaseResult: PurchaseResult) {
        if (wasInvoked.compareAndSet(false, true)) {
            when(purchaseResult) {
                is PurchaseResult.Success -> {
                    val purchaseProductId = purchaseResult.productId
                    if (purchaseProductId != null && !listOfNotNull(productId, oldSubProductId).contains(purchaseProductId))
                        return
                    analyticsTracker.trackSystemEvent(GoogleAPIResponseData.MakePurchase.create(requestEvent, purchaseResult))
                    callback.invoke(purchaseResult)
                }
                is PurchaseResult.Error, is PurchaseResult.Canceled -> {
                    analyticsTracker.trackSystemEvent(GoogleAPIResponseData.MakePurchase.create(requestEvent, purchaseResult))
                    callback.invoke(purchaseResult)
                }
            }
        }
    }
}