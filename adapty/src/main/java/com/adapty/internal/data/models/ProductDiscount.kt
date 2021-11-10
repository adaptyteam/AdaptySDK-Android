package com.adapty.internal.data.models

import com.adapty.models.ProductSubscriptionPeriodModel
import java.math.BigDecimal

data class ProductDiscount(
    val price: BigDecimal,
    val numberOfPeriods: Int,
    val localizedPrice: String,
    val subscriptionPeriod: ProductSubscriptionPeriodModel,
)