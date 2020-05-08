package com.adapty.api.entity.purchaserInfo

import com.adapty.api.entity.purchaserInfo.model.PurchaserInfoModel

interface OnPurchaserInfoUpdatedListener {
    fun didReceiveUpdatedPurchaserInfo(purchaserInfo: PurchaserInfoModel)
}