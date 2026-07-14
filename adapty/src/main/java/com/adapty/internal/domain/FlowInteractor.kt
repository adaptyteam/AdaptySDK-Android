@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.domain

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.cache.CacheRepository
import com.adapty.internal.data.cloud.CloudRepository
import com.adapty.internal.utils.FlowMapper
import com.adapty.internal.utils.INF_PLACEMENT_TIMEOUT_MILLIS
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PLACEMENT_TIMEOUT_MILLIS_SHIFT
import com.adapty.internal.utils.timeout
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyPlacementFetchPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeoutException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlowInteractor(
    private val flowFetcher: BasePlacementFetcher,
    private val flowMapper: FlowMapper,
    private val authInteractor: AuthInteractor,
    private val cloudRepository: CloudRepository,
    private val cacheRepository: CacheRepository,
) {

    fun getFlow(placementId: String, fetchPolicy: AdaptyPlacementFetchPolicy, loadTimeout: Int): Flow<AdaptyFlow> =
        flowFetcher.fetchFlow(placementId, fetchPolicy, loadTimeout)
            .map { flow -> flowMapper.map(flow) }

    fun getFlowUntargeted(placementId: String, fetchPolicy: AdaptyPlacementFetchPolicy): Flow<AdaptyFlow> =
        flowFetcher.fetchFlowUntargeted(placementId, fetchPolicy)
            .map { flow -> flowMapper.map(flow) }

    fun getViewConfiguration(flow: AdaptyFlow, locale: String?, loadTimeout: Int): Flow<Map<String, Any>> {
        val viewConfigurationId = flow.viewConfigurationId ?: return flow {
            throw AdaptyError(
                message = "View configuration has not been found for the requested flow",
                adaptyErrorCode = AdaptyErrorCode.WRONG_PARAMETER,
            )
        }

        flow.viewConfig?.let { embeddedConfig ->
            return flowOf(wrapViewConfigIfNeeded(embeddedConfig, viewConfigurationId, locale))
        }

        val isTestUser = cacheRepository.getProfile()?.isTestUser == true

        if (!isTestUser) {
            getLocalViewConfig(flow.id, viewConfigurationId)?.let { localConfig ->
                return flowOf(wrapViewConfigIfNeeded(localConfig, viewConfigurationId, locale))
            }
        }

        val baseFlow = authInteractor.runWhenAuthDataSynced {
            cloudRepository.getFlowViewConfigurationFallback(flow.id, viewConfigurationId).data
        }

        return (if (loadTimeout == INF_PLACEMENT_TIMEOUT_MILLIS) {
            baseFlow
        } else {
            timeout(baseFlow, loadTimeout - PLACEMENT_TIMEOUT_MILLIS_SHIFT)
        })
            .catch { e ->
                if (e is AdaptyError && e.adaptyErrorCode == AdaptyErrorCode.DECODING_FAILED)
                    throw e
                if (e !is TimeoutException && e !is AdaptyError)
                    throw e
                emit(cloudRepository.getFlowViewConfiguration(flow.id, viewConfigurationId).data)
            }
            .catch { e ->
                if (!isTestUser || (e !is TimeoutException && e !is AdaptyError))
                    throw e
                emit(getLocalViewConfig(flow.id, viewConfigurationId) ?: throw e)
            }
            .map { rawConfig ->
                cacheRepository.saveFlowViewConfig(flow.id, viewConfigurationId, rawConfig)
                wrapViewConfigIfNeeded(rawConfig, viewConfigurationId, locale)
            }
    }

    private fun getLocalViewConfig(flowId: String, viewConfigurationId: String): Map<String, Any>? =
        cacheRepository.getFlowViewConfig(flowId, viewConfigurationId)
            ?: cacheRepository.getFlowViewConfigFallback(viewConfigurationId)

    private fun wrapViewConfigIfNeeded(
        rawConfig: Map<String, Any>,
        viewConfigurationId: String,
        locale: String?,
    ): Map<String, Any> {
        val alreadyWrapped = rawConfig.containsKey(PAYWALL_BUILDER_CONFIG_KEY) ||
            (rawConfig[DATA_KEY] as? Map<*, *>)?.containsKey(PAYWALL_BUILDER_CONFIG_KEY) == true
        if (alreadyWrapped) return rawConfig

        return buildMap {
            put(PAYWALL_BUILDER_ID_KEY, viewConfigurationId)
            put(PAYWALL_BUILDER_CONFIG_KEY, rawConfig)
            if (locale != null) put(LANG_KEY, locale)
        }
    }

    private companion object {
        const val DATA_KEY = "data"
        const val PAYWALL_BUILDER_ID_KEY = "paywall_builder_id"
        const val PAYWALL_BUILDER_CONFIG_KEY = "paywall_builder_config"
        const val LANG_KEY = "lang"
    }
}
