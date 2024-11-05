package com.adapty.ui.listeners

import android.content.Context
import com.adapty.models.AdaptyPaywall
import com.adapty.models.AdaptyPaywallProduct

/**
 * If you use Adapty in [Observer mode](https://adapty.io/docs/observer-vs-full-mode),
 * implement this interface to handle purchases on your own.
 */
public fun interface AdaptyUiObserverModeHandler {

    /**
     * This callback is invoked when the user initiates a purchase.
     * You can trigger your custom purchase flow in response to this callback, [read more](https://adapty.io/docs/android-present-paywall-builder-paywalls-in-observer-mode).
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
     *
     * @param[paywall] An [AdaptyPaywall] within which the purchase is initiated.
     *
     * @param[context] A UI [Context] within which the the purchase is initiated.
     *
     * @param[onStartPurchase] A [PurchaseStartCallback] that should be invoked to notify AdaptyUI
     * that the purchase is started.
     *
     * From Kotlin:
     *
     * ```Kotlin
     * onStartPurchase()
     * ```
     *
     * From Java:
     *
     * ```Java
     * onStartPurchase.invoke()
     * ```
     *
     * @param[onFinishPurchase] A [PurchaseFinishCallback] that should be invoked to notify AdaptyUI
     * that the purchase is finished successfully or not, or the purchase is canceled.
     *
     * From Kotlin:
     *
     * ```Kotlin
     * onFinishPurchase()
     * ```
     *
     * From Java:
     *
     * ```Java
     * onFinishPurchase.invoke()
     * ```
     */
    public fun onPurchaseInitiated(
        product: AdaptyPaywallProduct,
        paywall: AdaptyPaywall,
        context: Context,
        onStartPurchase: PurchaseStartCallback,
        onFinishPurchase: PurchaseFinishCallback,
    )

    public fun interface PurchaseStartCallback {
        /**
         * This method should be called to notify AdaptyUI that the purchase is started.
         */
        public operator fun invoke()
    }

    public fun interface PurchaseFinishCallback {
        /**
         * This method should be called to notify AdaptyUI that the purchase is finished successfully or not,
         * or the purchase is canceled.
         */
        public operator fun invoke()
    }
}