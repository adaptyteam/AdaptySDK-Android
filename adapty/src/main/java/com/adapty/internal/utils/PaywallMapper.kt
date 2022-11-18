package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PaywallDto
import com.adapty.models.AdaptyPaywall
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallMapper(private val gson: Gson) {

    private val type by lazy {
        object : TypeToken<HashMap<String, Any>>() {}.type
    }

    @JvmSynthetic
    fun map(paywallDto: PaywallDto) = AdaptyPaywall(
        id = paywallDto.developerId ?: throw AdaptyError(
            message = "id in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
        ),
        name = paywallDto.name.orEmpty(),
        abTestName = paywallDto.abTestName.orEmpty(),
        revision = paywallDto.revision ?: 0,
        variationId = paywallDto.variationId ?: throw AdaptyError(
            message = "variationId in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
        ),
        vendorProductIds = paywallDto.products.mapNotNull { it.vendorProductId }
            .immutableWithInterop(),
        remoteConfigString = paywallDto.customPayload,
        remoteConfig = (try {
            paywallDto.customPayload?.let { gson.fromJson<Map<String, Any>>(it, type) }
        } catch (e: Exception) {
            null
        })?.immutableWithInterop(),
    )
}