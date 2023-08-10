package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.domain.models.PurchaseableProduct
import com.android.billingclient.api.Purchase
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ValidateReceiptRequest(
    @SerializedName("data")
    private val data: Data
) {
    internal class Data(
        @SerializedName("id")
        val id: String,
        @SerializedName("type")
        val type: String = "google_receipt_validation_result",
        @SerializedName("attributes")
        val attributes: Attributes
    ) {

        internal class Attributes(
            @SerializedName("profile_id")
            private val profileId: String,
            @SerializedName("product_id")
            private val productId: String,
            @SerializedName("purchase_token")
            private val purchaseToken: String,
            @SerializedName("is_subscription")
            private val isSubscription: Boolean,
            @SerializedName("variation_id")
            private val variationId: String,
            @SerializedName("product_details")
            private val productDetails: PurchasedProductDetails,
        )
    }

    internal companion object {
        fun create(
            id: String,
            purchase: Purchase,
            product: PurchaseableProduct,
        ): ValidateReceiptRequest {
            val offerDetails = product.offerToken?.let { offerToken ->
                product.productDetails.subscriptionOfferDetails?.firstOrNull { it.offerToken == offerToken }
            }
            return ValidateReceiptRequest(
                Data(
                    id = id,
                    attributes = Data.Attributes(
                        profileId = id,
                        productId = purchase.products.firstOrNull().orEmpty(),
                        purchaseToken = purchase.purchaseToken,
                        isSubscription = product.isSubscription,
                        variationId = product.variationId,
                        productDetails = when {
                            offerDetails == null -> PurchasedProductDetails(
                                productId = purchase.products.firstOrNull().orEmpty(),
                                oneTimePurchaseOfferDetails = PurchasedProductDetails.OneTime(
                                    product.priceAmountMicros,
                                    product.currencyCode,
                                ),
                                subscriptionOfferDetails = null,
                            )
                            else -> PurchasedProductDetails(
                                productId = purchase.products.firstOrNull().orEmpty(),
                                oneTimePurchaseOfferDetails = null,
                                subscriptionOfferDetails = listOf(
                                    PurchasedProductDetails.Sub(
                                        offerDetails.basePlanId,
                                        offerDetails.offerId,
                                        offerDetails.pricingPhases.pricingPhaseList.map { pricingPhase ->
                                            PurchasedProductDetails.Sub.PricingPhase(
                                                pricingPhase.priceAmountMicros,
                                                pricingPhase.priceCurrencyCode,
                                                pricingPhase.billingPeriod,
                                                pricingPhase.recurrenceMode,
                                                pricingPhase.billingCycleCount,
                                            )
                                        }
                                    )
                                ),
                            )
                        }
                    )
                )
            )
        }
    }
}