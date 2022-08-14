package com.adapty.listeners

import com.adapty.errors.AdaptyError
import com.adapty.models.GoogleValidationResult
import com.adapty.models.ProductModel
import com.adapty.models.PurchaserInfoModel
import com.adapty.visual.VisualPaywallActivity

@Deprecated("The functionality is deprecated and will be removed in future releases.")
public interface VisualPaywallListener {

    public fun onPurchased(
        purchaserInfo: PurchaserInfoModel?,
        purchaseToken: String?,
        googleValidationResult: GoogleValidationResult?,
        product: ProductModel,
        modalActivity: VisualPaywallActivity?
    )

    public fun onPurchaseFailure(
        product: ProductModel,
        error: AdaptyError,
        modalActivity: VisualPaywallActivity?
    )

    public fun onRestorePurchases(
        purchaserInfo: PurchaserInfoModel?,
        googleValidationResultList: List<GoogleValidationResult>?,
        error: AdaptyError?,
        modalActivity: VisualPaywallActivity?
    )

    public fun onCancel(modalActivity: VisualPaywallActivity?)
}