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

    val localizedDescription: String?
        get() = skuDetails?.description

    var variationId: String? = null

    val price: String?
        get() {
            skuDetails?.let {
                return formatPrice(it.price, it.priceCurrencyCode)
            }
            return null
        }

    val currencyCode: String?
        get() = skuDetails?.priceCurrencyCode

    val subscriptionPeriod: ProductSubscriptionPeriodModel?
        get() {
            skuDetails?.let {
                return ProductSubscriptionPeriodModel(it.subscriptionPeriod)
            }
            return null
        }

    var skuDetails: SkuDetails? = null

    class ProductSubscriptionPeriodModel(var period: String) {
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
}