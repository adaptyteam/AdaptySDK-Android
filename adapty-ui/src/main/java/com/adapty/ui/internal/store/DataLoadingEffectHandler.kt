@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.store

import com.adapty.Adapty
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.Image
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset.RemoteImage
import com.adapty.ui.internal.cache.MediaFetchService
import com.adapty.ui.internal.utils.LOADING_PRODUCTS_RETRY_DELAY
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

internal class DataLoadingEffectHandler(
    private val scope: CoroutineScope,
    private val flowKey: String,
    private val mediaFetchService: MediaFetchService,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.LoadProducts -> {
                scope.launch {
                    log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts begin" }
                    val flow = effect.flow
                    val failureCallback = effect.failureCallback

                    suspend fun load(): AdaptyResult<List<AdaptyPaywallProduct>> =
                        suspendCancellableCoroutine { continuation ->
                            Adapty.getPaywallProducts(flow) { result ->
                                when (result) {
                                    is AdaptyResult.Success -> {
                                        continuation.resumeWith(Result.success(result))
                                        log(VERBOSE) { "$LOG_PREFIX $flowKey loadProducts success" }
                                    }
                                    is AdaptyResult.Error -> {
                                        continuation.resumeWith(Result.success(result))
                                        log(ERROR) { "$LOG_PREFIX_ERROR $flowKey loadProducts error: ${result.error.message}" }
                                    }
                                }
                            }
                        }

                    suspend fun loadWithRetry(): AdaptyResult<List<AdaptyPaywallProduct>> {
                        val productResult = load()
                        return when (productResult) {
                            is AdaptyResult.Success -> productResult
                            is AdaptyResult.Error -> {
                                val shouldRetry = failureCallback.onLoadingProductsFailure(productResult.error)
                                if (shouldRetry) {
                                    delay(LOADING_PRODUCTS_RETRY_DELAY)
                                    loadWithRetry()
                                } else
                                    productResult
                            }
                        }
                    }

                    val result = loadWithRetry()
                    when (result) {
                        is AdaptyResult.Success -> {
                            val mapped = associateProductsToIds(result.value, flow)
                            dispatch(Message.ProductsLoaded(mapped))
                        }
                        is AdaptyResult.Error -> {
                            dispatch(Message.ProductsLoadFailed(result.error))
                        }
                    }
                }
            }
            is Effect.LoadRemoteImage -> {
                scope.launch {
                    try {
                        val image = loadImage(effect.remoteImage)
                        dispatch(Message.AssetLoaded(effect.assetId, image))
                    } catch (_: Exception) {
                    }
                }
            }
            else -> return
        }
    }

    private suspend fun loadImage(remoteImage: RemoteImage): Image =
        suspendCancellableCoroutine { continuation ->
            mediaFetchService.loadImage(
                remoteImage,
                handlePreview = null,
                handleResult = { image ->
                    continuation.resumeWith(Result.success(image))
                }
            )
        }
}
