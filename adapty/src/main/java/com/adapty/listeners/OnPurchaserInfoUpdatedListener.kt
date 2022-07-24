package com.adapty.listeners

import com.adapty.models.PurchaserInfoModel

public interface OnPurchaserInfoUpdatedListener {
    public fun onPurchaserInfoReceived(purchaserInfo: PurchaserInfoModel)
}