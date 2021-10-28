package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.responses.PaywallsResponse
import com.adapty.internal.di.Dependencies.inject
import com.adapty.models.PaywallModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object PaywallMapper {

    private val gson: Gson by inject()
    private val type by lazy {
        object : TypeToken<HashMap<String, Any>>() {}.type
    }

    @JvmSynthetic
    fun map(paywallDto: PaywallDto) = PaywallModel(
        developerId = paywallDto.developerId ?: throw AdaptyError(
            message = "developerId in PaywallModel should not be null",
            adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
        ),
        name = paywallDto.name,
        abTestName = paywallDto.abTestName,
        revision = paywallDto.revision ?: 0,
        isPromo = paywallDto.isPromo ?: false,
        variationId = paywallDto.variationId ?: throw AdaptyError(
            message = "variationId in PaywallModel should not be null",
            adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
        ),
        products = paywallDto.products
            ?.map { productDto -> ProductMapper.map(productDto, paywallDto) }.orEmpty(),
        customPayloadString = paywallDto.customPayload,
        customPayload = try {
            paywallDto.customPayload?.let { gson.fromJson<Map<String, Any>>(it, type) }
        } catch (e: Exception) {
            null
        },
        visualPaywall = paywallDto.visualPaywall,
    )

    @JvmSynthetic
    fun map(containers: ArrayList<PaywallsResponse.Data>) =
        containers.mapNotNull { it.attributes }.map { paywallDto -> map(paywallDto) }
}