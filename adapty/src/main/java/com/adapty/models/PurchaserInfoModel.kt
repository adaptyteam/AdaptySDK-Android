package com.adapty.models

public class PurchaserInfoModel(
    public val profileId: String,
    public val customerUserId: String?,
    public val accessLevels: Map<String, AccessLevelInfoModel>,
    public val subscriptions: Map<String, SubscriptionInfoModel>,
    public val nonSubscriptions: Map<String, List<NonSubscriptionInfoModel>>,
    public val customAttributes: Map<String, Any>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchaserInfoModel

        if (profileId != other.profileId) return false
        if (customerUserId != other.customerUserId) return false
        if (accessLevels != other.accessLevels) return false
        if (subscriptions != other.subscriptions) return false
        if (nonSubscriptions != other.nonSubscriptions) return false
        if (customAttributes != other.customAttributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = profileId.hashCode()
        result = 31 * result + (customerUserId?.hashCode() ?: 0)
        result = 31 * result + accessLevels.hashCode()
        result = 31 * result + subscriptions.hashCode()
        result = 31 * result + nonSubscriptions.hashCode()
        result = 31 * result + customAttributes.hashCode()
        return result
    }

    override fun toString(): String {
        return "PurchaserInfoModel(profileId=$profileId, customerUserId=$customerUserId, accessLevels=$accessLevels, subscriptions=$subscriptions, nonSubscriptions=$nonSubscriptions, customAttributes=$customAttributes)"
    }
}