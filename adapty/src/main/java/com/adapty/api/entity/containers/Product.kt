package com.adapty.api.entity.containers

import com.adapty.utils.formatPrice
import com.adapty.utils.getPeriodNumberOfUnits
import com.adapty.utils.getPeriodUnit
import com.android.billingclient.api.SkuDetails
import com.google.gson.annotations.SerializedName

class Product {
    @SerializedName("vendor_product_id")
    var vendorProductId: String? = null

    @SerializedName("title")
    var localizedTitle: String? = null

    @SerializedName("localizedDescription")
    var localizedDescription: String? = null

    var variationId: String? = null

    @SerializedName("price")
    var price: String? = null

    @SerializedName("currencyCode")
    var currencyCode: String? = null

    @SerializedName("subscriptionPeriod")
    var subscriptionPeriod: Product.ProductSubscriptionPeriodModel? = null

    var skuDetails: SkuDetails? = null

    data class ProductSubscriptionPeriodModel(var period: String) {
        val unit: PeriodUnit?
            get() {
                val pUnit = getPeriodUnit(period)
                pUnit?.let {
                    return PeriodUnit.valueOf(it)
                }
                return null
            }
        val numberOfUnits: Int?
            get() = getPeriodNumberOfUnits(period)
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
            this.localizedDescription = it.description
            this.price = formatPrice(it.priceAmountMicros)
            this.currencyCode = it.priceCurrencyCode
            this.subscriptionPeriod = Product.ProductSubscriptionPeriodModel(it.subscriptionPeriod)
        }
    }
}