package com.adapty.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.R
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.domain.models.Product
import com.adapty.internal.domain.models.Source
import com.adapty.models.*
import com.adapty.models.AdaptyEligibility.*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import java.math.BigDecimal
import java.text.Format

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductMapper(
    private val context: Context,
    private val priceFormatter: Format,
    private val currencyHelper: CurrencyHelper,
) {

    @JvmSynthetic
    fun map(
        products: List<Product>,
        storeData: Map<String, ProductStoreData>,
        paywall: AdaptyPaywall,
    ) =
        products.mapNotNull { product ->
            storeData[product.vendorProductId]?.let { productStoreData ->
                map(product, productStoreData, paywall)
            }
        }

    @JvmSynthetic
    fun map(
        product: Product,
        productStoreData: ProductStoreData,
        paywall: AdaptyPaywall,
    ) =
        AdaptyPaywallProduct(
            vendorProductId = product.vendorProductId,
            localizedTitle = productStoreData.localizedTitle,
            localizedDescription = productStoreData.localizedDescription,
            paywallName = paywall.name,
            paywallABTestName = paywall.abTestName,
            variationId = paywall.variationId,
            price = productStoreData.price,
            localizedPrice = productStoreData.localizedPrice,
            currencyCode = productStoreData.currencyCode,
            currencySymbol = productStoreData.currencySymbol,
            subscriptionPeriod = productStoreData.subscriptionPeriod,
            localizedSubscriptionPeriod = productStoreData.subscriptionPeriod?.let(::getLocalizedSubscriptionPeriod),
            introductoryOfferEligibility = product.introductoryOfferEligibility,
            introductoryDiscount = productStoreData.introductoryDiscount?.let(::mapProductDiscount),
            freeTrialPeriod = productStoreData.freeTrialPeriod,
            localizedFreeTrialPeriod = productStoreData.freeTrialPeriod?.let(::getLocalizedSubscriptionPeriod),
            skuDetails = productStoreData.skuDetails,
            timestamp = product.timestamp,
            payloadData = AdaptyPaywallProduct.Payload(
                productStoreData.skuDetails.priceAmountMicros,
                productStoreData.skuDetails.type,
            )
        )

    @JvmSynthetic
    fun map(productDtos: List<ProductDto>, source: Source) =
        productDtos.map { productDto -> map(productDto, source) }

    @JvmSynthetic
    fun map(productDto: ProductDto, source: Source) =
        Product(
            vendorProductId = productDto.vendorProductId ?: throw AdaptyError(
                message = "vendorProductId in Product should not be null",
                adaptyErrorCode = AdaptyErrorCode.MISSING_PARAMETER
            ),
            introductoryOfferEligibility = mapIntroductoryOfferEligibility(productDto, source),
            timestamp = productDto.timestamp ?: 0L,
        )

    @JvmSynthetic
    fun mapBillingInfoToProductStoreData(billingInfo: SkuDetails) =
        ProductStoreData(
            localizedTitle = billingInfo.title,
            localizedDescription = billingInfo.description,
            price = BigDecimal.valueOf(billingInfo.priceAmountMicros)
                .divide(BigDecimal.valueOf(1_000_000L)),
            localizedPrice = billingInfo.price,
            currencyCode = billingInfo.priceCurrencyCode,
            currencySymbol = currencyHelper.getCurrencySymbol(billingInfo.priceCurrencyCode),
            subscriptionPeriod = billingInfo.subscriptionPeriod.takeIf(String::isNotEmpty)
                ?.let(::mapSubscriptionPeriod),
            introductoryDiscount =
            billingInfo.introductoryPrice.takeIf(String::isNotEmpty)?.let { introductoryPrice ->
                ProductDiscountData(
                    price = BigDecimal.valueOf(billingInfo.introductoryPriceAmountMicros)
                        .divide(BigDecimal.valueOf(1_000_000L)),
                    numberOfPeriods = billingInfo.introductoryPriceCycles,
                    localizedPrice = introductoryPrice,
                    subscriptionPeriod = mapSubscriptionPeriod(billingInfo.introductoryPricePeriod)
                )
            },
            freeTrialPeriod =
            billingInfo.freeTrialPeriod.takeIf(String::isNotEmpty)
                ?.let(::mapSubscriptionPeriod),
            skuDetails = billingInfo,
        )

    @JvmSynthetic
    fun mapToMakePurchase(product: AdaptyPaywallProduct) =
        MakePurchaseProductInfo(
            vendorProductId = product.vendorProductId,
            type = product.payloadData.type,
            priceAmountMicros = product.payloadData.priceAmountMicros,
            currencyCode = product.currencyCode,
            variationId = product.variationId,
        )

    @JvmSynthetic
    fun mapToValidate(product: MakePurchaseProductInfo) =
        ValidateProductInfo(
            vendorProductId = product.vendorProductId,
            originalPrice = formatPrice(product.priceAmountMicros),
            priceLocale = product.currencyCode,
            variationId = product.variationId,
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
                ?.takeIf(String::isNotEmpty)?.let(::mapSubscriptionPeriod),
        )

    @JvmSynthetic
    fun mapToSyncedPurchase(historyRecord: PurchaseHistoryRecordModel) =
        SyncedPurchase(
            purchaseToken = historyRecord.purchase.purchaseToken,
            purchaseTime = historyRecord.purchase.purchaseTime,
        )

    private fun mapIntroductoryOfferEligibility(productDto: ProductDto, source: Source) =
        when {
            productDto.introductoryOfferEligibility == null || source == Source.FALLBACK -> UNKNOWN
            productDto.introductoryOfferEligibility == true -> ELIGIBLE
            else -> INELIGIBLE
        }

    private fun mapSubscriptionPeriod(period: String): AdaptyProductSubscriptionPeriod {
        val unit = getPeriodUnit(period)
        val numberOfUnits = getPeriodNumberOfUnits(period)

        return AdaptyProductSubscriptionPeriod(
            if (numberOfUnits == 0) AdaptyPeriodUnit.UNKNOWN else unit,
            if (unit == AdaptyPeriodUnit.UNKNOWN) 0 else numberOfUnits,
        )
    }

    private fun mapProductDiscount(productDiscount: ProductDiscountData) = AdaptyProductDiscount(
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
        when (period.lastOrNull()) {
            'D', 'd' -> AdaptyPeriodUnit.DAY
            'W', 'w' -> AdaptyPeriodUnit.WEEK
            'M', 'm' -> AdaptyPeriodUnit.MONTH
            'Y', 'y' -> AdaptyPeriodUnit.YEAR
            else -> AdaptyPeriodUnit.UNKNOWN
        }

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
        } ?: 0

    private val discountPeriodMultipliers = mapOf(
        "Y" to 365,
        "M" to 30,
        "W" to 7,
        "D" to 1
    )

    private fun getLocalizedSubscriptionPeriod(period: AdaptyProductSubscriptionPeriod): String {
        val period = when {
            period.unit == AdaptyPeriodUnit.DAY && period.numberOfUnits == 7 -> {
                AdaptyProductSubscriptionPeriod(AdaptyPeriodUnit.WEEK, 1)
            }
            else -> period
        }

        val pluralsRes = when (period.unit) {
            AdaptyPeriodUnit.DAY -> R.plurals.adapty_day
            AdaptyPeriodUnit.WEEK -> R.plurals.adapty_week
            AdaptyPeriodUnit.MONTH -> R.plurals.adapty_month
            AdaptyPeriodUnit.YEAR -> R.plurals.adapty_year
            else -> return ""
        }

        return context.resources.getQuantityString(
            pluralsRes,
            period.numberOfUnits,
            period.numberOfUnits
        )
    }
}