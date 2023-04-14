package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo
import com.adapty.models.AdaptyEligibility

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Product(
    val vendorProductId: String,
    val introductoryOfferEligibility: AdaptyEligibility,
    val isConsumable: Boolean,
    val timestamp: Long,
)