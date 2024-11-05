package com.adapty.ui.listeners

import android.app.Activity
import android.content.Context
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchasedInfo
import com.adapty.models.AdaptySubscriptionUpdateParameters
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
     * to supply an [AdaptySubscriptionUpdateParameters] object.
     * This is used when you need one subscription to be replaced with another.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     *
     * @return An [AdaptySubscriptionUpdateParameters] object, used when
     * you need a subscription to be replaced with another one, otherwise `null`, [read more](https://adapty.io/docs/making-purchases#change-subscription-when-making-a-purchase).
     */
    public fun onAwaitingSubscriptionUpdateParams(
        product: AdaptyPaywallProduct,
        context: Context,
    ): AdaptySubscriptionUpdateParameters?

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
     * This callback is invoked when user cancels the purchase manually.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onPurchaseCanceled(product: AdaptyPaywallProduct, context: Context)

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
     * This callback is invoked when a successful purchase is made.
     *
     * The [default][AdaptyUiDefaultEventListener.onPurchaseSuccess] implementation is simply
     * calling [onBackPressed][Activity.onBackPressed] method of
     * the [Activity] the [AdaptyPaywallView] is attached to.
     *
     * @param[purchasedInfo] An [AdaptyPurchasedInfo] object containing up to date information
     * about the purchase and the user's profile.
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onPurchaseSuccess(
        purchasedInfo: AdaptyPurchasedInfo?,
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
}