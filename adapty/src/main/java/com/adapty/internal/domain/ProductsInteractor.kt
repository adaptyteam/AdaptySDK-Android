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
import kotlinx.coroutines.flow.*
import java.io.IOException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductsInteractor(
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
    private val storeManager: StoreManager,
) {

    @JvmSynthetic
    fun getPaywalls(
        forceUpdate: Boolean
    ) = when {
        forceUpdate || !cacheRepository.arePaywallsReceivedFromBackend.get() -> {
            getPaywallsFromCloud()
        }
        else -> {
            cacheRepository.getPaywallsAndProducts()
                .takeIf { (paywalls, products) -> paywalls != null || products != null }
                ?.let { cachedPaywalls ->
                    flowOf(cachedPaywalls)
                }
                ?: getPaywallsFromCloud()
        }
    }

    private fun getPaywallsFromCloud() =
        authInteractor.runWhenAuthDataSynced {
            cloudRepository.getPaywalls()
        }
            .flatMapConcat { postProcessPaywalls(it, maxAttemptCount = DEFAULT_RETRY_COUNT) }
            .catch { error ->
                if (error is AdaptyError && (error.adaptyErrorCode == AdaptyErrorCode.SERVER_ERROR || error.originalError is IOException)) {
                    flow {
                        cacheRepository.getPaywallsAndProducts()
                            .takeIf { (paywalls, products) -> paywalls != null || products != null }
                            ?.let { cachedPaywalls ->
                                emit(cachedPaywalls)
                            }
                            ?: cacheRepository.getFallbackPaywalls()?.let { fallbackPaywalls ->
                                postProcessPaywalls(fallbackPaywalls, DEFAULT_RETRY_COUNT)
                                    .onEach(::emit)
                                    .catch { error -> throw error }
                                    .collect()
                            } ?: throw error
                    }
                        .onEach { paywalls -> emit(paywalls) }
                        .catch { error -> throw error }
                        .collect()
                } else {
                    throw error
                }
            }
            .flowOnIO()

    @JvmSynthetic
    fun getPaywallsOnStart() =
        authInteractor.runWhenAuthDataSynced(INFINITE_RETRY) {
            cloudRepository.getPaywalls()
        }
            .flatMapConcat(::postProcessPaywalls)
            .flowOnIO()

    @JvmSynthetic
    fun setFallbackPaywalls(paywalls: String) =
        cacheRepository.saveFallbackPaywalls(paywalls)

    @JvmSynthetic
    fun getPromo(maxAttemptCount: Long = DEFAULT_RETRY_COUNT) =
        authInteractor.runWhenAuthDataSynced(maxAttemptCount) { cloudRepository.getPromo() }
            .flatMapConcat { promoDto -> postProcessPromo(promoDto, maxAttemptCount) }

    @JvmSynthetic
    fun getPromoOnStart() =
        getPromo(INFINITE_RETRY)

    private fun postProcessPaywalls(
        pair: Pair<ArrayList<PaywallsResponse.Data>, ArrayList<ProductDto>>,
        maxAttemptCount: Long = INFINITE_RETRY
    ): Flow<Pair<List<PaywallModel>?, List<ProductModel>?>> {
        cacheRepository.canFallbackPaywallsBeSet.set(false)
        val (containers, products) = pair

        val data: ArrayList<Any> =
            containers.filterTo(arrayListOf()) { !it.attributes?.products.isNullOrEmpty() }

        if (data.isEmpty() && products.isEmpty()) {
            return flow {
                cacheRepository.saveContainersAndProducts(containers, products)
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

                    Pair(PaywallMapper.map(containersList), productsList.map(ProductMapper::map))
                }
        }
    }

    private fun postProcessPromo(it: PromoDto?, maxAttemptCount: Long = INFINITE_RETRY): Flow<PromoModel?> {
        return it?.let { promo ->
            cacheRepository.getPaywalls()
                ?.firstOrNull { it.variationId == promo.variationId }
                ?.let { paywall ->
                    flow {
                        emit(cacheRepository.setCurrentPromo(PromoMapper.map(promo, paywall)))
                    }
                }
                ?: (cloudRepository::getPaywalls)
                    .asFlow()
                    .retryIfNecessary(maxAttemptCount)
                    .flatMapConcat { postProcessPaywalls(it, maxAttemptCount) }
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