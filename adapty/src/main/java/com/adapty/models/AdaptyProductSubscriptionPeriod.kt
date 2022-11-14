package com.adapty.models

public class AdaptyProductSubscriptionPeriod(
    public val unit: AdaptyPeriodUnit,
    public val numberOfUnits: Int
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyProductSubscriptionPeriod

        if (unit != other.unit) return false
        if (numberOfUnits != other.numberOfUnits) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unit.hashCode()
        result = 31 * result + numberOfUnits
        return result
    }

    override fun toString(): String {
        return "AdaptyProductSubscriptionPeriod(unit=$unit, numberOfUnits=$numberOfUnits)"
    }
}