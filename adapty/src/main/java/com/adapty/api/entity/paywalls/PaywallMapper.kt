package com.adapty.api.entity.paywalls

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Exception

internal object PaywallMapper {

    private val gson by lazy {
        Gson()
    }
    private val type by lazy {
        object: TypeToken<HashMap<String, Any>>() { }.type
    }

    fun map(paywallDto: PaywallDto) = PaywallModel(
        developerId = paywallDto.developerId,
        name = paywallDto.name,
        abTestName = paywallDto.abTestName,
        revision = paywallDto.revision,
        isPromo = paywallDto.isPromo,
        variationId = paywallDto.variationId,
        products = paywallDto.products
            ?.onEach { product ->
                product.variationId = paywallDto.variationId
                product.paywallName = paywallDto.name
                product.paywallABTestName = paywallDto.abTestName
            },
        customPayloadString = paywallDto.customPayload
    ).apply {
        customPayload = try {
            customPayloadString?.let { gson.fromJson(it, type) }
        } catch (e: Exception) {
            null
        }
    }
}