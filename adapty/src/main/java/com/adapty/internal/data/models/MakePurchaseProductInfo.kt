package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class MakePurchaseProductInfo(
    val vendorProductId: String,
    val type: String,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val variationId: String,
)