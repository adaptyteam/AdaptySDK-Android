@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import android.app.Activity
import com.adapty.Adapty
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.models.AdaptyWebPresentation
import com.adapty.ui.internal.listeners.ContextAwareEventListener
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyResult
import com.adapty.utils.ErrorCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PurchaseEffectHandler(
    private val scope: CoroutineScope,
    private val flowKey: String,
    private val activityProvider: () -> Activity?,
    private val listenerProvider: () -> ContextAwareEventListener?,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.AwaitPurchaseParams -> {
                val listener = listenerProvider() ?: return
                listener.onAwaitingPurchaseParams(effect.product, listener.context) { params ->
                    scope.launch {
                        dispatch(Message.PurchaseParamsReceived(params, effect.product))
                    }
                }
            }
            is Effect.StartPurchaseFlow -> {
                val activity = activityProvider() ?: return
                log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase begin" }
                activity.runOnUiThread {
                    Adapty.makePurchase(activity, effect.product, effect.params) { result ->
                        scope.launch {
                            dispatch(when (result) {
                                is AdaptyResult.Success -> {
                                    log(VERBOSE) { "$LOG_PREFIX $flowKey makePurchase success" }
                                    Message.PurchaseSucceeded(result.value, effect.product)
                                }
                                is AdaptyResult.Error -> {
                                    log(ERROR) { "$LOG_PREFIX_ERROR $flowKey makePurchase error: ${result.error.message}" }
                                    Message.PurchaseFailed(result.error, effect.product)
                                }
                            })
                        }
                    }
                }
            }
            is Effect.StartWebPurchase -> {
                val activity = activityProvider() ?: return
                log(VERBOSE) { "$LOG_PREFIX $flowKey onWebPurchaseInitiated" }
                val presentation = when (effect.openIn) {
                    "browser_in_app" -> AdaptyWebPresentation.InAppBrowser
                    else -> AdaptyWebPresentation.ExternalBrowser
                }
                val listener = listenerProvider()
                val callback = ErrorCallback { error ->
                    if (error != null) {
                        log(ERROR) { "$LOG_PREFIX_ERROR $flowKey openWebPaywall error: ${error.message}" }
                        listener?.onFinishWebPaymentNavigation(null, error, listener.context)
                    } else {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey openWebPaywall success" }
                        listener?.onFinishWebPaymentNavigation(effect.product, null, listener.context)
                    }
                }
                Adapty.openWebPaywall(activity, effect.product, presentation, callback)
            }
            is Effect.StartRestoreFlow -> {
                log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases begin" }
                Adapty.restorePurchases { result ->
                    scope.launch {
                        dispatch(when (result) {
                            is AdaptyResult.Success -> {
                                log(VERBOSE) { "$LOG_PREFIX $flowKey restorePurchases success" }
                                Message.RestoreSucceeded(result.value)
                            }
                            is AdaptyResult.Error -> {
                                log(ERROR) { "$LOG_PREFIX_ERROR $flowKey restorePurchases error: ${result.error.message}" }
                                Message.RestoreFailed(result.error)
                            }
                        })
                    }
                }
            }
            else -> return
        }
    }
}
