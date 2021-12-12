package com.adapty.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.R
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.models.PeriodUnit
import com.adapty.models.ProductDiscountModel
import com.adapty.models.ProductModel
import com.adapty.models.ProductSubscriptionPeriodModel
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import java.math.BigDecimal
import java.text.Format

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductMapper(
    private val context: Context,
    private val priceFormatter: Format,
) {

    @JvmSynthetic
    fun map(productDto: ProductDto, paywallDto: PaywallDto) =
        map(productDto, paywallDto.name, paywallDto.abTestName, paywallDto.variationId)

    @JvmSynthetic
    fun map(productDto: ProductDto) =
        map(
            productDto,
            productDto.paywallName,
            productDto.paywallABTestName,
            productDto.variationId
        )

    private fun map(
        productDto: ProductDto,
        paywallName: String?,
        paywallAbTestName: String?,
        variationId: String?
    ) =
        ProductModel(
            vendorProductId = productDto.vendorProductId ?: throw AdaptyError(
                message = "vendorProductId in ProductModel should not be null",
                adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
            ),
            localizedTitle = productDto.localizedTitle.orEmpty(),
            localizedDescription = productDto.localizedDescription.orEmpty(),
            paywallName = paywallName,
            paywallABTestName = paywallAbTestName,
            variationId = variationId,
            price = productDto.price ?: BigDecimal.ZERO,
            localizedPrice = productDto.localizedPrice,
            currencyCode = productDto.currencyCode,
            currencySymbol = productDto.currencySymbol,
            subscriptionPeriod = productDto.subscriptionPeriod,
            localizedSubscriptionPeriod = productDto.subscriptionPeriod?.let(::getLocalizedSubscriptionPeriod),
            introductoryOfferEligibility = productDto.introductoryOfferEligibility ?: false,
            introductoryDiscount = productDto.introductoryDiscount?.let(::mapProductDiscountModel),
            freeTrialPeriod = productDto.freeTrialPeriod,
            localizedFreeTrialPeriod = productDto.freeTrialPeriod?.let(::getLocalizedSubscriptionPeriod),
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
    fun mapToRestore(
        historyRecord: PurchaseHistoryRecordModel,
        skuDetails: SkuDetails?,
    ) =
        RestoreProductInfo(
            isSubscription = historyRecord.type == BillingClient.SkuType.SUBS,
            productId = historyRecord.purchase.skus.firstOrNull(),
            purchaseToken = historyRecord.purchase.purchaseToken,
            transactionId = historyRecord.transactionId,
            localizedTitle = skuDetails?.title,
            localizedDescription = skuDetails?.description,
            price = skuDetails?.priceAmountMicros?.let(::formatPrice),
            currencyCode = skuDetails?.priceCurrencyCode,
            subscriptionPeriod = skuDetails?.subscriptionPeriod
                ?.takeIf(String::isNotEmpty)?.let(::mapSubscriptionPeriodModel),
        )

    @JvmSynthetic
    fun mapToSyncedPurchase(historyRecord: PurchaseHistoryRecordModel) =
        SyncedPurchase(
            purchaseToken = historyRecord.purchase.purchaseToken,
            purchaseTime = historyRecord.purchase.purchaseTime,
        )

    @JvmSynthetic
    fun mapSubscriptionPeriodModel(period: String?) = ProductSubscriptionPeriodModel(
        unit = period?.let(::getPeriodUnit)?.let(PeriodUnit::valueOf),
        numberOfUnits = period?.let(::getPeriodNumberOfUnits)
    )

    private fun mapProductDiscountModel(productDiscount: ProductDiscount) = ProductDiscountModel(
        price = productDiscount.price,
        numberOfPeriods = productDiscount.numberOfPeriods,
        localizedPrice = productDiscount.localizedPrice,
        subscriptionPeriod = productDiscount.subscriptionPeriod,
        localizedSubscriptionPeriod = getLocalizedSubscriptionPeriod(productDiscount.subscriptionPeriod)
    )

    private fun formatPrice(priceAmountMicros: Long): String {
        return priceFormatter.format(
            BigDecimal.valueOf(priceAmountMicros)
                .divide(BigDecimal.valueOf(1_000_000L))
        )
    }

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

    private fun getLocalizedSubscriptionPeriod(period: ProductSubscriptionPeriodModel): String {
        val period = when {
            period.unit == PeriodUnit.D && period.numberOfUnits == 7 -> {
                ProductSubscriptionPeriodModel(PeriodUnit.W, 1)
            }
            else -> period
        }

        val pluralsRes = when (period.unit) {
            PeriodUnit.D -> R.plurals.adapty_day
            PeriodUnit.W -> R.plurals.adapty_week
            PeriodUnit.M -> R.plurals.adapty_month
            PeriodUnit.Y -> R.plurals.adapty_year
            else -> return ""
        }
        return period.numberOfUnits?.let { numberOfUnits ->
            context.resources.getQuantityString(pluralsRes, numberOfUnits, numberOfUnits)
        }.orEmpty()
    }
}