package com.adapty.models

/**
 * An identifier of an attribution source applied to the profile.
 *
 * The set of possible values is not limited to the predefined constants: new identifiers
 * may be introduced over time. Instances are compared by [value], so the predefined
 * constants can be used for comparison.
 *
 * @property[value] The raw string identifier of the attribution source.
 */
public class AdaptyAttributionSource(
    value: String,
) {

    public val value: String = value.trim()

    public companion object {
        @JvmField
        public val APPLE_ADS: AdaptyAttributionSource = AdaptyAttributionSource("apple_search_ads")
        @JvmField
        public val ADJUST: AdaptyAttributionSource = AdaptyAttributionSource("adjust")
        @JvmField
        public val APPSFLYER: AdaptyAttributionSource = AdaptyAttributionSource("appsflyer")
        @JvmField
        public val BRANCH: AdaptyAttributionSource = AdaptyAttributionSource("branch")
        @JvmField
        public val TENJIN: AdaptyAttributionSource = AdaptyAttributionSource("tenjin")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyAttributionSource

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }
}
