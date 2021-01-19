package com.adapty.api.entity.paywalls

import com.adapty.utils.getCurrencySymbol
import com.adapty.utils.getPeriodNumberOfUnits
import com.adapty.utils.getPeriodUnit
import com.android.billingclient.api.SkuDetails
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class ProductModel {
    @SerializedName("vendor_product_id")
    var vendorProductId: String? = null

    @SerializedName("title")
    var localizedTitle: String? = null

    @SerializedName("localizedDescription")
    var localizedDescription: String? = null

    var variationId: String? = null

    var price: BigDecimal? = null

    var localizedPrice: String? = null

    var currencyCode: String? = null

    var currencySymbol: String? = null

    var subscriptionPeriod: ProductSubscriptionPeriodModel? = null

    @SerializedName("introductory_offer_eligibility")
    var introductoryOfferEligibility = true

    @SerializedName("promotional_offer_eligibility")
    var promotionalOfferEligibility = true

    var introductoryDiscount: ProductDiscountModel? = null

    var skuDetails: SkuDetails? = null

    class ProductSubscriptionPeriodModel private constructor(){

        private var _period: String? = null
        private var _unit: PeriodUnit? = null
        private var _numberOfUnits: Int? = null

        constructor(period: String) : this() {
            _period = period
        }

        constructor(unit: PeriodUnit, numberOfUnits: Int) : this() {
            _unit = unit
            _numberOfUnits = numberOfUnits
        }

        val unit: PeriodUnit?
            get() {
                return _unit ?: _period?.let(::getPeriodUnit)?.let(PeriodUnit::valueOf)
            }
        val numberOfUnits: Int?
            get() = _numberOfUnits ?: _period?.let(::getPeriodNumberOfUnits)
    }

    enum class PeriodUnit(val period: String) {
        D("day"),
        W("week"),
        M("month"),
        Y("year")
    }

    fun setDetails(sd: SkuDetails?) {
        sd?.let {
            this.skuDetails = it
            this.localizedTitle = it.title
            this.localizedDescription = it.description
            this.price = BigDecimal.valueOf(it.priceAmountMicros)
                .divide(BigDecimal.valueOf(1_000_000L))
            this.localizedPrice = it.price
            this.currencyCode = it.priceCurrencyCode
            this.currencySymbol = getCurrencySymbol(it.priceCurrencyCode)
            this.subscriptionPeriod = ProductSubscriptionPeriodModel(it.subscriptionPeriod)
            this.introductoryDiscount = ProductDiscountModel(
                price = BigDecimal.valueOf(it.introductoryPriceAmountMicros)
                    .divide(BigDecimal.valueOf(1_000_000L)),
                numberOfPeriods = it.introductoryPriceCycles,
                localizedPrice = it.introductoryPrice,
                subscriptionPeriod = ProductSubscriptionPeriodModel(it.introductoryPricePeriod)
            )
        }
    }

    class ProductDiscountModel(
        var price: BigDecimal,
        var numberOfPeriods: Int,
        var localizedPrice: String,
        var subscriptionPeriod : ProductSubscriptionPeriodModel
    )
}