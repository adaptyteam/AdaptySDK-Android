@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.models.AdaptyPaywall

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallMapper(
    private val placementMapper: PlacementMapper,
    private val remoteConfigMapper: RemoteConfigMapper,
) {

    @JvmSynthetic
    fun map(paywallDto: PaywallDto, products: List<BackendProduct>) = AdaptyPaywall(
        name = paywallDto.name,
        variationId = paywallDto.variationId,
        remoteConfig = paywallDto.remoteConfig?.let(remoteConfigMapper::map),
        products = products,
        id = paywallDto.id,
        snapshotAt = paywallDto.snapshotAt,
        viewConfig = paywallDto.paywallBuilder,
        placement = placementMapper.map(paywallDto.placement),
    )
}