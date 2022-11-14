package com.adapty.models

import com.adapty.utils.ImmutableList
import com.adapty.utils.ImmutableMap

public class AdaptyPaywall(
    public val id: String,
    public val name: String,
    public val abTestName: String,
    public val revision: Int,
    public val variationId: String,
    public val vendorProductIds: ImmutableList<String>,
    public val customPayloadString: String?,
    public val customPayload: ImmutableMap<String, Any>?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdaptyPaywall

        if (id != other.id) return false
        if (name != other.name) return false
        if (abTestName != other.abTestName) return false
        if (revision != other.revision) return false
        if (variationId != other.variationId) return false
        if (vendorProductIds != other.vendorProductIds) return false
        if (customPayloadString != other.customPayloadString) return false
        if (customPayload != other.customPayload) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + abTestName.hashCode()
        result = 31 * result + revision
        result = 31 * result + variationId.hashCode()
        result = 31 * result + vendorProductIds.hashCode()
        result = 31 * result + (customPayloadString?.hashCode() ?: 0)
        result = 31 * result + (customPayload?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "AdaptyPaywall(id=$id, name=$name, abTestName=$abTestName, revision=$revision, variationId=$variationId, vendorProductIds=$vendorProductIds, customPayloadString=$customPayloadString, customPayload=$customPayload)"
    }
}