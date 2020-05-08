package com.adapty.api.entity.purchaserInfo

interface AdaptyListener {
    fun didReceiveUpdatedPurchaserInfo(purchaserInfo: AttributePurchaserInfoRes)
}