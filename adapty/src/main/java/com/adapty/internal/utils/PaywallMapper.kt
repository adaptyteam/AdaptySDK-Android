@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.models.AdaptyPaywall

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallMapper {

    @JvmSynthetic
    fun map(paywallDto: PaywallDto, products: List<BackendProduct>) = AdaptyPaywall(
        placementId = paywallDto.developerId,
        name = paywallDto.name,
        abTestName = paywallDto.abTestName,
        audienceName = paywallDto.audienceName.orEmpty(),
        revision = paywallDto.revision,
        variationId = paywallDto.variationId,
        remoteConfig = paywallDto.remoteConfig?.let { remoteConfig ->
            AdaptyPaywall.RemoteConfig(
                locale = remoteConfig.lang,
                jsonString = remoteConfig.data,
                dataMap = remoteConfig.dataMap.immutableWithInterop(),
            )
        },
        products = products,
        paywallId = paywallDto.paywallId,
        snapshotAt = paywallDto.snapshotAt,
        viewConfig = paywallDto.paywallBuilder,
    )
}