package com.adapty.models

class PurchaserInfoModel(
    val profileId: String,
    val customerUserId: String?,
    val accessLevels: Map<String, AccessLevelInfoModel>,
    val subscriptions: Map<String, SubscriptionInfoModel>,
    val nonSubscriptions: Map<String, List<NonSubscriptionInfoModel>>,
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

        return true
    }

    override fun hashCode(): Int {
        var result = profileId.hashCode()
        result = 31 * result + (customerUserId?.hashCode() ?: 0)
        result = 31 * result + accessLevels.hashCode()
        result = 31 * result + subscriptions.hashCode()
        result = 31 * result + nonSubscriptions.hashCode()
        return result
    }

    override fun toString(): String {
        return "PurchaserInfoModel(profileId=$profileId, customerUserId=$customerUserId, accessLevels=$accessLevels, subscriptions=$subscriptions, nonSubscriptions=$nonSubscriptions)"
    }
}