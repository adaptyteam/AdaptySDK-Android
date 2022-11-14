package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class Product(
    val vendorProductId: String,
    val introductoryOfferEligibility: Boolean?,
    val timestamp: Long,
    val source: Source,
)