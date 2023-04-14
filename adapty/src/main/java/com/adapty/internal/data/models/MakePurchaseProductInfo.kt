package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyPaywallProduct.Type

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MakePurchaseProductInfo(
    val vendorProductId: String,
    val type: Type,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val variationId: String,
)