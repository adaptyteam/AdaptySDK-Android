package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.PaywallDto
import com.adapty.internal.data.models.ProductDto
import com.adapty.internal.data.models.ValidateProductInfo
import com.adapty.models.PeriodUnit
import com.adapty.models.ProductModel
import com.adapty.models.ProductSubscriptionPeriodModel
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object ProductMapper {

    @JvmSynthetic
    fun map(productDto: ProductDto, paywallDto: PaywallDto) =
        ProductModel(
            vendorProductId = productDto.vendorProductId ?: throw AdaptyError(
                message = "vendorProductId in ProductModel should not be null",
                adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
            ),
            localizedTitle = productDto.localizedTitle ?: "",
            localizedDescription = productDto.localizedDescription ?: "",
            paywallName = paywallDto.name,
            paywallABTestName = paywallDto.abTestName,
            variationId = paywallDto.variationId,
            price = productDto.price ?: BigDecimal.ZERO,
            localizedPrice = productDto.localizedPrice,
            currencyCode = productDto.currencyCode,
            currencySymbol = productDto.currencySymbol,
            subscriptionPeriod = productDto.subscriptionPeriod,
            introductoryOfferEligibility = productDto.introductoryOfferEligibility ?: false,
            introductoryDiscount = productDto.introductoryDiscount,
            freeTrialPeriod = productDto.freeTrialPeriod,
            skuDetails = productDto.skuDetails,
        )

    @JvmSynthetic
    fun map(productDto: ProductDto) =
        ProductModel(
            vendorProductId = productDto.vendorProductId ?: throw AdaptyError(
                message = "vendorProductId in ProductModel should not be null",
                adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
            ),
            localizedTitle = productDto.localizedTitle ?: "",
            localizedDescription = productDto.localizedDescription ?: "",
            paywallName = productDto.paywallName,
            paywallABTestName = productDto.paywallABTestName,
            variationId = productDto.variationId,
            price = productDto.price ?: BigDecimal.ZERO,
            localizedPrice = productDto.localizedPrice,
            currencyCode = productDto.currencyCode,
            currencySymbol = productDto.currencySymbol,
            subscriptionPeriod = productDto.subscriptionPeriod,
            introductoryOfferEligibility = productDto.introductoryOfferEligibility ?: false,
            introductoryDiscount = productDto.introductoryDiscount,
            freeTrialPeriod = productDto.freeTrialPeriod,
            skuDetails = productDto.skuDetails,
        )

    @JvmSynthetic
    fun mapToValidate(productModel: ProductModel) =
        ValidateProductInfo(
            variationId = productModel.variationId,
            priceLocale = productModel.currencyCode,
            originalPrice = productModel.skuDetails?.priceAmountMicros?.let(::formatPrice)
        )

    @JvmSynthetic
    fun mapToValidate(productDto: ProductDto) =
        ValidateProductInfo(
            variationId = productDto.variationId,
            priceLocale = productDto.currencyCode,
            originalPrice = productDto.skuDetails?.priceAmountMicros?.let(::formatPrice)
        )

    @JvmSynthetic
    fun mapSubscriptionPeriodModel(period: String?) = ProductSubscriptionPeriodModel(
        unit = period?.let(::getPeriodUnit)?.let(PeriodUnit::valueOf),
        numberOfUnits = period?.let(::getPeriodNumberOfUnits)
    )

    private fun getPeriodUnit(period: String) =
        period.takeIf(String::isNotEmpty)?.last()?.toString()

    private fun getPeriodNumberOfUnits(period: String) =
        when {
            period.isEmpty() -> null
            period.last() == 'D' -> {
                Regex("\\d+[a-zA-Z]").findAll(period, 0)
                    .map { matchResult ->
                        val segment = matchResult.groupValues.first()
                        val periodNumber = segment.filter(Char::isDigit).toInt()
                        val periodLetter = segment.filter(Char::isLetter)
                        discountPeriodMultipliers[periodLetter]?.let { multiplier ->
                            periodNumber * multiplier
                        } ?: 0
                    }.fold(0) { numberOfUnits, next -> numberOfUnits + next }
                    .takeIf { it > 0 }
            }
            else -> period.replace("[^0-9]".toRegex(), "").takeIf(String::isNotEmpty)?.toInt()
        }

    private val discountPeriodMultipliers = mapOf(
        "Y" to 365,
        "M" to 30,
        "W" to 7,
        "D" to 1
    )
}