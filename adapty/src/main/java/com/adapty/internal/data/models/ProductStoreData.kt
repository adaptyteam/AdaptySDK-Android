package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyProductSubscriptionPeriod
import com.android.billingclient.api.SkuDetails
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductStoreData(
    val localizedTitle: String,
    val localizedDescription: String,
    val price: BigDecimal,
    val localizedPrice: String,
    val currencyCode: String,
    val currencySymbol: String,
    val subscriptionPeriod: AdaptyProductSubscriptionPeriod?,
    val introductoryDiscount: ProductDiscountData?,
    val freeTrialPeriod: AdaptyProductSubscriptionPeriod?,
    val skuDetails: SkuDetails,
)