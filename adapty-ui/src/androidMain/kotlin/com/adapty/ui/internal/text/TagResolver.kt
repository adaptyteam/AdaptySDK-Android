@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.text

import androidx.compose.runtime.Composable
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.PriceFormatter
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPeriodUnit
import com.adapty.models.AdaptyPeriodUnit.DAY
import com.adapty.models.AdaptyPeriodUnit.MONTH
import com.adapty.models.AdaptyPeriodUnit.WEEK
import com.adapty.models.AdaptyPeriodUnit.YEAR
import com.adapty.models.AdaptyProductDiscountPhase
import com.adapty.models.AdaptyProductDiscountPhase.PaymentMode
import com.adapty.ui.AdaptyUI.FlowConfiguration.RichText
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.Products
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.utils.firstDiscountOfferOrNull
import com.adapty.ui.listeners.AdaptyUiTagResolver
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal enum class MissingTagKind { UNKNOWN_TAG, EMPTY_VARIABLE }

internal class TagResolver(
    private val priceFormatter: PriceFormatter,
    private val priceConverter: PriceConverter,
    var customTagResolver: AdaptyUiTagResolver,
    private val locale: Locale = Locale.getDefault(),
    private val missingTagHandler: ((tag: String, kind: MissingTagKind) -> String?)? = null,
) {

    @Composable
    fun tryResolveInlineTag(
        item: RichText.Item.Tag,
        tagValues: Map<String, TagValueSource>?,
        textElementAttrs: Attributes?,
        assets: Assets,
        locale: Locale,
        literalOnly: Boolean,
    ): StringWrapper.Str? {
        val source = tagValues?.get(item.tag) ?: return null
        if ((source is TagValueSource.Literal) != literalOnly) return null
        val text = source.resolve()?.let { rawValue ->
            val skipConverter = source is TagValueSource.Binding && rawValue is String
            val converter = if (skipConverter) null else resolveConverter(item, source.defaultConverter)
            if (converter != null) converter.format(rawValue, locale) else rawValue.toTagString()
        } ?: run {
            val kind = if (source is TagValueSource.Binding) MissingTagKind.EMPTY_VARIABLE else MissingTagKind.UNKNOWN_TAG
            missingTagHandler?.invoke(item.tag, kind)
        } ?: ""
        return StringWrapper.Str(text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }, item.actions)
    }

    private fun resolveConverter(
        item: RichText.Item.Tag,
        defaultConverter: ConverterSpec?,
    ): TagConverter? {
        val name = item.converterName ?: defaultConverter?.name ?: return null
        val params = if (item.converterName != null) {
            item.converterParams
        } else {
            mergeParams(defaultConverter?.params, item.converterParams)
        }
        return TagConverter.fromJson(ConverterSpec(name, params))
    }

    private fun mergeParams(
        base: Map<String, Any?>?,
        override: Map<String, Any?>?,
    ): Map<String, Any?>? = when {
        base.isNullOrEmpty() -> override
        override.isNullOrEmpty() -> base
        else -> base + override
    }

    @Composable
    fun tryResolveProductTag(
        item: RichText.Item.Tag,
        productId: String?,
        textElementAttrs: Attributes?,
        assets: Assets,
        products: Products,
    ): StringWrapper.Str? {
        if (item.tag !in ProductTags.all)
            return null
        val product = products[productId.orEmpty()] ?: return StringWrapper.PRODUCT_NOT_FOUND
        val firstDiscountOfferIfExists = product.firstDiscountOfferOrNull()
        val text = when (item.tag) {
            ProductTags.title -> product.localizedTitle
            ProductTags.description -> product.localizedDescription
            ProductTags.price -> product.price.localizedString
            ProductTags.priceAmount -> formatPriceAmount(product.price)
            ProductTags.priceAmountInteger -> formatPriceAmountInteger(product.price)
            ProductTags.priceAmountFraction -> formatPriceAmountFraction(product.price)
            ProductTags.currencyCode -> product.price.currencyCode
            ProductTags.currencySymbol -> product.price.currencySymbol
            ProductTags.subscriptionPeriod -> product.subscriptionDetails?.localizedSubscriptionPeriod
            ProductTags.pricePerDay -> createPricePerPeriodText(product, DAY)
            ProductTags.pricePerWeek -> createPricePerPeriodText(product, WEEK)
            ProductTags.pricePerMonth -> createPricePerPeriodText(product, MONTH)
            ProductTags.pricePerYear -> createPricePerPeriodText(product, YEAR)
            ProductTags.offerPrice -> firstDiscountOfferIfExists?.price?.localizedString
            ProductTags.offerPeriod -> firstDiscountOfferIfExists?.localizedSubscriptionPeriod
            ProductTags.offerNumberOfPeriods -> firstDiscountOfferIfExists?.localizedNumberOfPeriods
            ProductTags.offerPricePerDay -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, DAY)
            ProductTags.offerPricePerWeek -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, WEEK)
            ProductTags.offerPricePerMonth -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, MONTH)
            ProductTags.offerPricePerYear -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, YEAR)
            else -> ""
        }.orEmpty()
        return StringWrapper.Str(text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }, item.actions)
    }

    private fun createPricePerPeriodText(product: AdaptyPaywallProduct, targetUnit: AdaptyPeriodUnit): String? {
        val subscriptionPeriod = product.subscriptionDetails?.subscriptionPeriod
        val price = product.price
        val unit =
            subscriptionPeriod?.unit?.takeIf { it in listOf(DAY, WEEK, MONTH, YEAR) } ?: return null
        val numberOfUnits = subscriptionPeriod.numberOfUnits.takeIf { it > 0 } ?: return null
        val localizedPrice = price.localizedString
        return when {
            unit == targetUnit && numberOfUnits == 1 -> localizedPrice
            else -> {
                val pricePerPeriod = when (targetUnit) {
                    unit -> price.amount.divide(
                        numberOfUnits.toBigDecimal(),
                        4,
                        RoundingMode.CEILING
                    )
                    DAY -> priceConverter.toDaily(price.amount, unit, numberOfUnits)
                    WEEK -> priceConverter.toWeekly(price.amount, unit, numberOfUnits)
                    MONTH -> priceConverter.toMonthly(price.amount, unit, numberOfUnits)
                    else -> priceConverter.toYearly(price.amount, unit, numberOfUnits)
                }
                priceFormatter.format(pricePerPeriod, localizedPrice)
            }
        }
    }

    private fun createOfferPricePerPeriodText(
        offer: AdaptyProductDiscountPhase?,
        targetUnit: AdaptyPeriodUnit,
    ): String? {
        offer ?: return null
        val period = offer.subscriptionPeriod
        val unit = period.unit.takeIf { it in listOf(DAY, WEEK, MONTH, YEAR) } ?: return null
        val numberOfUnits = period.numberOfUnits.takeIf { it > 0 } ?: return null

        val effectiveAmount: BigDecimal
        val repeatCount: Int
        when (offer.paymentMode) {
            PaymentMode.FREE_TRIAL -> {
                effectiveAmount = BigDecimal.ZERO
                repeatCount = 1
            }
            PaymentMode.PAY_UPFRONT -> {
                effectiveAmount = offer.price.amount
                repeatCount = offer.numberOfPeriods
            }
            else -> {
                effectiveAmount = offer.price.amount
                repeatCount = 1
            }
        }

        if (effectiveAmount.compareTo(BigDecimal.ZERO) == 0) {
            return priceFormatter.format(BigDecimal.ZERO, offer.price.localizedString)
        }

        val singlePeriodPrice = when (targetUnit) {
            unit -> effectiveAmount.divide(numberOfUnits.toBigDecimal(), 4, RoundingMode.CEILING)
            DAY -> priceConverter.toDaily(effectiveAmount, unit, numberOfUnits)
            WEEK -> priceConverter.toWeekly(effectiveAmount, unit, numberOfUnits)
            MONTH -> priceConverter.toMonthly(effectiveAmount, unit, numberOfUnits)
            else -> priceConverter.toYearly(effectiveAmount, unit, numberOfUnits)
        }

        val pricePerPeriod = if (repeatCount > 1) {
            singlePeriodPrice.divide(repeatCount.toBigDecimal(), 4, RoundingMode.CEILING)
        } else {
            singlePeriodPrice
        }
        return priceFormatter.format(pricePerPeriod, offer.price.localizedString)
    }

    private fun fractionDigitsForCurrency(currencyCode: String): Int {
        return try {
            Currency.getInstance(currencyCode).defaultFractionDigits
        } catch (_: Exception) {
            2
        }
    }

    private fun formatPriceAmount(price: AdaptyPaywallProduct.Price): String {
        val fractionDigits = fractionDigitsForCurrency(price.currencyCode)
        val formatter = NumberFormat.getInstance(locale)
        formatter.minimumFractionDigits = fractionDigits
        formatter.maximumFractionDigits = fractionDigits
        formatter.isGroupingUsed = true
        return formatter.format(price.amount)
    }

    private fun formatPriceAmountInteger(price: AdaptyPaywallProduct.Price): String {
        val formatter = NumberFormat.getInstance(locale)
        formatter.maximumFractionDigits = 0
        formatter.roundingMode = RoundingMode.DOWN
        formatter.isGroupingUsed = true
        return formatter.format(price.amount)
    }

    private fun formatPriceAmountFraction(price: AdaptyPaywallProduct.Price): String? {
        val fractionDigits = fractionDigitsForCurrency(price.currencyCode)
        if (fractionDigits <= 0) return null
        val multiplier = BigDecimal.TEN.pow(fractionDigits)
        val scaled = price.amount.multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .toBigInteger()
            .abs()
        val modulus = multiplier.toBigInteger()
        val fraction = scaled.mod(modulus)
        return fraction.toString().padStart(fractionDigits, '0')
    }

    @Composable
    fun tryResolveCustomTagOrNull(
        item: RichText.Item.Tag,
        textElementAttrs: Attributes?,
        assets: Assets,
    ): StringWrapper.Str? {
        val text = customTagResolver.replacement(item.tag) ?: return null
        return StringWrapper.Str(text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }, item.actions)
    }

    @Composable
    fun tryResolveMissingTag(
        item: RichText.Item.Tag,
        textElementAttrs: Attributes?,
        assets: Assets,
        ignoreMissingCustomTag: Boolean,
    ): StringWrapper.Str {
        if (!ignoreMissingCustomTag)
            return StringWrapper.CUSTOM_TAG_NOT_FOUND
        missingTagHandler?.invoke(item.tag, MissingTagKind.UNKNOWN_TAG)?.let { placeholder ->
            return StringWrapper.Str(placeholder, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }, item.actions)
        }
        return StringWrapper.EMPTY
    }
}

private fun Any.toTagString(): String? = when (this) {
    is String -> this
    is Number -> {
        val d = toDouble()
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
    }
    is Boolean -> toString()
    else -> null
}

private object ProductTags {
    const val title = "TITLE"
    const val description = "DESCRIPTION"
    const val price = "PRICE"
    const val priceAmount = "PRICE_AMOUNT"
    const val priceAmountInteger = "PRICE_AMOUNT_INTEGER"
    const val priceAmountFraction = "PRICE_AMOUNT_FRACTION"
    const val currencyCode = "CURRENCY_CODE"
    const val currencySymbol = "CURRENCY_SYMBOL"
    const val subscriptionPeriod = "SUBSCRIPTION_PERIOD"
    const val pricePerDay = "PRICE_PER_DAY"
    const val pricePerWeek = "PRICE_PER_WEEK"
    const val pricePerMonth = "PRICE_PER_MONTH"
    const val pricePerYear = "PRICE_PER_YEAR"
    const val offerPrice = "OFFER_PRICE"
    const val offerPeriod = "OFFER_PERIOD"
    const val offerNumberOfPeriods = "OFFER_NUMBER_OF_PERIOD"
    const val offerPricePerDay = "OFFER_PRICE_PER_DAY"
    const val offerPricePerWeek = "OFFER_PRICE_PER_WEEK"
    const val offerPricePerMonth = "OFFER_PRICE_PER_MONTH"
    const val offerPricePerYear = "OFFER_PRICE_PER_YEAR"

    val all = setOf(
        title,
        description,
        price,
        priceAmount,
        priceAmountInteger,
        priceAmountFraction,
        currencyCode,
        currencySymbol,
        subscriptionPeriod,
        pricePerDay,
        pricePerWeek,
        pricePerMonth,
        pricePerYear,
        offerPrice,
        offerPeriod,
        offerNumberOfPeriods,
        offerPricePerDay,
        offerPricePerWeek,
        offerPricePerMonth,
        offerPricePerYear,
    )
}
