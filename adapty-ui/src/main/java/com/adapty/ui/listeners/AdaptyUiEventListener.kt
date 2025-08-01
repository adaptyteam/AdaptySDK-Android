package com.adapty.ui.listeners

import android.app.Activity
import android.content.Context
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyPaywallView
import com.adapty.ui.AdaptyUI

/**
 * Implement this interface to respond to different events happening inside the purchase screen.
 */
public interface AdaptyUiEventListener {

    /**
     * This callback is invoked when user interacts with some widgets on the paywall.
     *
     * If the user presses the "Terms" or "Privacy Policy" buttons, action [OpenUrl][AdaptyUI.Action.OpenUrl] will be invoked.
     * The [default][AdaptyUiDefaultEventListener.onActionPerformed] implementation shows a chooser
     * with apps that can open the link.
     *
     * If the user presses the *close* button, action [Close][AdaptyUI.Action.Close] will be invoked.
     * The [default][AdaptyUiDefaultEventListener.onActionPerformed] implementation is simply
     * imitating pressing the system back button.
     *
     * Note: this callback is *not* invoked when user presses the system back button
     * instead of the *close* icon on the screen.
     *
     * If a button has a custom action, action [Custom][AdaptyUI.Action.Custom] will be invoked.
     *
     * @param[action] An [Action][AdaptyUI.Action] object representing the action.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onActionPerformed(action: AdaptyUI.Action, context: Context)

    /**
     * This callback is invoked when user initiates the purchase process, providing the ability
     * to supply an [AdaptyPurchaseParameters] object with additional purchase options.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     *
     * @param[onPurchaseParamsReceived] Call `onPurchaseParamsReceived(...)` either with [AdaptyPurchaseParameters]
     * instance if you want to provide additional purchase options — e.g., if the new subscription should replace a currently active subscription
     * or if you want to indicate whether the price is personalized ([read more](https://developer.android.com/google/play/billing/integrate#personalized-price)) —
     * or with [AdaptyPurchaseParameters.Empty] as the default value.
     */
    public fun onAwaitingPurchaseParams(
        product: AdaptyPaywallProduct,
        context: Context,
        onPurchaseParamsReceived: PurchaseParamsCallback,
    ): PurchaseParamsCallback.IveBeenInvoked

    /**
     * This callback is invoked in case of errors during the products loading process.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[context] A UI [Context] within which the event occurred.
     *
     * @return `true`, if you want to retry products fetching.
     * The [default][AdaptyUiDefaultEventListener.onLoadingProductsFailure] implementation returns `false`.
     */
    public fun onLoadingProductsFailure(
        error: AdaptyError,
        context: Context,
    ): Boolean

    /**
     * This callback is invoked when the paywall view was dismissed.
     */
    public fun onPaywallClosed()

    /**
     * This callback is invoked when the paywall view was presented.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onPaywallShown(context: Context)

    /**
     * This callback is invoked when a product was selected for purchase (by user or by system).
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onProductSelected(
        product: AdaptyPaywallProduct,
        context: Context,
    )

    /**
     * This callback is invoked when the purchase process fails.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onPurchaseFailure(
        error: AdaptyError,
        product: AdaptyPaywallProduct,
        context: Context,
    )

    /**
     * This callback is invoked when user initiates the purchase process.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onPurchaseStarted(
        product: AdaptyPaywallProduct,
        context: Context,
    )

    /**
     * This callback is invoked to inform on a canceled, successful, and pending purchase.
     *
     * The [default][AdaptyUiDefaultEventListener.onPurchaseFinished] implementation is simply
     * calling [onBackPressed][Activity.onBackPressed] method of
     * the [Activity] the [AdaptyPaywallView] is attached to.
     *
     * @param[purchaseResult] An [AdaptyPurchaseResult] object containing details about the purchase.
     * If the result is [AdaptyPurchaseResult.Success], it also includes the user's profile.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onPurchaseFinished(
        purchaseResult: AdaptyPurchaseResult,
        product: AdaptyPaywallProduct,
        context: Context,
    )

    /**
     * This callback is invoked in case of errors during the screen rendering process.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onRenderingError(
        error: AdaptyError,
        context: Context,
    )

    /**
     * This callback is invoked when the restore process fails.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onRestoreFailure(
        error: AdaptyError,
        context: Context,
    )

    /**
     * This callback is invoked when user initiates the restore process.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onRestoreStarted(
        context: Context,
    )

    /**
     * This callback is invoked when a successful restore is made.
     *
     * Check if the [AdaptyProfile] object contains the desired access level, and if so,
     * you can navigate back from the paywall.
     *
     * @param[profile] An [AdaptyProfile] object containing up to date information about the user.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onRestoreSuccess(
        profile: AdaptyProfile,
        context: Context,
    )

    public fun interface PurchaseParamsCallback {
        public operator fun invoke(purchaseParams: AdaptyPurchaseParameters)

        public companion object IveBeenInvoked
    }
}