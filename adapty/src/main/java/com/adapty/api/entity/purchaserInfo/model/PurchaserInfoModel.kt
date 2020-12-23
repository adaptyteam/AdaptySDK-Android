package com.adapty.api.entity.purchaserInfo.model

data class PurchaserInfoModel(
    var customerUserId: String?,
    var accessLevels: Map<String, AccessLevelInfoModel>?,
    var subscriptions: Map<String, SubscriptionInfoModel>?,
    var nonSubscriptions: Map<String, List<NonSubscriptionInfoModel>>?,
    var customAttributes: Map<String, Any>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PurchaserInfoModel

        if (customerUserId != other.customerUserId) return false
        if (accessLevels != other.accessLevels) return false
        if (subscriptions != other.subscriptions) return false
        if (nonSubscriptions != other.nonSubscriptions) return false
        if (customAttributes?.takeIf { it.isNotEmpty() } != other.customAttributes?.takeIf { it.isNotEmpty() }) return false

        return true
    }

    override fun hashCode(): Int {
        var result = customerUserId?.hashCode() ?: 0
        result = 31 * result + (accessLevels?.hashCode() ?: 0)
        result = 31 * result + (subscriptions?.hashCode() ?: 0)
        result = 31 * result + (nonSubscriptions?.hashCode() ?: 0)
        result = 31 * result + (customAttributes?.takeIf { it.isNotEmpty() }?.hashCode() ?: 0)
        return result
    }
}