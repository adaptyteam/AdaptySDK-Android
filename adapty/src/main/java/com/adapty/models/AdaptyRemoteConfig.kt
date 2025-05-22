package com.adapty.models

import com.adapty.utils.ImmutableMap

/**
 * @property[dataMap] A custom map configured in Adapty Dashboard for this paywall (same as [jsonString])
 * @property[jsonString] A custom JSON string configured in Adapty Dashboard for this paywall.
 * @property[locale] An identifier of a paywall locale.
 */
public class AdaptyRemoteConfig(
    public val locale: String,
    public val jsonString: String,
    public val dataMap: ImmutableMap<String, Any>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyRemoteConfig

        if (locale != other.locale) return false
        if (jsonString != other.jsonString) return false
        if (dataMap != other.dataMap) return false

        return true
    }

    override fun hashCode(): Int {
        var result = locale.hashCode()
        result = 31 * result + jsonString.hashCode()
        result = 31 * result + dataMap.hashCode()
        return result
    }

    override fun toString(): String {
        return "AdaptyRemoteConfig(locale=$locale, dataMap=$dataMap)"
    }
}