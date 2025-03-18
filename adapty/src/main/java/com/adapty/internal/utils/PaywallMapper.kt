@file:OptIn(InternalAdaptyApi::class)

package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.models.AdaptyPaywall
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaywallMapper(private val gson: Gson) {

    private val type by lazy {
        object : TypeToken<HashMap<String, Any>>() {}.type
    }

    @JvmSynthetic
    fun map(paywallDto: PaywallDto, products: List<BackendProduct>) = AdaptyPaywall(
        placementId = paywallDto.developerId ?: throw AdaptyError(
            message = "placementId in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        name = paywallDto.name.orEmpty(),
        abTestName = paywallDto.abTestName.orEmpty(),
        audienceName = paywallDto.audienceName.orEmpty(),
        revision = paywallDto.revision ?: 0,
        variationId = paywallDto.variationId ?: throw AdaptyError(
            message = "variationId in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        remoteConfig = paywallDto.remoteConfig?.takeIf { it.data != null }?.let { remoteConfig ->
            AdaptyPaywall.RemoteConfig(
                locale = remoteConfig.lang ?: throw AdaptyError(
                    message = "lang in RemoteConfig should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
                jsonString = remoteConfig.data ?: throw AdaptyError(
                    message = "data in RemoteConfig should not be null",
                    adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                ),
                dataMap = (try {
                    gson.fromJson<Map<String, Any>>(remoteConfig.data, type) ?: emptyMap()
                } catch (e: Exception) {
                    throw AdaptyError(
                        originalError = e,
                        message = "Couldn't decode jsonString in RemoteConfig: ${e.localizedMessage}",
                        adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
                    )
                }).immutableWithInterop(),
            )
        },
        products = products,
        paywallId = paywallDto.paywallId ?: throw AdaptyError(
            message = "paywallId in Paywall should not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        snapshotAt = paywallDto.snapshotAt.orDefault(),
        viewConfig = paywallDto.paywallBuilder,
    )

    fun mapToCache(paywallDto: PaywallDto, snapshotAt: Long) =
        paywallDto.copy(snapshotAt = snapshotAt)
}