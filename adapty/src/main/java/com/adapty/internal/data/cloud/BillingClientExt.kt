package com.adapty.internal.data.cloud

import com.android.billingclient.api.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal data class ProductDetailsResult(
    val billingResult: BillingResult,
    val productDetailsList: List<ProductDetails>,
    val unfetchedProductList: List<UnfetchedProduct>,
)

internal fun List<UnfetchedProduct>.toLogString() =
    joinToString(prefix = "[", postfix = "]") { product ->
        val status = when (product.statusCode) {
            UnfetchedProduct.StatusCode.INVALID_PRODUCT_ID_FORMAT -> "INVALID_PRODUCT_ID_FORMAT"
            UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND -> "PRODUCT_NOT_FOUND"
            UnfetchedProduct.StatusCode.NO_ELIGIBLE_OFFER -> "NO_ELIGIBLE_OFFER"
            else -> "UNKNOWN (${product.statusCode})"
        }
        "${product.productId}: $status"
    }

internal data class PurchasesResult(
    val billingResult: BillingResult,
    val purchasesList: List<Purchase>,
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

internal suspend fun BillingClient.queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult =
    suspendCancellableCoroutine { continuation ->
        queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            continuation.safeResume(
                ProductDetailsResult(
                    billingResult,
                    queryProductDetailsResult.productDetailsList,
                    queryProductDetailsResult.unfetchedProductList,
                )
            )
        }
    }

internal suspend fun BillingClient.queryPurchasesAsync(params: QueryPurchasesParams): PurchasesResult =
    suspendCancellableCoroutine { continuation ->
        queryPurchasesAsync(params) { billingResult, purchasesList ->
            continuation.safeResume(PurchasesResult(billingResult, purchasesList))
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
