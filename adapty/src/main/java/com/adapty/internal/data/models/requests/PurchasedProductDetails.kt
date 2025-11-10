package com.adapty.internal.data.models.requests

import androidx.annotation.RestrictTo
import com.android.billingclient.api.ProductDetails
import com.google.gson.annotations.SerializedName

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PurchasedProductDetails(
    @SerializedName("product_id")
    val productId: String,
    @SerializedName("one_time_purchase_offer_details")
    val oneTimePurchaseOfferDetails: OneTime?,
    @SerializedName("subscription_offer_details")
    val subscriptionOfferDetails: List<Sub>?,
) {
    internal class OneTime(
        @SerializedName("price_amount_micros")
        val priceAmountMicros: Long,
        @SerializedName("price_currency_code")
        val currencyCode: String,
    )

    internal class Sub(
        @SerializedName("base_plan_id")
        val basePlanId: String,
        @SerializedName("offer_id")
        val offerId: String?,
        @SerializedName("pricing_phases")
        val pricingPhases: List<PricingPhase>,
    ) {
        internal class PricingPhase(
            @SerializedName("price_amount_micros")
            val priceAmountMicros: Long,
            @SerializedName("price_currency_code")
            val currencyCode: String,
            @SerializedName("billing_period")
            val billingPeriod: String,
            @SerializedName("recurrence_mode")
            val recurrenceMode: Int,
            @SerializedName("billing_cycle_count")
            val billingCycleCount: Int,
        )
    }

    companion object {
        fun create(productDetails: ProductDetails) =
            PurchasedProductDetails(
                productId = productDetails.productId,
                oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetails?.let { oneTime ->
                    OneTime(
                        oneTime.priceAmountMicros,
                        oneTime.priceCurrencyCode,
                    )
                },
                subscriptionOfferDetails = productDetails.subscriptionOfferDetails?.map { sub ->
                    Sub(
                        sub.basePlanId,
                        sub.offerId,
                        sub.pricingPhases.pricingPhaseList.map { pricingPhase ->
                            Sub.PricingPhase(
                                pricingPhase.priceAmountMicros,
                                pricingPhase.priceCurrencyCode,
                                pricingPhase.billingPeriod,
                                pricingPhase.recurrenceMode,
                                pricingPhase.billingCycleCount,
                            )
                        }
                    )
                }
            )
    }
}