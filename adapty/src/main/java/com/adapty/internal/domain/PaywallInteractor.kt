@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode.NO_PRODUCT_IDS_FOUND
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.BrowserLauncher
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.utils.WebPaywallUrlCreator
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.utils.DEFAULT_RETRY_COUNT
import com.adapty.internal.utils.INFINITE_RETRY
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.LifecycleManager
import com.adapty.internal.utils.Logger
import com.adapty.internal.utils.ProductMapper
import com.adapty.internal.utils.retryIfNecessary
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyFlowPaywall
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyWebPresentation
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.FileLocation
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import android.app.Activity
import android.net.Uri

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val lifecycleManager: LifecycleManager,
    private val storeManager: StoreManager,
    private val productMapper: ProductMapper,
    private val webPaywallUrlCreator: WebPaywallUrlCreator,
    private val browserLauncher: BrowserLauncher,
    private val allowLocalPAL: Boolean,
) {

    fun getPaywallProducts(flow: AdaptyFlow) : Flow<List<AdaptyPaywallProduct>> =
        getPaywallProducts(flow.paywalls)

    fun getPaywallProducts(paywall: AdaptyFlowPaywall) : Flow<List<AdaptyPaywallProduct>> =
        getPaywallProducts(listOf(paywall))

    private fun getPaywallProducts(paywalls: Iterable<AdaptyFlowPaywall>) : Flow<List<AdaptyPaywallProduct>> =
        flow {
            emit(paywalls.flatMap { it.products })
        }.flatMapConcat { products ->
            getBillingInfo(products, maxAttemptCount = DEFAULT_RETRY_COUNT)
                .map { billingInfo ->
                    paywalls.flatMap { paywall ->
                        productMapper.map(paywall.products, billingInfo, paywall)
                    }
                }
        }

    fun getProductsOnStart() =
        lifecycleManager.onActivateAllowed()
            .mapLatest { cloudRepository.getProducts().data }
            .retryIfNecessary(INFINITE_RETRY)
            .flatMapConcat { products ->
                if (allowLocalPAL)
                    cacheRepository.saveProductPALMappings(products)
                val productIds = products.items.keys.mapNotNull {
                    it.split(":")[0].takeIf(String::isNotEmpty)
                }.toList()
                storeManager.queryProductDetails(productIds, INFINITE_RETRY)
            }

    fun setFallback(source: FileLocation) =
        flow {
            emit(cacheRepository.saveFallback(source))
        }

    private fun getBillingInfo(
        products: List<BackendProduct>,
        maxAttemptCount: Long,
    ) : Flow<Map<String, ProductDetails>> {
        if (products.isEmpty())
            throwNoProductIdsFoundError()
        val productIds = products.map { it.vendorProductId }.distinct()
        return storeManager.queryProductDetails(productIds, maxAttemptCount)
            .map { productDetailsList ->
                if (productDetailsList.isEmpty())
                    throwNoProductIdsFoundError()
                productDetailsList.associateBy { productDetails -> productDetails.productId }
            }
    }

    private fun throwNoProductIdsFoundError(): Nothing {
        val message = "No In-App Purchase product identifiers were found."
        Logger.log(ERROR) { message }
        throw AdaptyError(
            message = message,
            adaptyErrorCode = NO_PRODUCT_IDS_FOUND
        )
    }

    fun createWebPaywallUrl(product: AdaptyPaywallProduct): Uri =
        webPaywallUrlCreator.create(product)

    fun openWebPaywall(activity: Activity, product: AdaptyPaywallProduct, presentation: AdaptyWebPresentation) {
        val url = webPaywallUrlCreator.create(product)
        cacheRepository.saveLastWebPaywallOpenedTime(System.currentTimeMillis())
        browserLauncher.openUrl(activity, url, presentation)
    }

    fun createWebPaywallUrl(paywall: AdaptyFlowPaywall): Uri =
        webPaywallUrlCreator.create(paywall)

    fun openWebPaywall(activity: Activity, paywall: AdaptyFlowPaywall, presentation: AdaptyWebPresentation) {
        val url = webPaywallUrlCreator.create(paywall)
        cacheRepository.saveLastWebPaywallOpenedTime(System.currentTimeMillis())
        browserLauncher.openUrl(activity, url, presentation)
    }
}
