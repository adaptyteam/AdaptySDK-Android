package com.adapty.listeners

import com.adapty.errors.AdaptyError
import com.adapty.models.GoogleValidationResult
import com.adapty.models.ProductModel
import com.adapty.models.PurchaserInfoModel
import com.adapty.visual.VisualPaywallActivity

interface VisualPaywallListener {

    fun onPurchased(
        purchaserInfo: PurchaserInfoModel?,
        purchaseToken: String?,
        googleValidationResult: GoogleValidationResult?,
        product: ProductModel,
        modalActivity: VisualPaywallActivity?
    )

    fun onPurchaseFailure(
        product: ProductModel,
        error: AdaptyError,
        modalActivity: VisualPaywallActivity?
    )

    fun onRestorePurchases(
        purchaserInfo: PurchaserInfoModel?,
        googleValidationResultList: List<GoogleValidationResult>?,
        error: AdaptyError?,
        modalActivity: VisualPaywallActivity?
    )

    fun onCancel(modalActivity: VisualPaywallActivity?)
}