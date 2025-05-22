@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.listeners

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adapty.errors.AdaptyError
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.getActivityOrNull
import com.adapty.ui.internal.utils.log
import com.adapty.ui.listeners.AdaptyUiEventListener.SubscriptionUpdateParamsCallback
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public open class AdaptyUiDefaultEventListener : AdaptyUiEventListener {

    override fun onActionPerformed(action: AdaptyUI.Action, context: Context) {
        when (action) {
            AdaptyUI.Action.Close -> context.getActivityOrNull()?.onBackPressed()
            is AdaptyUI.Action.OpenUrl -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
                try {
                    context.startActivity(Intent.createChooser(intent, ""))
                } catch (e: Throwable) {
                    log(ERROR) { "$LOG_PREFIX_ERROR couldn't find an app that can process this url" }
                }
            }
            is AdaptyUI.Action.Custom -> Unit
        }
    }

    override fun onAwaitingSubscriptionUpdateParams(
        product: AdaptyPaywallProduct,
        context: Context,
        onSubscriptionUpdateParamsReceived: SubscriptionUpdateParamsCallback,
    ) {
        onSubscriptionUpdateParamsReceived(null)
    }

    override fun onLoadingProductsFailure(
        error: AdaptyError,
        context: Context,
    ): Boolean = false

    override fun onPaywallClosed() {}

    override fun onPaywallShown(context: Context) {}

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
    ) {
        if (purchaseResult !is AdaptyPurchaseResult.UserCanceled)
            context.getActivityOrNull()?.onBackPressed()
    }

    override fun onRenderingError(
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