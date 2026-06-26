package com.adapty.ui.listeners

import android.content.Context
import com.adapty.errors.AdaptyError
import com.adapty.models.AdaptyPaywallProduct
import com.adapty.models.AdaptyProfile
import com.adapty.models.AdaptyPurchaseParameters
import com.adapty.models.AdaptyPurchaseResult
import com.adapty.ui.AdaptyUI

/**
 * Implement this interface to respond to different events happening inside the purchase screen.
 */
public interface AdaptyFlowEventListener {

    /**
     * This callback is invoked when user interacts with some widgets on the flow.
     *
     * If the user presses the "Terms" or "Privacy Policy" buttons, action [OpenUrl][AdaptyUI.Action.OpenUrl] will be invoked.
     * The [default][AdaptyFlowDefaultEventListener.onActionPerformed] implementation shows a chooser
     * with apps that can open the link.
     *
     * If the user presses the *close* button, action [Close][AdaptyUI.Action.Close] will be invoked.
     * The [default][AdaptyFlowDefaultEventListener.onActionPerformed] implementation is simply
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
     * This callback is invoked when the user presses the system back button (or performs the
     * back gesture) while the flow is shown, *and* no `on_device_back` action is configured
     * for the current screen or its navigator. A configured `on_device_back` takes precedence
     * and is handled internally without invoking this callback.
     *
     * The [default][AdaptyFlowDefaultEventListener.onBackPressed] implementation is a no-op that
     * consumes the press (returns `true`), so by default the system back button does nothing —
     * matching iOS, where a flow cannot be dismissed by a system gesture. Provide an explicit
     * dismissal path (a *close* button or an `on_device_back` action), or return `false` here, if
     * the user should be able to leave the flow with back.
     *
     * @param[context] A UI [Context] within which the event occurred.
     *
     * @return `true` if you handled the back press (it will be consumed); `false` to let the
     * host's own back handling run (e.g. finishing the activity / popping the fragment).
     */
    public fun onBackPressed(context: Context): Boolean

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
     * This callback is invoked after attempting to navigate to the web payment url for a purchase.
     *
     * The [product] and [error] parameters can be in one of three states:
     * - If the url was handled by a browser, and the web payment was for a specific product: [product] is not `null`, [error] is `null`.
     * - If the url was handled by a browser, and the web payment was for a paywall without specifying a product: both [product] and [error] are `null`.
     * - Otherwise: [product] is `null`, [error] is not `null`.
     *
     * @param[product] An [AdaptyPaywallProduct] associated with the web payment, or `null`.
     *
     * @param[error] An [AdaptyError] object representing the error, or `null`.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onFinishWebPaymentNavigation(
        product: AdaptyPaywallProduct?,
        error: AdaptyError?,
        context: Context,
    )

    /**
     * This callback is invoked in case of errors during the products loading process.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[context] A UI [Context] within which the event occurred.
     *
     * @return `true`, if you want to retry products fetching.
     * The [default][AdaptyFlowDefaultEventListener.onLoadingProductsFailure] implementation returns `false`.
     */
    public fun onLoadingProductsFailure(
        error: AdaptyError,
        context: Context,
    ): Boolean

    /**
     * This callback is invoked when the flow view was dismissed.
     */
    public fun onFlowClosed()

    /**
     * This callback is invoked when the flow view was presented.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onFlowShown(context: Context)

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
     * The [default][AdaptyFlowDefaultEventListener.onPurchaseFinished] implementation is a no-op.
     * Check the [purchaseResult] and navigate back from the flow yourself if needed.
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
     * This callback is invoked in case of errors that are not tied to a specific
     * purchase or restore flow, e.g. errors during the screen rendering process.
     *
     * @param[error] An [AdaptyError] object representing the error.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onError(
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
     * you can navigate back from the flow.
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

    /**
     * This callback is invoked when the flow reports a custom analytics event intended for
     * your own analytics. Events intended only for Adapty analytics are not delivered here.
     *
     * The [default][AdaptyFlowDefaultEventListener.onAnalyticEvent] implementation is a no-op —
     * override it to forward the event to your own analytics pipeline.
     *
     * @param[name] The name of the analytics event.
     *
     * @param[params] Additional parameters associated with the event.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onAnalyticEvent(
        name: String,
        params: Map<String, Any?>,
        context: Context,
    )

    /**
     * This callback is invoked when the flow requests an app-rating prompt.
     *
     * The [default][AdaptyFlowDefaultEventListener.onShowAppRate] implementation is a no-op —
     * override it to present an app-rating prompt (e.g. via the Play In-App Review `ReviewManager`).
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onShowAppRate(context: Context)

    /**
     * This callback is invoked when the flow requests a runtime permission.
     *
     * Override it to request the named runtime permission (or otherwise resolve it) and invoke
     * [callback] with the result. The [default][AdaptyFlowDefaultEventListener.onShowRequestPermission]
     * implementation invokes [callback] with `granted = false`.
     *
     * @param[permission] The name of the requested runtime permission, or `null`.
     *
     * @param[customArgs] Additional arguments associated with the request, or `null`.
     *
     * @param[callback] Invoke this with the result of the permission request.
     *
     * @param[context] A UI [Context] within which the event occurred.
     */
    public fun onShowRequestPermission(
        permission: String?,
        customArgs: Map<String, String>?,
        callback: PermissionCallback,
        context: Context,
    )

    public fun interface PermissionCallback {
        public operator fun invoke(granted: Boolean, detailResult: String?)
    }
}