package com.adapty.models

/**
 * An external attribution provider recognized by the Adapty backend.
 *
 * The set of possible values is not limited to the predefined constants: new identifiers
 * may be introduced over time. Instances are compared by [value], so the predefined
 * constants can be used for comparison.
 *
 * @property[value] The raw string identifier of the external attribution provider.
 */
public class AdaptyExternalAttributionProvider(
    value: String,
) {

    public val value: String = value.trim()

    public companion object {
        @JvmField
        public val APPLE_ADS: AdaptyExternalAttributionProvider = AdaptyExternalAttributionProvider("apple_search_ads")
        @JvmField
        public val ADJUST: AdaptyExternalAttributionProvider = AdaptyExternalAttributionProvider("adjust")
        @JvmField
        public val APPSFLYER: AdaptyExternalAttributionProvider = AdaptyExternalAttributionProvider("appsflyer")
        @JvmField
        public val BRANCH: AdaptyExternalAttributionProvider = AdaptyExternalAttributionProvider("branch")
        @JvmField
        public val TENJIN: AdaptyExternalAttributionProvider = AdaptyExternalAttributionProvider("tenjin")
        @JvmField
        public val CUSTOM: AdaptyExternalAttributionProvider = AdaptyExternalAttributionProvider("custom")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyExternalAttributionProvider

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value
    }
}
