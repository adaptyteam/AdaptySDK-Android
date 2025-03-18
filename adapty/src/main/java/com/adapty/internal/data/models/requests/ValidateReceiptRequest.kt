package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.adapty.internal.domain.models.PurchaseableProduct
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails
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
            profileId: String,
            purchase: Purchase,
            product: PurchaseableProduct,
        ): ValidateReceiptRequest {
            val productId = purchase.products.firstOrNull().orEmpty()
            val offerDetails = product.currentOfferDetails
            val productDetails = when {
                offerDetails == null -> PurchasedProductDetails(
                    productId = productId,
                    oneTimePurchaseOfferDetails = PurchasedProductDetails.OneTime(
                        product.priceAmountMicros,
                        product.currencyCode,
                    ),
                    subscriptionOfferDetails = null,
                )
                else -> PurchasedProductDetails(
                    productId = productId,
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
            val variationId = product.variationId
            val isSubscription = product.isSubscription
            return create(profileId, productId, variationId, isSubscription, purchase, productDetails)
        }

        fun create(
            profileId: String,
            variationId: String,
            purchase: Purchase,
            product: ProductDetails,
        ): ValidateReceiptRequest {
            val productId = product.productId
            val isSubscription = product.productType == ProductType.SUBS
            val productDetails = PurchasedProductDetails(
                productId = productId,
                oneTimePurchaseOfferDetails = product.oneTimePurchaseOfferDetails?.let { offerDetails ->
                    PurchasedProductDetails.OneTime(
                        offerDetails.priceAmountMicros,
                        offerDetails.priceCurrencyCode,
                    )
                },
                subscriptionOfferDetails = product.subscriptionOfferDetails?.map { offerDetails ->
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
                },
            )
            return create(profileId, productId, variationId, isSubscription, purchase, productDetails)
        }

        private fun create(
            profileId: String,
            productId: String,
            variationId: String,
            isSubscription: Boolean,
            purchase: Purchase,
            productDetails: PurchasedProductDetails,
        ): ValidateReceiptRequest {
            return ValidateReceiptRequest(
                Data(
                    id = profileId,
                    attributes = Data.Attributes(
                        profileId = profileId,
                        productId = productId,
                        purchaseToken = purchase.purchaseToken,
                        isSubscription = isSubscription,
                        variationId = variationId,
                        productDetails = productDetails,
                    )
                )
            )
        }
    }
}