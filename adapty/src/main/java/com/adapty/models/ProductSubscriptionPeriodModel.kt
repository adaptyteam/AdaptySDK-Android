package com.adapty.models

public class ProductSubscriptionPeriodModel(
    public val unit: PeriodUnit?,
    public val numberOfUnits: Int?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductSubscriptionPeriodModel

        if (unit != other.unit) return false
        if (numberOfUnits != other.numberOfUnits) return false

        return true
    }

    override fun hashCode(): Int {
        var result = unit?.hashCode() ?: 0
        result = 31 * result + (numberOfUnits ?: 0)
        return result
    }

    override fun toString(): String {
        return "ProductSubscriptionPeriodModel(unit=$unit, numberOfUnits=$numberOfUnits)"
    }
}

public enum class PeriodUnit {

    /**
     * Day
     */
    D,

    /**
     * Week
     */
    W,

    /**
     * Month
     */
    M,

    /**
     * Year
     */
    Y
}