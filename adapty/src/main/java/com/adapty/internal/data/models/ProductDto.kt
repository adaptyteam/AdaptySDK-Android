package com.adapty.internal.data.models

import androidx.annotation.RestrictTo
import com.adapty.internal.utils.CurrencyHelper
import com.adapty.internal.utils.ProductMapper
import com.adapty.models.ProductSubscriptionPeriodModel
import com.android.billingclient.api.SkuDetails
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductDto(
    @SerializedName("vendor_product_id")
    val vendorProductId: String?,
    @SerializedName("title")
    var localizedTitle: String?,
    @SerializedName("localizedDescription")
    var localizedDescription: String?,
    var paywallName: String?,
    var paywallABTestName: String?,
    var variationId: String?,
    var price: BigDecimal?,
    var localizedPrice: String?,
    var currencyCode: String?,
    var currencySymbol: String?,
    var subscriptionPeriod: ProductSubscriptionPeriodModel?,
    @SerializedName("introductory_offer_eligibility")
    val introductoryOfferEligibility: Boolean?,
    var introductoryDiscount: ProductDiscount?,
    var freeTrialPeriod: ProductSubscriptionPeriodModel?,
    var skuDetails: SkuDetails?,
) {

    @JvmSynthetic
    fun setDetails(sd: SkuDetails?, currencyHelper: CurrencyHelper, productMapper: ProductMapper) {
        if (sd == null) return

        skuDetails = sd
        localizedTitle = sd.title
        localizedDescription = sd.description
        price = BigDecimal.valueOf(sd.priceAmountMicros)
            .divide(BigDecimal.valueOf(1_000_000L))
        localizedPrice = sd.price
        currencyCode = sd.priceCurrencyCode
        currencySymbol = currencyHelper.getCurrencySymbol(sd.priceCurrencyCode)
        subscriptionPeriod = sd.subscriptionPeriod.takeIf(String::isNotEmpty)
            ?.let(productMapper::mapSubscriptionPeriodModel)
        introductoryDiscount =
            sd.introductoryPrice.takeIf(String::isNotEmpty)?.let { introductoryPrice ->
                ProductDiscount(
                    price = BigDecimal.valueOf(sd.introductoryPriceAmountMicros)
                        .divide(BigDecimal.valueOf(1_000_000L)),
                    numberOfPeriods = sd.introductoryPriceCycles,
                    localizedPrice = introductoryPrice,
                    subscriptionPeriod = productMapper.mapSubscriptionPeriodModel(sd.introductoryPricePeriod)
                )
            }
        freeTrialPeriod =
            sd.freeTrialPeriod.takeIf(String::isNotEmpty)
                ?.let(productMapper::mapSubscriptionPeriodModel)
    }
}