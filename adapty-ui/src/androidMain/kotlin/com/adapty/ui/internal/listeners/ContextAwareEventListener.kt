package com.adapty.ui.internal.listeners

import android.content.Context
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI
import com.adapty.ui.listeners.AdaptyFlowEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener.PurchaseParamsCallback

internal class ContextAwareEventListener(
    private val delegate: AdaptyFlowEventListener,
    private val contextProvider: () -> Context,
) : AdaptyFlowEventListener {

    val context: Context get() = contextProvider()

    override fun onActionPerformed(action: AdaptyUI.Action, context: Context) =
        delegate.onActionPerformed(action, contextProvider())

    override fun onBackPressed(context: Context): Boolean =
        delegate.onBackPressed(contextProvider())

    override fun onAwaitingPurchaseParams(
        product: AdaptyPaywallProduct,
        context: Context,
        onPurchaseParamsReceived: PurchaseParamsCallback,
    ): PurchaseParamsCallback.IveBeenInvoked =
        delegate.onAwaitingPurchaseParams(product, contextProvider(), onPurchaseParamsReceived)

    override fun onFinishWebPaymentNavigation(
        product: AdaptyPaywallProduct?,
        error: AdaptyError?,
        context: Context,
    ) = delegate.onFinishWebPaymentNavigation(product, error, contextProvider())

    override fun onLoadingProductsFailure(error: AdaptyError, context: Context): Boolean =
        delegate.onLoadingProductsFailure(error, contextProvider())

    override fun onFlowClosed() =
        delegate.onFlowClosed()

    override fun onFlowShown(context: Context) =
        delegate.onFlowShown(contextProvider())

    override fun onProductSelected(product: AdaptyPaywallProduct, context: Context) =
        delegate.onProductSelected(product, contextProvider())

    override fun onPurchaseFailure(error: AdaptyError, product: AdaptyPaywallProduct, context: Context) =
        delegate.onPurchaseFailure(error, product, contextProvider())

    override fun onPurchaseStarted(product: AdaptyPaywallProduct, context: Context) =
        delegate.onPurchaseStarted(product, contextProvider())

    override fun onPurchaseFinished(purchaseResult: AdaptyPurchaseResult, product: AdaptyPaywallProduct, context: Context) =
        delegate.onPurchaseFinished(purchaseResult, product, contextProvider())

    override fun onError(error: AdaptyError, context: Context) =
        delegate.onError(error, contextProvider())

    override fun onRestoreFailure(error: AdaptyError, context: Context) =
        delegate.onRestoreFailure(error, contextProvider())

    override fun onRestoreStarted(context: Context) =
        delegate.onRestoreStarted(contextProvider())

    override fun onRestoreSuccess(profile: AdaptyProfile, context: Context) =
        delegate.onRestoreSuccess(profile, contextProvider())

    override fun onAnalyticEvent(name: String, params: Map<String, Any?>, context: Context) =
        delegate.onAnalyticEvent(name, params, contextProvider())

    override fun onShowAppRate(context: Context) =
        delegate.onShowAppRate(contextProvider())

    override fun onShowRequestPermission(
        permission: String?,
        customArgs: Map<String, String>?,
        callback: AdaptyFlowEventListener.PermissionCallback,
        context: Context,
    ) = delegate.onShowRequestPermission(permission, customArgs, callback, contextProvider())
}
