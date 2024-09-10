package com.adapty.internal.utils

import androidx.annotation.RestrictTo
import com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import java.lang.ArithmeticException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalAdaptyApi
public class PriceFormatter internal constructor(
    private val intNumberFormat: NumberFormat,
    private val decimalNumberFormat: NumberFormat,
) {
    public constructor(locale: Locale): this(
        NumberFormat.getInstance(locale).apply {
            maximumFractionDigits = 0
            isGroupingUsed = true
        },
        NumberFormat.getInstance(locale).apply {
            minimumFractionDigits = 2
            isGroupingUsed = true
        },
    )

    public fun format(pricingPhase: PricingPhase): String {
        return formatPriceMicros(
            pricingPhase.priceAmountMicros,
            pricingPhase.formattedPrice,
        )
    }

    public fun format(oneTimeOfferDetails: OneTimePurchaseOfferDetails): String {
        return formatPriceMicros(
            oneTimeOfferDetails.priceAmountMicros,
            oneTimeOfferDetails.formattedPrice,
        )
    }

    public fun format(newPriceValue: BigDecimal, originalFormattedPrice: String): String {
        return try {
            val newPriceBigInt = newPriceValue.toBigIntegerExact()
            formatBigInt(newPriceBigInt, originalFormattedPrice)
        } catch (e: ArithmeticException) {
            formatBigDecimal(newPriceValue, originalFormattedPrice)
        }
    }

    public fun formatCurrencySymbol(pricingPhase: PricingPhase): String {
        return formatCurrencySymbol(pricingPhase.formattedPrice)
    }

    public fun formatCurrencySymbol(oneTimeOfferDetails: OneTimePurchaseOfferDetails): String {
        return formatCurrencySymbol(oneTimeOfferDetails.formattedPrice)
    }

    private fun formatCurrencySymbol(formattedPrice: String): String {
        val range = findPriceValueRange(formattedPrice)
        return formattedPrice.removeRange(range)
            .trim { char -> char.isWhitespace() || char.isDirectionMark() }
    }

    private fun formatBigDecimal(newPriceValue: BigDecimal, originalFormattedPrice: String): String {
        val formattedNumber =
            newPriceValue
                .setScale(2, RoundingMode.CEILING)
                .let(decimalNumberFormat::format)

        return replacePrice(formattedNumber, originalFormattedPrice)
    }

    private fun formatBigInt(newPriceValue: BigInteger, originalFormattedPrice: String): String {
        val formattedNumber =
            newPriceValue
                .let(intNumberFormat::format)

        return replacePrice(formattedNumber, originalFormattedPrice)
    }

    private fun formatPriceMicros(priceMicros: Long, originalFormattedPrice: String): String {
        val priceValue = BigDecimal.valueOf(priceMicros)
            .divide(DIVIDER)
        return format(priceValue, originalFormattedPrice)
    }

    private fun replacePrice(formattedNumber: String, originalFormattedPrice: String): String {
        val range = findPriceValueRange(originalFormattedPrice)
        return if (range.first > -1 && range.last in range.first until originalFormattedPrice.length) {
            originalFormattedPrice.replace(
                originalFormattedPrice.substring(range),
                formattedNumber
            )
        } else {
            originalFormattedPrice
        }
    }

    private fun findPriceValueRange(originalFormattedPrice: String): IntRange {
        var startIndex = -1
        var endIndex = -1
        for ((i, ch) in originalFormattedPrice.withIndex()) {
            if (ch.isDigit()) {
                if (startIndex == -1) startIndex = i
                endIndex = i
            }
        }
        return startIndex..endIndex
    }

    private fun Char.isDirectionMark() = this in DIRECTION_MARKS

    private companion object {
        val DIVIDER = BigDecimal.valueOf(1_000_000L)
        val DIRECTION_MARKS = setOf('\u200f', '\u200e', '\u061c')
    }
}