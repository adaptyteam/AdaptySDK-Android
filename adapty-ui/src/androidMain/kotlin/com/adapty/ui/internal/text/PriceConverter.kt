package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.AdaptyPeriodUnit
import com.adapty.ui.internal.platform.BigDecimalLike
import com.adapty.ui.internal.platform.RoundingMode
import com.adapty.ui.internal.platform.bigDecimalOf
import com.adapty.ui.internal.platform.bigDecimalOne

internal class PriceConverter {

    fun toYearly(
        amount: BigDecimalLike,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimalLike {
        val unitsInYear = when (unit) {
            AdaptyPeriodUnit.YEAR -> 1
            AdaptyPeriodUnit.MONTH -> 12
            AdaptyPeriodUnit.WEEK -> 52
            else -> 365
        }
        val divisor = numberOfUnits.toBigDecimal()
        val multiplier = unitsInYear.toBigDecimal()
        return amount.divide(divisor, 4, RoundingMode.CEILING) * multiplier
    }

    fun toMonthly(
        amount: BigDecimalLike,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimalLike {
        val divisor: BigDecimalLike
        val multiplier: BigDecimalLike
        when (unit) {
            AdaptyPeriodUnit.YEAR -> {
                divisor = (12 * numberOfUnits).toBigDecimal()
                multiplier = bigDecimalOne()
            }
            AdaptyPeriodUnit.MONTH -> {
                divisor = numberOfUnits.toBigDecimal()
                multiplier = bigDecimalOne()
            }
            AdaptyPeriodUnit.WEEK -> {
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
        amount: BigDecimalLike,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimalLike {
        if (unit == AdaptyPeriodUnit.DAY) {
            return amount.multiply(bigDecimalOf(7))
                .divide(numberOfUnits.toBigDecimal(), 4, RoundingMode.CEILING)
        }
        if (unit == AdaptyPeriodUnit.MONTH) {
            return amount.multiply(bigDecimalOf(7))
                .divide((30 * numberOfUnits).toBigDecimal(), 4, RoundingMode.CEILING)
        }
        val weeksInUnit = when (unit) {
            AdaptyPeriodUnit.YEAR -> 52
            else -> 1
        }
        val divisor = (weeksInUnit * numberOfUnits).toBigDecimal()
        return amount.divide(divisor, 4, RoundingMode.CEILING)
    }

    fun toDaily(
        amount: BigDecimalLike,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimalLike {
        val daysInUnit = when (unit) {
            AdaptyPeriodUnit.YEAR -> 365
            AdaptyPeriodUnit.MONTH -> 30
            AdaptyPeriodUnit.WEEK -> 7
            else -> 1
        }
        val divisor = (daysInUnit * numberOfUnits).toBigDecimal()
        return amount.divide(divisor, 4, RoundingMode.CEILING)
    }
}