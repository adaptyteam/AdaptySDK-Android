@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ObserverModeEffectHandler(
    private val scope: CoroutineScope,
    private val flowKey: String,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.InitiateObserverModePurchase -> {
                log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onPurchaseInitiated begin" }
                effect.handler.onPurchaseInitiated(
                    effect.product,
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onStartPurchase called" }
                        scope.launch { dispatch(Message.ObserverPurchaseStarted(effect.product)) }
                    },
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onFinishPurchase called" }
                        scope.launch { dispatch(Message.ObserverPurchaseFinished(effect.product)) }
                    },
                )
            }
            is Effect.InitiateObserverModeRestore -> {
                log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onRestoreInitiated begin" }
                effect.restoreHandler.onRestoreInitiated(
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onStartRestore called" }
                        scope.launch { dispatch(Message.ObserverRestoreStarted) }
                    },
                    {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey observerModeHandler: onFinishRestore called" }
                        scope.launch { dispatch(Message.ObserverRestoreFinished) }
                    },
                )
            }
            is Effect.ObserverModeWarning -> {
                log(effect.level) { "$LOG_PREFIX $flowKey ${effect.message}" }
            }
            else -> return
        }
    }
}
