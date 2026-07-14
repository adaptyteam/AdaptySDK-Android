package com.adapty.internal.crossplatform

import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.*

internal class AdaptyPaymentModeSerializer : JsonSerializer<PaymentMode> {

    override fun serialize(
        src: PaymentMode,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return context.serialize(
            when (src) {
                PaymentMode.PAY_UPFRONT -> "pay_up_front"
                else -> src.name.lowercase(Locale.ENGLISH)
            }
        )
    }
}