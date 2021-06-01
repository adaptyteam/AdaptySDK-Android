package com.adapty.models

class NonSubscriptionInfoModel(
    val purchaseId: String,
    val vendorProductId: String,
    val vendorTransactionId: String?,
    val store: String,
    val purchasedAt: String?,
    val isOneTime: Boolean,
    val isSandbox: Boolean,
    val isRefund: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NonSubscriptionInfoModel

        if (purchaseId != other.purchaseId) return false
        if (vendorProductId != other.vendorProductId) return false
        if (vendorTransactionId != other.vendorTransactionId) return false
        if (store != other.store) return false
        if (purchasedAt != other.purchasedAt) return false
        if (isOneTime != other.isOneTime) return false
        if (isSandbox != other.isSandbox) return false
        if (isRefund != other.isRefund) return false

        return true
    }

    override fun hashCode(): Int {
        var result = purchaseId.hashCode()
        result = 31 * result + vendorProductId.hashCode()
        result = 31 * result + (vendorTransactionId?.hashCode() ?: 0)
        result = 31 * result + store.hashCode()
        result = 31 * result + (purchasedAt?.hashCode() ?: 0)
        result = 31 * result + isOneTime.hashCode()
        result = 31 * result + isSandbox.hashCode()
        result = 31 * result + isRefund.hashCode()
        return result
    }

    override fun toString(): String {
        return "NonSubscriptionInfoModel(purchaseId='$purchaseId', vendorProductId='$vendorProductId', vendorTransactionId=$vendorTransactionId, store='$store', purchasedAt=$purchasedAt, isOneTime=$isOneTime, isSandbox=$isSandbox, isRefund=$isRefund)"
    }
}