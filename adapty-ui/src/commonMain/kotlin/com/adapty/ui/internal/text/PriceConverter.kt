package com.adapty.ui.internal.text

import com.adapty.ui.internal.platform.AdaptyPeriodUnit
import com.adapty.ui.internal.platform.BigDecimalLike

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
        return PlatformFormat.multiply(PlatformFormat.divide(amount, numberOfUnits), unitsInYear)
    }

    fun toMonthly(
        amount: BigDecimalLike,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimalLike {
        val divisor: Int
        val multiplier: Int
        when (unit) {
            AdaptyPeriodUnit.YEAR -> {
                divisor = 12 * numberOfUnits
                multiplier = 1
            }
            AdaptyPeriodUnit.MONTH -> {
                divisor = numberOfUnits
                multiplier = 1
            }
            AdaptyPeriodUnit.WEEK -> {
                divisor = 7 * numberOfUnits
                multiplier = 30
            }
            else -> {
                divisor = numberOfUnits
                multiplier = 30
            }
        }
        return PlatformFormat.multiply(PlatformFormat.divide(amount, divisor), multiplier)
    }

    fun toWeekly(
        amount: BigDecimalLike,
        unit: AdaptyPeriodUnit,
        numberOfUnits: Int,
    ): BigDecimalLike {
        if (unit == AdaptyPeriodUnit.DAY) {
            return PlatformFormat.divide(PlatformFormat.multiply(amount, 7), numberOfUnits)
        }
        if (unit == AdaptyPeriodUnit.MONTH) {
            return PlatformFormat.divide(PlatformFormat.multiply(amount, 7), 30 * numberOfUnits)
        }
        val weeksInUnit = when (unit) {
            AdaptyPeriodUnit.YEAR -> 52
            else -> 1
        }
        return PlatformFormat.divide(amount, weeksInUnit * numberOfUnits)
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
        return PlatformFormat.divide(amount, daysInUnit * numberOfUnits)
    }
}
