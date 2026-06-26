@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.listeners

import android.content.Context
import android.net.Uri
import com.adapty.errors.AdaptyError
import com.adapty.internal.data.cloud.BrowserLauncher
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public open class AdaptyFlowDefaultEventListener : AdaptyFlowEventListener {

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

    override fun onActionPerformed(action: AdaptyUI.Action, context: Context) {
        when (action) {
            AdaptyUI.Action.Close -> context.getActivityOrNull()?.onBackPressed()
            is AdaptyUI.Action.OpenUrl -> {
                runCatching {
                    Dependencies.injectInternal<BrowserLauncher>()
                        .openUrl(context, Uri.parse(action.url), action.presentation)
                }.getOrElse { e ->
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't process this url (${action.url}): (${e.localizedMessage})" }
                }
            }
            is AdaptyUI.Action.Custom -> Unit
        }
    }

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

    override fun onLoadingProductsFailure(
        error: AdaptyError,
        context: Context,
    ): Boolean = false

    override fun onFlowClosed() {}

    override fun onFlowShown(context: Context) {}

    override fun onProductSelected(
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    override fun onPurchaseFailure(
        error: AdaptyError,
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    override fun onPurchaseStarted(
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    override fun onPurchaseFinished(
        purchaseResult: AdaptyPurchaseResult,
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    override fun onError(
        error: AdaptyError,
        context: Context,
    ) {}

    override fun onRestoreFailure(
        error: AdaptyError,
        context: Context,
    ) {}

    override fun onRestoreStarted(context: Context)  {}

    override fun onRestoreSuccess(
        profile: AdaptyProfile,
        context: Context,
    ) {}
}