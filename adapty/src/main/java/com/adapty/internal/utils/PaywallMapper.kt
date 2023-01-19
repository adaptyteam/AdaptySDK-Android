package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.domain.models.Product
import com.adapty.models.AdaptyPaywall
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallMapper(private val gson: Gson) {

    private val type by lazy {
        object : TypeToken<HashMap<String, Any>>() {}.type
    }

    @JvmSynthetic
    fun map(paywallDto: PaywallDto, products: List<Product>) = AdaptyPaywall(
        id = paywallDto.developerId ?: throw AdaptyError(
            message = "id in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        name = paywallDto.name.orEmpty(),
        abTestName = paywallDto.abTestName.orEmpty(),
        revision = paywallDto.revision ?: 0,
        variationId = paywallDto.variationId ?: throw AdaptyError(
            message = "variationId in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        locale = paywallDto.remoteConfig?.lang ?: DEFAULT_PAYWALL_LOCALE,
        remoteConfigString = paywallDto.remoteConfig?.data,
        remoteConfig = (try {
            paywallDto.remoteConfig?.data?.let { gson.fromJson<Map<String, Any>>(it, type) }
        } catch (e: Exception) {
            null
        })?.immutableWithInterop(),
        products = products,
        updatedAt = paywallDto.updatedAt ?: 0L,
    )
}