package com.adapty.ui.listeners

import com.adapty.models.AdaptyPaywallProduct

/**
 * If you use Adapty in [Observer mode](https://adapty.io/docs/observer-vs-full-mode),
 * implement this interface to handle purchases and restores on your own.
 */
public interface AdaptyUiObserverModeHandler {

    /**
     * This callback is invoked when the user initiates a purchase.
     * You can trigger your custom purchase flow in response to this callback, [read more](https://adapty.io/docs/android-present-paywall-builder-paywalls-in-observer-mode).
     *
     * @param[product] An [AdaptyPaywallProduct] of the purchase.
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
        onStartPurchase: PurchaseStartCallback,
        onFinishPurchase: PurchaseFinishCallback,
    )

    /**
     * Override to provide custom restore handling.
     * Return `null` (default) to use SDK's default restore behavior.
     *
     * From Kotlin:
     *
     * ```Kotlin
     * override fun getRestoreHandler() = RestoreHandler { onStart, onFinish ->
     *     onStart()
     *     // your custom restore logic
     *     onFinish()
     * }
     * ```
     *
     * From Java:
     *
     * ```Java
     * @Override
     * public RestoreHandler getRestoreHandler() {
     *     return (onStart, onFinish) -> {
     *         onStart.invoke();
     *         // your custom restore logic
     *         onFinish.invoke();
     *     };
     * }
     * ```
     */
    public fun getRestoreHandler(): RestoreHandler? = null

    /**
     * A handler for custom restore logic in Observer Mode.
     */
    public fun interface RestoreHandler {
        /**
         * This callback is invoked when the user initiates a restore purchases.
         * You can trigger your custom restore flow in response to this callback.
         *
         * @param[onStartRestore] A [RestoreStartCallback] that should be invoked to notify AdaptyUI
         * that the restore is started.
         *
         * @param[onFinishRestore] A [RestoreFinishCallback] that should be invoked to notify AdaptyUI
         * that the restore is finished successfully or not, or the restore is canceled.
         */
        public fun onRestoreInitiated(
            onStartRestore: RestoreStartCallback,
            onFinishRestore: RestoreFinishCallback,
        )
    }

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

    public fun interface RestoreStartCallback {
        /**
         * This method should be called to notify AdaptyUI that the restore is started.
         */
        public operator fun invoke()
    }

    public fun interface RestoreFinishCallback {
        /**
         * This method should be called to notify AdaptyUI that the restore is finished successfully or not,
         * or the restore is canceled.
         */
        public operator fun invoke()
    }
}