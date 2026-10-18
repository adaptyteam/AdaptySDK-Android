package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.AdaptyPeriodUnit
import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.localeForTag
import kotlin.test.Test
import kotlin.test.assertEquals

// Expected strings are JVM outputs of core PriceFormatter(Locale.US) / PlatformFormat.android for the same BigDecimals
class WebPriceFormattingTest {
    private val formatter = WebPriceFormatting("en-US")
    private val converter = PriceConverter()
    private val us = localeForTag("en-US")

    private fun perPeriod(price: String, target: AdaptyPeriodUnit): String {
        val amount = BigDecimalLike(price.toDouble())
        val value = when (target) {
            AdaptyPeriodUnit.DAY -> converter.toDaily(amount, AdaptyPeriodUnit.MONTH, 1)
            AdaptyPeriodUnit.WEEK -> converter.toWeekly(amount, AdaptyPeriodUnit.MONTH, 1)
            else -> converter.toYearly(amount, AdaptyPeriodUnit.MONTH, 1)
        }
        return formatter.format(value, "$$price")
    }

    @Test
    fun pricePerPeriodMatchesAndroid() {
        val expected = mapOf(
            "1.99" to listOf("$0.47", "$23.88", "$0.07"),
            "9.99" to listOf("$2.34", "$119.88", "$0.34"),
            "79.99" to listOf("$18.67", "$959.88", "$2.67"),
            "49.99" to listOf("$11.67", "$599.88", "$1.67"),
            "99.99" to listOf("$23.34", "$1,199.88", "$3.34"),
            "10" to listOf("$2.34", "$120", "$0.34"),
            "1.015" to listOf("$0.24", "$12.18", "$0.04"),
            "0.125" to listOf("$0.03", "$1.50", "$0.01"),
        )
        for ((price, want) in expected) {
            val got = listOf(AdaptyPeriodUnit.WEEK, AdaptyPeriodUnit.YEAR, AdaptyPeriodUnit.DAY).map { perPeriod(price, it) }
            assertEquals(want, got, "price $price")
        }
    }

    @Test
    fun converterArithmeticMatchesBigDecimal() {
        val amount = BigDecimalLike(79.99)
        assertEquals("18.6644", plainDecimal(converter.toWeekly(amount, AdaptyPeriodUnit.MONTH, 1).toDouble()))
        assertEquals("959.88", plainDecimal(converter.toYearly(amount, AdaptyPeriodUnit.MONTH, 1).toDouble()))
        assertEquals("2.6664", plainDecimal(converter.toDaily(amount, AdaptyPeriodUnit.MONTH, 1).toDouble()))
    }

    @Test
    fun priceAmountTagsMatchAndroid() {
        val cases = mapOf(
            "79.99" to listOf("79.99", "79", "99"),
            "10" to listOf("10.00", "10", "00"),
            "1.015" to listOf("1.02", "1", "02"),
            "0.125" to listOf("0.12", "0", "13"),
            "1234.5" to listOf("1,234.50", "1,234", "50"),
        )
        for ((price, want) in cases) {
            val amount = BigDecimalLike(price.toDouble())
            val got = listOf(
                PlatformFormat.formatPriceAmount(amount, 2, us),
                PlatformFormat.formatPriceAmountInteger(amount, us),
                PlatformFormat.formatPriceAmountFraction(amount, 2),
            )
            assertEquals(want, got, "price $price")
        }
        assertEquals(2, PlatformFormat.currencyFractionDigits("USD"))
        assertEquals(0, PlatformFormat.currencyFractionDigits("JPY"))
        assertEquals(3, PlatformFormat.currencyFractionDigits("KWD"))
        assertEquals(2, PlatformFormat.currencyFractionDigits("???"))
    }

    @Test
    fun replacesOnlyTheNumberOfTheOriginalPrice() {
        assertEquals("€18.67", formatter.format(BigDecimalLike(18.6644), "€79.99"))
        assertEquals("USD 120", formatter.format(BigDecimalLike(120.0), "USD 79.99"))
        assertEquals("free", formatter.format(BigDecimalLike(1.0), "free"))
    }
}
