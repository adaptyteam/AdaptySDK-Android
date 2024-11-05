package com.adapty.ui.internal.text

import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyPeriodUnit
import com.adapty.models.AdaptyPeriodUnit.MONTH
import com.adapty.models.AdaptyPeriodUnit.YEAR
import java.math.BigDecimal
import java.math.RoundingMode

internal class PriceConverter {

    fun toYearly(
        price: AdaptyPaywallProduct.Price,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        val unitsInYear = when (unit) {
            YEAR -> 1
            MONTH -> 12
            else -> 52
        }
        val divisor = numberOfUnits.toBigDecimal()
        val multiplier = unitsInYear.toBigDecimal()
        return price.amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
    }

    fun toMonthly(
        price: AdaptyPaywallProduct.Price,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        val divisor: BigDecimal
        val multiplier: BigDecimal
        when (unit) {
            YEAR -> {
                divisor = (12 * numberOfUnits).toBigDecimal()
                multiplier = BigDecimal.ONE
            }
            MONTH -> {
                divisor = numberOfUnits.toBigDecimal()
                multiplier = BigDecimal.ONE
            }
            else -> {
                divisor = numberOfUnits.toBigDecimal()
                multiplier = 4.toBigDecimal()
            }
        }
        return price.amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
    }

    fun toWeekly(
        price: AdaptyPaywallProduct.Price,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        val weeksInUnit = when (unit) {
            YEAR -> 52
            MONTH -> 4
            else -> 1
        }
        val divisor = (weeksInUnit * numberOfUnits).toBigDecimal()
        return price.amount.divide(divisor, 4, RoundingMode.CEILING)
    }

    fun toDaily(
        price: AdaptyPaywallProduct.Price,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        val daysInUnit = when (unit) {
            YEAR -> 365
            MONTH -> 30
            else -> 7
        }
        val divisor = (daysInUnit * numberOfUnits).toBigDecimal()
        return price.amount.divide(divisor, 4, RoundingMode.CEILING)
    }
}