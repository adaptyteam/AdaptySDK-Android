package com.adapty.ui.listeners

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchasedInfo
import com.adapty.models.AdaptySubscriptionUpdateParameters
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

public open class AdaptyUiDefaultEventListener : AdaptyUiEventListener {

    override fun onActionPerformed(action: AdaptyUI.Action, context: Context) {
        when (action) {
            AdaptyUI.Action.Close -> (context as? Activity)?.onBackPressed()
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
    ): AdaptySubscriptionUpdateParameters? {
        return null
    }

    public override fun onLoadingProductsFailure(
        error: AdaptyError,
        context: Context,
    ): Boolean = false

    override fun onProductSelected(
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    public override fun onPurchaseCanceled(
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    public override fun onPurchaseFailure(
        error: AdaptyError,
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    override fun onPurchaseStarted(
        product: AdaptyPaywallProduct,
        context: Context,
    ) {}

    public override fun onPurchaseSuccess(
        purchasedInfo: AdaptyPurchasedInfo?,
        product: AdaptyPaywallProduct,
        context: Context,
    ) {
        (context as? Activity)?.onBackPressed()
    }

    public override fun onRenderingError(
        error: AdaptyError,
        context: Context,
    ) {}

    public override fun onRestoreFailure(
        error: AdaptyError,
        context: Context,
    ) {}

    override fun onRestoreStarted(context: Context)  {}

    public override fun onRestoreSuccess(
        profile: AdaptyProfile,
        context: Context,
    ) {}
}