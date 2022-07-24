package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.models.ProductSubscriptionPeriodModel
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ProductDiscount(
    val price: BigDecimal,
    val numberOfPeriods: Int,
    val localizedPrice: String,
    val subscriptionPeriod: ProductSubscriptionPeriodModel,
)