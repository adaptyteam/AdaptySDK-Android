package com.adapty.internal.data.cloud

import com.android.billingclient.api.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.coroutines.resume

internal data class ProductDetailsResult(
    val billingResult: BillingResult,
    val productDetailsList: List<ProductDetails>,
)

internal data class PurchasesResult(
    val billingResult: BillingResult,
    val purchasesList: List<Purchase>,
)

internal data class PurchaseHistoryResult(
    val billingResult: BillingResult,
    val purchaseHistoryRecordList: List<PurchaseHistoryRecord>?,
)

internal data class ConsumeResult(
    val billingResult: BillingResult,
    val purchaseToken: String,
)

private fun <T> CancellableContinuation<T>.safeResume(value: T) {
    if (isActive) {
        runCatching { resume(value) }
    }
}

private val isPbl8: Boolean by lazy {
    runCatching {
        Class.forName("com.android.billingclient.api.QueryProductDetailsResult")
    }.isSuccess
}

private val queryProductDetailsStrategy: (BillingClient, QueryProductDetailsParams, CancellableContinuation<ProductDetailsResult>) -> Unit by lazy {
    if (isPbl8) ::queryProductDetailsPbl8 else ::queryProductDetailsPbl7
}

private fun queryProductDetailsPbl7(
    billingClient: BillingClient,
    params: QueryProductDetailsParams,
    continuation: CancellableContinuation<ProductDetailsResult>,
) {
    billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
        continuation.safeResume(ProductDetailsResult(billingResult, productDetailsList.orEmpty()))
    }
}

private fun queryProductDetailsPbl8(
    billingClient: BillingClient,
    params: QueryProductDetailsParams,
    continuation: CancellableContinuation<ProductDetailsResult>,
) {
    val handler = InvocationHandler { _, method, args ->
        if (method.name == "onProductDetailsResponse" && args != null && args.size == 2) {
            try {
                val billingResult = args[0] as BillingResult
                val queryResult = args[1]
                @Suppress("UNCHECKED_CAST")
                val productDetailsList = queryResult.javaClass
                    .getMethod("getProductDetailsList")
                    .invoke(queryResult) as? List<ProductDetails> ?: emptyList()
                continuation.safeResume(ProductDetailsResult(billingResult, productDetailsList))
            } catch (e: Throwable) {
                val message = e.localizedMessage ?: "Unknown billing error occurred"
                continuation.safeResume(
                    ProductDetailsResult(
                        BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                            .setDebugMessage(message)
                            .build(),
                        emptyList(),
                    )
                )
            }
        }
        null
    }
    val listener = Proxy.newProxyInstance(
        ProductDetailsResponseListener::class.java.classLoader,
        arrayOf(ProductDetailsResponseListener::class.java),
        handler
    ) as ProductDetailsResponseListener
    billingClient.queryProductDetailsAsync(params, listener)
}

internal suspend fun BillingClient.queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult =
    suspendCancellableCoroutine { continuation ->
        queryProductDetailsStrategy(this, params, continuation)
    }

internal suspend fun BillingClient.queryPurchasesAsync(params: QueryPurchasesParams): PurchasesResult =
    suspendCancellableCoroutine { continuation ->
        queryPurchasesAsync(params) { billingResult, purchasesList ->
            continuation.safeResume(PurchasesResult(billingResult, purchasesList))
        }
    }

internal suspend fun BillingClient.queryPurchaseHistory(params: QueryPurchaseHistoryParams): PurchaseHistoryResult =
    if (isPbl8) {
        PurchaseHistoryResult(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build(),
            emptyList(),
        )
    } else {
        suspendCancellableCoroutine { continuation ->
            queryPurchaseHistoryAsync(params) { billingResult, purchaseHistoryRecordList ->
                continuation.safeResume(PurchaseHistoryResult(billingResult, purchaseHistoryRecordList))
            }
        }
    }

internal suspend fun BillingClient.acknowledgePurchase(params: AcknowledgePurchaseParams): BillingResult =
    suspendCancellableCoroutine { continuation ->
        acknowledgePurchase(params) { billingResult ->
            continuation.safeResume(billingResult)
        }
    }

internal suspend fun BillingClient.consumePurchase(params: ConsumeParams): ConsumeResult =
    suspendCancellableCoroutine { continuation ->
        consumeAsync(params) { billingResult, purchaseToken ->
            continuation.safeResume(ConsumeResult(billingResult, purchaseToken))
        }
    }
