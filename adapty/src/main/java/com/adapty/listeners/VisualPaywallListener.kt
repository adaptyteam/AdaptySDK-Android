package com.adapty.listeners

import com.adapty.errors.AdaptyError
import com.adapty.models.GoogleValidationResult
import com.adapty.models.ProductModel
import com.adapty.models.PurchaserInfoModel

interface VisualPaywallListener {

    fun onPurchased(
        purchaserInfo: PurchaserInfoModel?,
        purchaseToken: String?,
        googleValidationResult: GoogleValidationResult?,
        product: ProductModel
    )

    fun onPurchaseFailure(product: ProductModel, error: AdaptyError)

    fun onRestorePurchases(
        purchaserInfo: PurchaserInfoModel?,
        googleValidationResultList: List<GoogleValidationResult>?,
        error: AdaptyError?
    )

    fun onClosed()
}