package com.adapty.internal.data.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ValidateProductInfo(
    val vendorProductId: String,
    val variationId: String?,
    val priceLocale: String,
    val originalPrice: String,
)