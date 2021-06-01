package com.adapty.listeners

import com.adapty.models.PurchaserInfoModel

interface OnPurchaserInfoUpdatedListener {
    fun onPurchaserInfoReceived(purchaserInfo: PurchaserInfoModel)
}