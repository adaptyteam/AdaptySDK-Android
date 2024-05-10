package com.adapty.internal.utils

import android.content.Context
import androidx.annotation.RestrictTo
import com.adapty.R
import com.adapty.errors.AdaptyError
import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.data.models.*
import com.adapty.internal.data.models.requests.PurchasedProductDetails
import com.adapty.internal.domain.models.BackendProduct
import com.adapty.internal.domain.models.ProductType.Consumable
import com.adapty.internal.domain.models.ProductType.NonConsumable
import com.adapty.internal.domain.models.ProductType.Subscription
import com.adapty.internal.domain.models.PurchaseableProduct
import com.adapty.models.*
import com.adapty.models.AdaptyEligibility.*
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import java.math.BigDecimal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductMapper(
    private val context: Context,
    private val currencyHelper: CurrencyHelper,
) {

    @JvmSynthetic
    fun map(
        products: List<BackendProduct>,
        billingInfo: Map<String, ProductDetails>,
        paywall: AdaptyPaywall,
    ) =
        products.mapNotNull { product ->
            billingInfo[product.vendorProductId]?.let { productDetails ->
                map(product, productDetails, paywall)
            }
        }

    @JvmSynthetic
    fun map(
        product: BackendProduct,
        productDetails: ProductDetails,
        paywall: AdaptyPaywall,
    ) : AdaptyPaywallProduct? {

        val priceAmountMicros: Long
        val localizedPrice: String
        val currencyCode: String
        val subscriptionData: BackendProduct.SubscriptionData?
        val subscriptionDetails: AdaptyProductSubscriptionDetails?

        when {
            product.type is Subscription -> {
                val subOfferDetails = productDetails.subscriptionOfferDetails ?: kotlin.run {
                    Logger.log(ERROR) { "Subscription data was not found for the product ${product.vendorProductId}" }
                    return null
                }

                val subsData = product.type.subscriptionData

                val basePlanId = subsData.basePlanId
                val offerId = subsData.offerId

                val offer = findCurrentOffer(subOfferDetails, subsData)

                if (offer == null) {
                    Logger.log(ERROR) { "Base plan $basePlanId was not found for the product ${product.vendorProductId}" }
                    return null
                }

                if (offer.offerId == null && offerId != null) {
                    Logger.log(WARN) { "Offer $offerId was not found for the base plan $basePlanId for the product ${product.vendorProductId}" }
                }

                val basePriceInfo = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: kotlin.run {
                    Logger.log(ERROR) { "Subscription price was not found for the ${if (offerId == null) "base plan $basePlanId" else "offer $basePlanId:$offerId"} for the product ${product.vendorProductId}" }
                    return null
                }

                priceAmountMicros = basePriceInfo.priceAmountMicros
                localizedPrice = basePriceInfo.formattedPrice
                currencyCode = basePriceInfo.priceCurrencyCode
                subscriptionData = BackendProduct.SubscriptionData(offer.basePlanId, offer.offerId)

                val subscriptionPeriod = mapSubscriptionPeriod(basePriceInfo.billingPeriod)

                subscriptionDetails = AdaptyProductSubscriptionDetails(
                    basePlanId = basePlanId,
                    offerId = offerId,
                    offerTags = offer.offerTags.immutableWithInterop(),
                    renewalType = when (basePriceInfo.recurrenceMode) {
                        RecurrenceMode.NON_RECURRING -> AdaptyProductSubscriptionDetails.RenewalType.PREPAID
                        else -> AdaptyProductSubscriptionDetails.RenewalType.AUTORENEWABLE
                    },
                    subscriptionPeriod = subscriptionPeriod,
                    localizedSubscriptionPeriod = localize(subscriptionPeriod),
                    introductoryOfferPhases = offer.pricingPhases.pricingPhaseList.dropLast(1)
                        .map { phase ->
                            val phaseSubscriptionPeriod = mapSubscriptionPeriod(phase.billingPeriod)
                            val numberOfPeriods = phase.billingCycleCount

                            AdaptyProductDiscountPhase(
                                price = AdaptyPaywallProduct.Price(
                                    amount = priceFromMicros(phase.priceAmountMicros),
                                    localizedString = phase.formattedPrice,
                                    currencyCode = phase.priceCurrencyCode,
                                    currencySymbol = currencyHelper.getCurrencySymbol(phase.priceCurrencyCode),
                                ),
                                numberOfPeriods = numberOfPeriods,
                                subscriptionPeriod = phaseSubscriptionPeriod,
                                paymentMode = when {
                                    phase.priceAmountMicros == 0L -> AdaptyProductDiscountPhase.PaymentMode.FREE_TRIAL
                                    phase.billingCycleCount > 1 -> AdaptyProductDiscountPhase.PaymentMode.PAY_AS_YOU_GO
                                    else -> AdaptyProductDiscountPhase.PaymentMode.PAY_UPFRONT
                                },
                                localizedNumberOfPeriods = localize(
                                    phaseSubscriptionPeriod.unit,
                                    numberOfPeriods * phaseSubscriptionPeriod.numberOfUnits
                                ),
                                localizedSubscriptionPeriod = localize(phaseSubscriptionPeriod),
                            )
                        }.immutableWithInterop()
                )
            }
            else -> {
                val inappDetails = productDetails.oneTimePurchaseOfferDetails ?: kotlin.run {
                    Logger.log(ERROR) { "In-app data was not found for the product ${product.vendorProductId}" }
                    return null
                }

                subscriptionDetails = null
                subscriptionData = null
                priceAmountMicros = inappDetails.priceAmountMicros
                localizedPrice = inappDetails.formattedPrice
                currencyCode = inappDetails.priceCurrencyCode
            }
        }

        return AdaptyPaywallProduct(
            vendorProductId = product.vendorProductId,
            localizedTitle = productDetails.name,
            localizedDescription = productDetails.description,
            paywallName = paywall.name,
            paywallABTestName = paywall.abTestName,
            variationId = paywall.variationId,
            price = AdaptyPaywallProduct.Price(
                amount = priceFromMicros(priceAmountMicros),
                localizedString = localizedPrice,
                currencyCode = currencyCode,
                currencySymbol = currencyHelper.getCurrencySymbol(currencyCode),
            ),
            subscriptionDetails = subscriptionDetails,
            productDetails = productDetails,
            payloadData = AdaptyPaywallProduct.Payload(
                priceAmountMicros,
                currencyCode,
                product.type.toString(),
                subscriptionData,
            ),
        )
    }

    @JvmSynthetic
    fun map(productDtos: List<ProductDto>) =
        productDtos.map { productDto -> map(productDto) }

    @JvmSynthetic
    fun map(productDto: ProductDto) =
        BackendProduct(
            id = productDto.id ?: throw AdaptyError(
                message = "id in Product should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            vendorProductId = productDto.vendorProductId ?: throw AdaptyError(
                message = "vendorProductId in Product should not be null",
                adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
            ),
            type = when {
                productDto.basePlanId != null -> Subscription(
                    BackendProduct.SubscriptionData(
                        productDto.basePlanId,
                        productDto.offerId,
                    )
                )
                productDto.isConsumable == true -> Consumable
                else -> NonConsumable
            },
            timestamp = productDto.timestamp.orDefault(),
        )

    @JvmSynthetic
    fun mapToPurchaseableProduct(
        product: AdaptyPaywallProduct,
        productDetails: ProductDetails,
        isOfferPersonalized: Boolean,
    ): PurchaseableProduct {
        val subData = product.payloadData.subscriptionData
        val subOfferDetails = productDetails.subscriptionOfferDetails
        val currentOfferDetails = if (subOfferDetails != null && subData != null) {
            findCurrentOffer(subOfferDetails, subData)
        } else {
            null
        }
        return PurchaseableProduct(
            vendorProductId = product.vendorProductId,
            type = product.payloadData.type,
            priceAmountMicros = product.payloadData.priceAmountMicros,
            currencyCode = product.payloadData.currencyCode,
            variationId = product.variationId,
            currentOfferDetails = currentOfferDetails,
            isOfferPersonalized = isOfferPersonalized,
            productDetails = productDetails,
        )
    }

    @JvmSynthetic
    fun mapToRestore(
        purchaseRecord: PurchaseRecordModel,
        productDetails: ProductDetails?,
    ) =
        RestoreProductInfo(
            isSubscription = purchaseRecord.type == ProductType.SUBS,
            productId = purchaseRecord.products.firstOrNull(),
            purchaseToken = purchaseRecord.purchaseToken,
            productDetails = productDetails?.let(PurchasedProductDetails.Companion::create)
        )

    @JvmSynthetic
    fun mapToSyncedPurchase(purchaseRecord: PurchaseRecordModel) =
        SyncedPurchase(
            purchaseToken = purchaseRecord.purchaseToken,
            purchaseTime = purchaseRecord.purchaseTime,
        )

    private fun mapSubscriptionPeriod(period: String): AdaptyProductSubscriptionPeriod {
        val unit = getPeriodUnit(period)
        val numberOfUnits = getPeriodNumberOfUnits(period)

        return AdaptyProductSubscriptionPeriod(
            if (numberOfUnits == 0) AdaptyPeriodUnit.UNKNOWN else unit,
            if (unit == AdaptyPeriodUnit.UNKNOWN) 0 else numberOfUnits,
        )
    }

    private fun priceFromMicros(priceAmountMicros: Long) =
        priceAmountMicros.takeIf { it > 0L }?.toBigDecimal()?.divide(BigDecimal.valueOf(1_000_000L))
            ?: BigDecimal.ZERO

    private fun findCurrentOffer(
        subOfferDetails: List<SubscriptionOfferDetails>,
        subData: BackendProduct.SubscriptionData,
    ): SubscriptionOfferDetails? {
        val basePlanId = subData.basePlanId
        val offerId = subData.offerId
        var baseOffer: SubscriptionOfferDetails? = null
        for (offer in subOfferDetails) {
            if (offer.basePlanId == basePlanId) {
                if (offer.offerId == offerId) {
                    return offer
                } else if (offer.offerId == null) {
                    baseOffer = offer
                }
            }
        }

        return baseOffer
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

    private fun localize(period: AdaptyProductSubscriptionPeriod): String {
        return localize(period.unit, period.numberOfUnits)
    }

    private fun localize(unit: AdaptyPeriodUnit, numberOfUnits: Int): String {
        val pluralsRes = when (unit) {
            AdaptyPeriodUnit.DAY -> R.plurals.adapty_day
            AdaptyPeriodUnit.WEEK -> R.plurals.adapty_week
            AdaptyPeriodUnit.MONTH -> R.plurals.adapty_month
            AdaptyPeriodUnit.YEAR -> R.plurals.adapty_year
            else -> return ""
        }

        return context.resources.getQuantityString(pluralsRes, numberOfUnits, numberOfUnits)
    }
}