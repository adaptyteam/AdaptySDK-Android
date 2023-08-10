package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class BackendProduct(
    val vendorProductId: String,
    val timestamp: Long,
    val subscriptionData: SubscriptionData?,
) {
    class SubscriptionData(
        val basePlanId: String,
        val offerId: String?,
    )
}