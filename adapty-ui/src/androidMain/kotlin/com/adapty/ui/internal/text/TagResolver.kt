@file:OptIn(InternalAdaptyUiApi::class)

package com.adapty.ui.internal.text

import androidx.compose.runtime.Composable
import com.adapty.ui.AdaptyUI.FlowConfiguration.RichText
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.Products
import com.adapty.ui.internal.platform.AdaptyPaywallProduct
import com.adapty.ui.internal.platform.AdaptyPaywallProductPrice
import com.adapty.ui.internal.platform.AdaptyPeriodUnit
import com.adapty.ui.internal.platform.AdaptyProductDiscountPhase
import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.PaymentMode
import com.adapty.ui.internal.platform.PlatformLocale
import com.adapty.ui.internal.platform.bigDecimalZero
import com.adapty.ui.internal.platform.defaultLocale
import com.adapty.ui.internal.ui.PriceFormatting
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.utils.firstDiscountOfferOrNull
import com.adapty.ui.listeners.AdaptyUiTagResolver

public enum class MissingTagKind { UNKNOWN_TAG, EMPTY_VARIABLE }

internal class TagResolver(
    private val priceFormatter: PriceFormatting,
    private val priceConverter: PriceConverter,
    var customTagResolver: AdaptyUiTagResolver,
    private val locale: PlatformLocale = defaultLocale(),
    private val missingTagHandler: ((tag: String, kind: MissingTagKind) -> String?)? = null,
) {

    @Composable
    fun tryResolveInlineTag(
        item: RichText.Item.Tag,
        tagValues: Map<String, TagValueSource>?,
        textElementAttrs: Attributes?,
        assets: Assets,
        locale: PlatformLocale,
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
            ProductTags.pricePerDay -> createPricePerPeriodText(product, AdaptyPeriodUnit.DAY)
            ProductTags.pricePerWeek -> createPricePerPeriodText(product, AdaptyPeriodUnit.WEEK)
            ProductTags.pricePerMonth -> createPricePerPeriodText(product, AdaptyPeriodUnit.MONTH)
            ProductTags.pricePerYear -> createPricePerPeriodText(product, AdaptyPeriodUnit.YEAR)
            ProductTags.offerPrice -> firstDiscountOfferIfExists?.price?.localizedString
            ProductTags.offerPeriod -> firstDiscountOfferIfExists?.localizedSubscriptionPeriod
            ProductTags.offerNumberOfPeriods -> firstDiscountOfferIfExists?.localizedNumberOfPeriods
            ProductTags.offerPricePerDay -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, AdaptyPeriodUnit.DAY)
            ProductTags.offerPricePerWeek -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, AdaptyPeriodUnit.WEEK)
            ProductTags.offerPricePerMonth -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, AdaptyPeriodUnit.MONTH)
            ProductTags.offerPricePerYear -> createOfferPricePerPeriodText(firstDiscountOfferIfExists, AdaptyPeriodUnit.YEAR)
            else -> ""
        }.orEmpty()
        return StringWrapper.Str(text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) }, item.actions)
    }

    private fun createPricePerPeriodText(product: AdaptyPaywallProduct, targetUnit: AdaptyPeriodUnit): String? {
        val subscriptionPeriod = product.subscriptionDetails?.subscriptionPeriod
        val price = product.price
        val unit =
            subscriptionPeriod?.unit?.takeIf { it in listOf(AdaptyPeriodUnit.DAY, AdaptyPeriodUnit.WEEK, AdaptyPeriodUnit.MONTH, AdaptyPeriodUnit.YEAR) } ?: return null
        val numberOfUnits = subscriptionPeriod.numberOfUnits.takeIf { it > 0 } ?: return null
        val localizedPrice = price.localizedString
        return when {
            unit == targetUnit && numberOfUnits == 1 -> localizedPrice
            else -> {
                val pricePerPeriod = when (targetUnit) {
                    unit -> PlatformFormat.divide(price.amount, numberOfUnits)
                    AdaptyPeriodUnit.DAY -> priceConverter.toDaily(price.amount, unit, numberOfUnits)
                    AdaptyPeriodUnit.WEEK -> priceConverter.toWeekly(price.amount, unit, numberOfUnits)
                    AdaptyPeriodUnit.MONTH -> priceConverter.toMonthly(price.amount, unit, numberOfUnits)
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
        val unit = period.unit.takeIf { it in listOf(AdaptyPeriodUnit.DAY, AdaptyPeriodUnit.WEEK, AdaptyPeriodUnit.MONTH, AdaptyPeriodUnit.YEAR) } ?: return null
        val numberOfUnits = period.numberOfUnits.takeIf { it > 0 } ?: return null

        val effectiveAmount: BigDecimalLike
        val repeatCount: Int
        when (offer.paymentMode) {
            PaymentMode.FREE_TRIAL -> {
                effectiveAmount = bigDecimalZero()
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

        if (effectiveAmount.compareTo(bigDecimalZero()) == 0) {
            return priceFormatter.format(bigDecimalZero(), offer.price.localizedString)
        }

        val singlePeriodPrice = when (targetUnit) {
            unit -> PlatformFormat.divide(effectiveAmount, numberOfUnits)
            AdaptyPeriodUnit.DAY -> priceConverter.toDaily(effectiveAmount, unit, numberOfUnits)
            AdaptyPeriodUnit.WEEK -> priceConverter.toWeekly(effectiveAmount, unit, numberOfUnits)
            AdaptyPeriodUnit.MONTH -> priceConverter.toMonthly(effectiveAmount, unit, numberOfUnits)
            else -> priceConverter.toYearly(effectiveAmount, unit, numberOfUnits)
        }

        val pricePerPeriod = if (repeatCount > 1) {
            PlatformFormat.divide(singlePeriodPrice, repeatCount)
        } else {
            singlePeriodPrice
        }
        return priceFormatter.format(pricePerPeriod, offer.price.localizedString)
    }

    private fun fractionDigitsForCurrency(currencyCode: String): Int =
        PlatformFormat.currencyFractionDigits(currencyCode)

    private fun formatPriceAmount(price: AdaptyPaywallProductPrice): String {
        val fractionDigits = fractionDigitsForCurrency(price.currencyCode)
        return PlatformFormat.formatPriceAmount(price.amount, fractionDigits, locale)
    }

    private fun formatPriceAmountInteger(price: AdaptyPaywallProductPrice): String =
        PlatformFormat.formatPriceAmountInteger(price.amount, locale)

    private fun formatPriceAmountFraction(price: AdaptyPaywallProductPrice): String? {
        val fractionDigits = fractionDigitsForCurrency(price.currencyCode)
        if (fractionDigits <= 0) return null
        return PlatformFormat.formatPriceAmountFraction(price.amount, fractionDigits)
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
