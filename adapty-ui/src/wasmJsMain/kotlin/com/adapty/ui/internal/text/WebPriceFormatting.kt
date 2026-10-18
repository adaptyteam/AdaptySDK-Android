package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.ui.PriceFormatting

/** Port of core `PriceFormatter(locale)`.format(BigDecimal, String) used by the Android preview (Locale.US). */
internal class WebPriceFormatting(private val localeTag: String = "en-US") : PriceFormatting {

    override fun format(newPriceValue: BigDecimalLike, originalFormattedPrice: String): String {
        val formattedNumber = if (newPriceValue.isIntegral()) {
            intlFormatNumber(newPriceValue.toDouble(), localeTag, 0, 0)
        } else {
            // setScale(2, CEILING) + NumberFormat(minimumFractionDigits = 2)
            intlFormatNumber(roundDecimal(newPriceValue.toDouble(), 2, WebRounding.CEILING), localeTag, 2, 3)
        }
        return replacePrice(formattedNumber, originalFormattedPrice)
    }

    private fun replacePrice(formattedNumber: String, originalFormattedPrice: String): String {
        val range = findPriceValueRange(originalFormattedPrice)
        return if (range.first > -1 && range.last in range.first until originalFormattedPrice.length) {
            originalFormattedPrice.replace(originalFormattedPrice.substring(range), formattedNumber)
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
}
