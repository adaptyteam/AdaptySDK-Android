@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.SDKGlobals
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class JsEffectHandler(
    private val scope: CoroutineScope,
    private val stateHandler: StateHandler,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.ExecuteJSActions -> {
                val jsActions = mutableListOf<com.adapty.ui.internal.ui.element.Action>()
                for (action in effect.actions) {
                    val shortCircuited = if (action.func.startsWith("SDK.") && action.scope == Scope.Global) {
                        sdkActionToJSCallback(action.func, action.params)
                    } else null

                    if (shortCircuited != null) {
                        dispatch(shortCircuited)
                    } else {
                        jsActions.add(action)
                    }
                }
                if (jsActions.isNotEmpty()) {
                    scope.launch {
                        jsActions.forEach { action -> stateHandler.executeAction(action.func, action.params, action.scope, effect.screen) }
                    }
                }
            }
            is Effect.SetJSValue -> scope.launch {
                stateHandler.setValue(effect.binding, effect.value, effect.screen)
            }
            is Effect.RefreshStateCache -> scope.launch {
                stateHandler.refreshState()
            }
            is Effect.InvokeTimerCallback -> scope.launch {
                stateHandler.invokeTimerCallback(effect.timerId)
            }
            is Effect.InvokeJSPurchaseCallback -> scope.launch {
                stateHandler.invokePurchaseCallback(
                    effect.callbackId,
                    mapOf("productId" to effect.productId, "result" to effect.result),
                )
            }
            is Effect.InvokeJSRestoreCallback -> scope.launch {
                stateHandler.invokeRestoreCallback(
                    effect.callbackId,
                    mapOf("result" to effect.result),
                )
            }
            is Effect.InvokeJSAlertCallback -> scope.launch {
                stateHandler.invokeAlertCallback(effect.callbackId, mapOf("actionId" to effect.actionId))
            }
            is Effect.InvokeJSPermissionCallback -> scope.launch {
                val response = buildMap<String, Any?> {
                    put("permission", effect.permission)
                    if (effect.customArgs != null) put("customArgs", effect.customArgs)
                    put("result", effect.granted)
                    put("detailResult", effect.detailResult)
                }
                stateHandler.invokePermissionCallback(effect.callbackId, response)
            }
            is Effect.UpdateJSProducts -> scope.launch {
                val json = SDKGlobals.buildSDKProductsJson(effect.products)
                stateHandler.updateSDKProducts(json)
            }
            is Effect.SendSDKEvent -> scope.launch {
                val eventJson = when (effect) {
                    is Effect.SendSDKEvent.ProductsLoaded ->
                        """{"name":"productsLoaded"}"""
                    is Effect.SendSDKEvent.WillPurchase ->
                        """{"name":"willPurchase","productId":${jsonString(effect.productId)}}"""
                    is Effect.SendSDKEvent.DidPurchase ->
                        """{"name":"didPurchase","productId":${jsonString(effect.productId)},"result":${jsonString(effect.result)}}"""
                    is Effect.SendSDKEvent.WillRestorePurchases ->
                        """{"name":"willRestorePurchases"}"""
                    is Effect.SendSDKEvent.DidRestorePurchases ->
                        """{"name":"didRestorePurchases","result":${jsonString(effect.result)}}"""
                }
                stateHandler.sendSDKEvent(eventJson)
            }
            is Effect.ClearActionHandler -> {
                stateHandler.setActionHandler(null)
            }
            else -> return
        }
    }

    private fun sdkActionToJSCallback(func: String, params: Map<String, Any?>): Message.JSCallback? {
        return when (func) {
            "SDK.openUrl" -> {
                val source = (params["url"] as? String)?.let { Message.JSCallback.OpenUrl.Source.Url(it) }
                    ?: (params["stringId"] as? String)?.let { Message.JSCallback.OpenUrl.Source.StringId(it) }
                    ?: return null
                val openIn = params["openIn"] as? String
                Message.JSCallback.OpenUrl(source, openIn)
            }
            "SDK.userCustomAction" -> {
                val userCustomId = params["userCustomId"] as? String ?: return null
                Message.JSCallback.CustomAction(userCustomId)
            }
            "SDK.purchaseProduct" -> {
                val productId = params["productId"] as? String ?: return null
                val paywallId = params["paywallId"] as? String
                Message.JSCallback.PurchaseProduct(productId, paywallId)
            }
            "SDK.webPurchaseProduct" -> {
                val productId = params["productId"] as? String ?: return null
                val paywallId = params["paywallId"] as? String
                val openIn = params["openIn"] as? String
                Message.JSCallback.WebPurchaseProduct(productId, paywallId, openIn)
            }
            "SDK.restorePurchases" -> Message.JSCallback.RestorePurchases()
            "SDK.closeAll" -> Message.JSCallback.CloseAll
            "SDK.onSelectProduct" -> {
                val productId = params["productId"] as? String ?: return null
                val paywallId = params["paywallId"] as? String
                Message.JSCallback.SelectProduct(productId, paywallId)
            }
            "SDK.openScreen" -> {
                val screenInstanceId = params["instanceId"] as? String ?: return null
                val screenType = params["type"] as? String ?: return null
                val contextPath = params["contextPath"] as? String
                val navigatorId = params["navigatorId"] as? String ?: "default"
                val transitionId = params["transitionId"] as? String ?: return null
                Message.JSCallback.OpenScreen(
                    NavigationEntry(screenInstanceId, screenType, contextPath, navigatorId, transitionId)
                )
            }
            "SDK.moveScroll" -> {
                val instanceId = params["instanceId"] as? String ?: return null
                val kind = params["kind"] as? String ?: return null
                val value = params["value"] as? String ?: return null
                Message.JSCallback.MoveScroll(instanceId, kind, value)
            }
            "SDK.closeScreen" -> {
                val navigatorId = params["navigatorId"] as? String ?: "default"
                val transitionId = params["transitionId"] as? String ?: "on_disappear"
                Message.JSCallback.CloseScreen(navigatorId, transitionId)
            }
            "SDK.changeFocus" -> {
                Message.JSCallback.ChangeFocus(params["id"] as? String)
            }
            "SDK.setTimer" -> {
                val timerId = params["id"] as? String ?: return null
                val endAtMs = (params["endAt"] as? Number)?.toLong()
                val durationSeconds = (params["duration"] as? Number)?.toLong()
                val behavior = params["behavior"] as? String
                Message.JSCallback.SetTimer(timerId, endAtMs, durationSeconds, behavior)
            }
            "SDK.sendAnalyticsEvent" -> {
                val name = params["name"] as? String ?: return null
                Message.JSCallback.SendAnalyticsEvent(name, params)
            }
            "SDK.sendEvents" -> {
                val events = (params["events"] as? List<*>)?.mapNotNull { it as? String } ?: return null
                if (events.isEmpty()) return null
                Message.JSCallback.SendEvents(params["instanceId"] as? String, events)
            }
            "SDK.showAppRate" -> Message.JSCallback.ShowAppRate
            else -> null
        }
    }

    private fun jsonString(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }
}
