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
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.RichText
import com.adapty.ui.internal.mapping.element.Assets
import com.adapty.ui.internal.mapping.element.Products
import com.adapty.ui.internal.ui.element.BaseTextElement.Attributes
import com.adapty.ui.internal.utils.firstDiscountOfferOrNull
import com.adapty.ui.listeners.AdaptyUiTagResolver
import java.math.RoundingMode

internal class TagResolver(
    private val priceFormatter: PriceFormatter,
    private val priceConverter: PriceConverter,
    var customTagResolver: AdaptyUiTagResolver,
) {

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
            ProductTags.offerPeriod -> firstDiscountOfferIfExists?.localizedSubscriptionPeriod
            ProductTags.offerNumberOfPeriods -> firstDiscountOfferIfExists?.localizedNumberOfPeriods
            else -> {
                when (item.tag) {
                    ProductTags.price -> product.price.localizedString
                    ProductTags.pricePerDay -> createPricePerPeriodText(product, DAY)
                    ProductTags.pricePerWeek -> createPricePerPeriodText(product, WEEK)
                    ProductTags.pricePerMonth -> createPricePerPeriodText(product, MONTH)
                    ProductTags.pricePerYear -> createPricePerPeriodText(product, YEAR)
                    ProductTags.offerPrice -> firstDiscountOfferIfExists?.price?.localizedString
                    else -> ""
                }
            }
        }.orEmpty()
        return StringWrapper.Str(text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) })
    }

    private fun createPricePerPeriodText(product: AdaptyPaywallProduct, targetUnit: AdaptyPeriodUnit): String? {
        val subscriptionPeriod = product.subscriptionDetails?.subscriptionPeriod
        val price = product.price
        val unit =
            subscriptionPeriod?.unit?.takeIf { it in listOf(WEEK, YEAR, MONTH) } ?: return null
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
                    DAY -> priceConverter.toDaily(price, unit, numberOfUnits)
                    WEEK -> priceConverter.toWeekly(price, unit, numberOfUnits)
                    MONTH -> priceConverter.toMonthly(price, unit, numberOfUnits)
                    else -> priceConverter.toYearly(price, unit, numberOfUnits)
                }
                priceFormatter.format(pricePerPeriod, localizedPrice)
            }
        }
    }

    @Composable
    fun tryResolveTimerTag(
        item: RichText.Item.Tag,
        textElementAttrs: Attributes?,
        assets: Assets,
    ): StringWrapper.TimerSegmentStr? {
        val tag = item.tag
        if (!tag.startsWith(TimerTags.timerPrefix))
            return null
        TimerTags.startingParts.forEach { startingPart ->
            if (tag.startsWith(startingPart)) {
                val numberOfDigits = tag.drop(startingPart.length).toIntOrNull()?.coerceAtLeast(1)
                    ?: return null
                val timeUnit = resolveTimeUnit(tag)
                return StringWrapper.TimerSegmentStr(
                    "%0${numberOfDigits}d",
                    timeUnit,
                    item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) },
                )
            }
        }
        val numberOfDigits = when (tag) {
            in TimerTags.componentValues1Digit -> 1
            in TimerTags.componentValues2Digit -> 2
            in TimerTags.componentValues3Digit -> 3
            else -> return null
        }
        val timeUnit = resolveTimeUnit(tag)
        return StringWrapper.TimerSegmentStr(
            "%0${numberOfDigits}d",
            timeUnit,
            item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) },
        )
    }

    private fun resolveTimeUnit(strToContain: String): TimerSegment {
        return when {
            strToContain.contains(TimerTags.milliseconds) || strToContain.contains(TimerTags.millisecondsShort) -> TimerSegment.MILLISECONDS
            strToContain.contains(TimerTags.centisecondsShort) -> TimerSegment.CENTISECONDS
            strToContain.contains(TimerTags.decisecondsShort) -> TimerSegment.DECISECONDS
            strToContain.contains(TimerTags.seconds) || strToContain.contains("_${TimerTags.secondsShort}") -> TimerSegment.SECONDS
            strToContain.contains(TimerTags.minutes) || strToContain.contains("_${TimerTags.minutesShort}") -> TimerSegment.MINUTES
            strToContain.contains(TimerTags.hours) || strToContain.contains("_${TimerTags.hoursShort}") -> TimerSegment.HOURS
            strToContain.contains(TimerTags.days) -> TimerSegment.DAYS
            else -> TimerSegment.UNKNOWN
        }
    }

    @Composable
    fun tryResolveCustomTag(
        item: RichText.Item.Tag,
        textElementAttrs: Attributes?,
        assets: Assets,
        ignoreMissingCustomTag: Boolean,
    ): StringWrapper.Str {
        val text = customTagResolver.replacement(item.tag) ?: run {
            if (!ignoreMissingCustomTag)
                return StringWrapper.CUSTOM_TAG_NOT_FOUND
            item.tag
        }
        return StringWrapper.Str(text, item.attrs?.let { ComposeTextAttrs.from(it, textElementAttrs, assets) })
    }
}

private object ProductTags {
    const val title = "TITLE"
    const val price = "PRICE"
    const val pricePerDay = "PRICE_PER_DAY"
    const val pricePerWeek = "PRICE_PER_WEEK"
    const val pricePerMonth = "PRICE_PER_MONTH"
    const val pricePerYear = "PRICE_PER_YEAR"
    const val offerPrice = "OFFER_PRICE"
    const val offerPeriod = "OFFER_PERIOD"
    const val offerNumberOfPeriods = "OFFER_NUMBER_OF_PERIOD"

    val all = setOf(
        title,
        price,
        pricePerDay,
        pricePerWeek,
        pricePerMonth,
        pricePerYear,
        offerPrice,
        offerPeriod,
        offerNumberOfPeriods,
    )
}

private object TimerTags {
    const val timerPrefix = "TIMER_"
    const val days = "Days"
    const val hours = "Hours"
    const val minutes = "Minutes"
    const val seconds = "Seconds"
    const val milliseconds = "Milliseconds"
    const val millisecondsShort = "SSS"
    const val centisecondsShort = "SS"
    const val decisecondsShort = "S"
    const val secondsShort = "s"
    const val seconds2Short = "ss"
    const val minutesShort = "m"
    const val minutes2Short = "mm"
    const val hoursShort = "h"
    const val hours2Short = "hh"

    val startingParts = setOf(
        "${timerPrefix}Total_${days}_",
        "${timerPrefix}Total_${hours}_",
        "${timerPrefix}Total_${minutes}_",
        "${timerPrefix}Total_${seconds}_",
        "${timerPrefix}Total_${milliseconds}_",
    )
    val componentValues1Digit = setOf(
        "${timerPrefix}${hoursShort}",
        "${timerPrefix}${minutesShort}",
        "${timerPrefix}${secondsShort}",
        "${timerPrefix}${decisecondsShort}",
    )
    val componentValues2Digit = setOf(
        "${timerPrefix}${hours2Short}",
        "${timerPrefix}${minutes2Short}",
        "${timerPrefix}${seconds2Short}",
        "${timerPrefix}${centisecondsShort}",
    )
    val componentValues3Digit = setOf(
        "${timerPrefix}${millisecondsShort}",
    )
}