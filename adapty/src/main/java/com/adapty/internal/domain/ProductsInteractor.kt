package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.data.cloud.StoreManager
import com.adapty.internal.data.models.ProductDto
import com.adapty.internal.data.models.PromoDto
import com.adapty.internal.data.models.responses.PaywallsResponse
import com.adapty.internal.utils.*
import com.adapty.models.PaywallModel
import com.adapty.models.ProductModel
import com.adapty.models.PromoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductsInteractor(
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
) {

    @JvmSynthetic
    fun getPaywalls(
        forceUpdate: Boolean
    ) = cloudRepository.arePaywallRequestsAllowed
        .filter { it }
        .flatMapLatest {
            when {
                forceUpdate -> {
                    cloudRepository.getPaywalls()
                        .flatMapConcat { postProcessPaywalls(it, maxAttemptCount = 3L) }
                        .catch { error ->
                            if (error is AdaptyError && error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR) {
                                emit(cacheRepository.getPaywallsAndProducts())
                            } else {
                                throw error
                            }
                        }
                        .flowOnIO()
                }
                else -> {
                    cacheRepository.getPaywallsAndProducts()
                        .takeIf { (paywalls, _) -> paywalls != null }?.let { cachedPaywalls ->
                            flowOf(cachedPaywalls)
                        }
                        ?: cacheRepository.getFallbackPaywalls()?.let { fallbackPaywalls ->
                            flowOf(fallbackPaywalls)
                                .flatMapConcat(::postProcessPaywalls)
                        } ?: throw AdaptyError(
                            message = "Paywalls not found",
                            adaptyErrorCode = AdaptyErrorCode.PAYWALL_NOT_FOUND
                        )
                }
            }
        }

    @JvmSynthetic
    fun getPaywallsOnStart() =
        (cloudRepository::getPaywallsForced)
            .asFlow()
            .retryWhen { error, attempt ->
                if (error !is AdaptyError) {
                    return@retryWhen false
                }

                if (attempt > 2 && error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR && !cloudRepository.arePaywallRequestsAllowed.value) {
                    cacheRepository.getContainers()?.let {
                        cloudRepository.arePaywallRequestsAllowed.compareAndSet(expect = false, update = true)
                    } ?: cacheRepository.getFallbackPaywalls()?.let {
                        cloudRepository.arePaywallRequestsAllowed.compareAndSet(expect = false, update = true)
                    }
                }

                when (error.getRetryType(true)) {
                    AdaptyError.RetryType.PROGRESSIVE -> {
                        delay(getServerErrorDelay(attempt))
                        return@retryWhen true
                    }
                    AdaptyError.RetryType.SIMPLE -> {
                        delay(NETWORK_ERROR_DELAY_MILLIS)
                        return@retryWhen true
                    }
                    else -> {
                        return@retryWhen false
                    }
                }
            }
            .flatMapConcat(::postProcessPaywalls)
            .flowOnIO()

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String) =
        cacheRepository.saveFallbackPaywalls(paywalls)

    @JvmSynthetic
    fun getPromo() = cloudRepository.getPromo()
        .flatMapConcat { promoDto -> postProcessPromo(promoDto, maxAttemptCount = 3) }

    @JvmSynthetic
    fun getPromoOnStart() =
        (cloudRepository::getPromoForced)
            .asFlow()
            .retryIfNecessary()
            .flatMapConcat(::postProcessPromo)
            .flowOnIO()

    private fun postProcessPaywalls(
        pair: Pair<ArrayList<PaywallsResponse.Data>, ArrayList<ProductDto>>,
        maxAttemptCount: Long = -1
    ): Flow<Pair<List<PaywallModel>?, List<ProductModel>?>> {
        cacheRepository.arePaywallsReceivedFromBackend.compareAndSet(false, true)
        val (containers, products) = pair

        val data: ArrayList<Any> =
            containers.filterTo(arrayListOf()) { !it.attributes?.products.isNullOrEmpty() }

        if (data.isEmpty() && products.isEmpty()) {
            return flow {
                cacheRepository.saveContainersAndProducts(containers, products)
                cloudRepository.arePaywallRequestsAllowed.compareAndSet(expect = false, update = true)
                emit(Pair(PaywallMapper.map(containers), products.map(ProductMapper::map)))
            }
        } else {
            if (products.isNotEmpty())
                data.add(products)
            return storeManager
                .fillBillingInfo(data, maxAttemptCount)
                .map { data ->
                    val containersList = arrayListOf<PaywallsResponse.Data>()
                    val productsList = arrayListOf<ProductDto>()

                    for (item in data) {
                        if (item is PaywallsResponse.Data)
                            containersList.add(item)
                        else if (item is ArrayList<*>)
                            productsList.addAll(item.filterIsInstance(ProductDto::class.java))
                    }

                    val unfilledContainers =
                        containers.filter { container -> containersList.all { it.id != container.id } }
                    containersList.addAll(unfilledContainers)

                    cacheRepository.saveContainersAndProducts(containersList, productsList)
                    cloudRepository.arePaywallRequestsAllowed.compareAndSet(expect = false, update = true)

                    Pair(PaywallMapper.map(containersList), productsList.map(ProductMapper::map))
                }
        }
    }

    private fun postProcessPromo(it: PromoDto?, maxAttemptCount: Long = -1): Flow<PromoModel?> {
        return it?.let { promo ->
            cacheRepository.getPaywalls()
                ?.firstOrNull { it.variationId == promo.variationId }
                ?.let { paywall ->
                    flow {
                        emit(cacheRepository.setCurrentPromo(PromoMapper.map(promo, paywall)))
                    }
                }
                ?: (cloudRepository::getPaywallsForced)
                    .asFlow()
                    .retryIfNecessary(maxAttemptCount)
                    .flatMapConcat(::postProcessPaywalls)
                    .map { (paywalls, _) ->
                        paywalls
                            ?.firstOrNull { it.variationId == promo.variationId }
                            ?.let { paywall ->
                                cacheRepository.setCurrentPromo(PromoMapper.map(promo, paywall))
                            }
                            ?: throw AdaptyError(
                                message = "Paywall not found",
                                adaptyErrorCode = AdaptyErrorCode.PAYWALL_NOT_FOUND
                            )
                    }
                    .flowOnIO()
        } ?: flowOf(null)
    }
}