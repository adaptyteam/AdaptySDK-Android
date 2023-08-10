package com.adapty.internal.domain.models

import androidx.annotation.RestrictTo
import com.android.billingclient.api.ProductDetails
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchaseableProduct(
    val vendorProductId: String,
    val priceAmountMicros: Long,
    val currencyCode: String,
    val variationId: String,
    val offerToken: String?,
    val isOfferPersonalized: Boolean,
    val productDetails: ProductDetails,
) {
    val isSubscription = offerToken != null
}