package com.adapty.models

class PaywallModel(
    val developerId: String,
    val name: String?,
    val abTestName: String?,
    val revision: Int,
    val isPromo: Boolean,
    val variationId: String,
    val products: List<ProductModel>,
    val customPayloadString: String?,
    val customPayload: Map<String, Any>?,
    val visualPaywall: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaywallModel

        if (developerId != other.developerId) return false
        if (name != other.name) return false
        if (abTestName != other.abTestName) return false
        if (revision != other.revision) return false
        if (isPromo != other.isPromo) return false
        if (variationId != other.variationId) return false
        if (products != other.products) return false
        if (customPayloadString != other.customPayloadString) return false
        if (customPayload != other.customPayload) return false
        if (visualPaywall != other.visualPaywall) return false

        return true
    }

    override fun hashCode(): Int {
        var result = developerId.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (abTestName?.hashCode() ?: 0)
        result = 31 * result + revision
        result = 31 * result + isPromo.hashCode()
        result = 31 * result + variationId.hashCode()
        result = 31 * result + products.hashCode()
        result = 31 * result + (customPayloadString?.hashCode() ?: 0)
        result = 31 * result + (customPayload?.hashCode() ?: 0)
        result = 31 * result + (visualPaywall?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "PaywallModel(developerId=$developerId, name=$name, abTestName=$abTestName, revision=$revision, isPromo=$isPromo, variationId=$variationId, products=$products, customPayloadString=$customPayloadString, customPayload=$customPayload, visualPaywall=$visualPaywall)"
    }
}