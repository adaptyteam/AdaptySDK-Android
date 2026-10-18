package com.adapty.ui.listeners

import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.platform.AdaptyError
import com.adapty.ui.internal.platform.AdaptyPaywallProduct
import com.adapty.ui.internal.platform.AdaptyProfile
import com.adapty.ui.internal.platform.AdaptyPurchaseParameters
import com.adapty.ui.internal.platform.AdaptyPurchaseResult
import com.adapty.ui.internal.platform.Context

/**
 * Platform-neutral AdaptyFlowEventListener whose methods do nothing.
 *
 * Unlike the Android default listener, it doesn't close the host, open urls or show the
 * app-rate prompt. onBackPressed consumes the press, onAwaitingPurchaseParams answers with
 * AdaptyPurchaseParameters.Empty and onLoadingProductsFailure doesn't retry.
 */
public open class AdaptyFlowEventListenerBase : AdaptyFlowEventListener {

    override fun onActionPerformed(action: AdaptyUI.Action, context: Context) {}

    override fun onBackPressed(context: Context): Boolean = true

    override fun onAwaitingPurchaseParams(
        product: AdaptyPaywallProduct,
        context: Context,
        onPurchaseParamsReceived: AdaptyFlowEventListener.PurchaseParamsCallback,
    ): AdaptyFlowEventListener.PurchaseParamsCallback.IveBeenInvoked {
        onPurchaseParamsReceived(AdaptyPurchaseParameters.Empty)
        return AdaptyFlowEventListener.PurchaseParamsCallback.IveBeenInvoked
    }

    override fun onFinishWebPaymentNavigation(
        product: AdaptyPaywallProduct?,
        error: AdaptyError?,
        context: Context,
    ) {}

    override fun onLoadingProductsFailure(error: AdaptyError, context: Context): Boolean = false

    override fun onFlowClosed() {}

    override fun onFlowShown(context: Context) {}

    override fun onProductSelected(product: AdaptyPaywallProduct, context: Context) {}

    override fun onPurchaseFailure(error: AdaptyError, product: AdaptyPaywallProduct, context: Context) {}

    override fun onPurchaseStarted(product: AdaptyPaywallProduct, context: Context) {}

    override fun onPurchaseFinished(
        purchaseResult: AdaptyPurchaseResult,
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    override fun onError(error: AdaptyError, context: Context) {}

    override fun onRestoreFailure(error: AdaptyError, context: Context) {}

    override fun onRestoreStarted(context: Context) {}

    override fun onRestoreSuccess(profile: AdaptyProfile, context: Context) {}

    override fun onAnalyticEvent(name: String, params: Map<String, Any?>, context: Context) {}

    override fun onShowAppRate(context: Context) {}

    override fun onShowRequestPermission(
        permission: String?,
        customArgs: Map<String, String>?,
        callback: AdaptyFlowEventListener.PermissionCallback,
        context: Context,
    ) {
        callback.invoke(false, null)
    }
}
