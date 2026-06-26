@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.FlowDto
import com.adapty.models.AdaptyFlow
import com.adapty.models.AdaptyFlowPaywall

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FlowMapper(
    private val placementMapper: PlacementMapper,
    private val remoteConfigMapper: RemoteConfigMapper,
    private val productMapper: ProductMapper,
) {

    fun map(flowDto: FlowDto): AdaptyFlow {
        val placement = placementMapper.map(flowDto.placement)
        return AdaptyFlow(
            id = flowDto.id,
            variationId = flowDto.variationId,
            name = flowDto.name,
            remoteConfigs = flowDto.remoteConfigs.orEmpty().map(remoteConfigMapper::map).immutableWithInterop(),
            placement = placement,
            viewConfigurationId = flowDto.viewConfigurationId,
            paywalls = flowDto.paywalls.orEmpty().map { paywall ->
                AdaptyFlowPaywall(
                    id = paywall.id,
                    variationId = paywall.variationId,
                    name = paywall.name,
                    placement = placement,
                    products = productMapper.map(paywall.products),
                    webPurchaseUrl = paywall.webPurchaseUrl,
                )
            }.immutableWithInterop(),
            viewConfig = null,
            requestedLocale = DEFAULT_PLACEMENT_LOCALE,
            snapshotAt = flowDto.snapshotAt,
        )
    }
}
