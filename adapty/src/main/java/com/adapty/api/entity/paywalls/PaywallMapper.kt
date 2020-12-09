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

    fun map(paywallModel: PaywallModel) = PaywallDto(
        developerId = paywallModel.developerId,
        revision = paywallModel.revision,
        isPromo = paywallModel.isPromo,
        variationId = paywallModel.variationId,
        products = paywallModel.products,
        customPayload = paywallModel.customPayloadString
    )

    fun map(paywallDto: PaywallDto) = PaywallModel(
        developerId = paywallDto.developerId,
        revision = paywallDto.revision,
        isPromo = paywallDto.isPromo,
        variationId = paywallDto.variationId,
        products = paywallDto.products,
        customPayloadString = paywallDto.customPayload
    ).apply {
        customPayload = try {
            customPayloadString?.let { gson.fromJson(it, type) }
        } catch (e: Exception) {
            null
        }
    }
}