package com.adapty.ui.internal.text

import com.adapty.models.AdaptyPeriodUnit
import com.adapty.models.AdaptyPeriodUnit.DAY
import com.adapty.models.AdaptyPeriodUnit.MONTH
import com.adapty.models.AdaptyPeriodUnit.WEEK
import com.adapty.models.AdaptyPeriodUnit.YEAR
import java.math.BigDecimal
import java.math.RoundingMode

internal class PriceConverter {

    fun toYearly(
        amount: BigDecimal,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        val unitsInYear = when (unit) {
            YEAR -> 1
            MONTH -> 12
            WEEK -> 52
            else -> 365
        }
        val divisor = numberOfUnits.toBigDecimal()
        val multiplier = unitsInYear.toBigDecimal()
        return amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
    }

    fun toMonthly(
        amount: BigDecimal,
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
            WEEK -> {
                divisor = (7 * numberOfUnits).toBigDecimal()
                multiplier = 30.toBigDecimal()
            }
            else -> {
                divisor = numberOfUnits.toBigDecimal()
                multiplier = 30.toBigDecimal()
            }
        }
        return amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
    }

    fun toWeekly(
        amount: BigDecimal,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        if (unit == DAY) {
            return amount.multiply(BigDecimal(7))
                .divide(numberOfUnits.toBigDecimal(), 4, RoundingMode.CEILING)
        }
        if (unit == MONTH) {
            return amount.multiply(BigDecimal(7))
                .divide((30 * numberOfUnits).toBigDecimal(), 4, RoundingMode.CEILING)
        }
        val weeksInUnit = when (unit) {
            YEAR -> 52
            else -> 1
        }
        val divisor = (weeksInUnit * numberOfUnits).toBigDecimal()
        return amount.divide(divisor, 4, RoundingMode.CEILING)
    }

    fun toDaily(
        amount: BigDecimal,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimal {
        val daysInUnit = when (unit) {
            YEAR -> 365
            MONTH -> 30
            WEEK -> 7
            else -> 1
        }
        val divisor = (daysInUnit * numberOfUnits).toBigDecimal()
        return amount.divide(divisor, 4, RoundingMode.CEILING)
    }
}