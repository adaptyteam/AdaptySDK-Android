@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.listeners.ContextAwareEventListener
import com.adapty.ui.listeners.AdaptyFlowEventListener

internal class ListenerEffectHandler(
    private val listenerProvider: () -> ContextAwareEventListener?,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.NotifyListener.ActionPerformed -> {
                val listener = listenerProvider() ?: return
                listener.onActionPerformed(effect.action, listener.context)
            }
            is Effect.NotifyListener.ProductSelected -> {
                val listener = listenerProvider() ?: return
                listener.onProductSelected(effect.product, listener.context)
            }
            is Effect.NotifyListener.PurchaseStarted -> {
                val listener = listenerProvider() ?: return
                listener.onPurchaseStarted(effect.product, listener.context)
            }
            is Effect.NotifyListener.PurchaseFinished -> {
                val listener = listenerProvider() ?: return
                listener.onPurchaseFinished(effect.result, effect.product, listener.context)
            }
            is Effect.NotifyListener.PurchaseFailed -> {
                val listener = listenerProvider() ?: return
                listener.onPurchaseFailure(effect.error, effect.product, listener.context)
            }
            is Effect.NotifyListener.RestoreStarted -> {
                val listener = listenerProvider() ?: return
                listener.onRestoreStarted(listener.context)
            }
            is Effect.NotifyListener.RestoreSucceeded -> {
                val listener = listenerProvider() ?: return
                listener.onRestoreSuccess(effect.profile, listener.context)
            }
            is Effect.NotifyListener.RestoreFailed -> {
                val listener = listenerProvider() ?: return
                listener.onRestoreFailure(effect.error, listener.context)
            }
            is Effect.NotifyListener.FlowShown -> {
                val listener = listenerProvider() ?: return
                listener.onFlowShown(listener.context)
            }
            is Effect.NotifyListener.FlowClosed -> {
                listenerProvider()?.onFlowClosed()
            }
            is Effect.NotifyListener.OnError -> {
                val listener = listenerProvider() ?: return
                listener.onError(effect.error, listener.context)
            }
            is Effect.NotifyListener.AnalyticEvent -> {
                val listener = listenerProvider() ?: return
                listener.onAnalyticEvent(effect.name, effect.params, listener.context)
            }
            is Effect.NotifyListener.ShowAppRate -> {
                val listener = listenerProvider() ?: return
                listener.onShowAppRate(listener.context)
            }
            is Effect.NotifyListener.ShowRequestPermission -> {
                val listener = listenerProvider() ?: return
                val callbackId = effect.callbackId
                val permission = effect.permission
                val customArgs = effect.customArgs
                val callback = AdaptyFlowEventListener.PermissionCallback { granted, detailResult ->
                    if (callbackId != null) {
                        dispatch(Message.JSPermissionCallbackInvoked(
                            callbackId = callbackId,
                            permission = permission,
                            customArgs = customArgs,
                            granted = granted,
                            detailResult = detailResult,
                        ))
                    }
                }
                listener.onShowRequestPermission(permission, customArgs, callback, listener.context)
            }
            else -> return
        }
    }
}
