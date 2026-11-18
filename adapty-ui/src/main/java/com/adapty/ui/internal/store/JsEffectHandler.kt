@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.script.ActionHandler
import com.adapty.ui.internal.script.JsLiteralWriter
import com.adapty.ui.internal.script.SDKGlobals
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.script.toJsScreenInstance
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class JsEffectHandler(
    private val scope: CoroutineScope,
    private val currentStateHandler: () -> StateHandler?,
    private val currentActionHandler: () -> ActionHandler?,
    private val flowConfiguration: () -> AdaptyUI.FlowConfiguration?,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        val stateHandler = currentStateHandler() ?: return
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
                if (json == "{}") return@launch
                stateHandler.updateSDKProducts(json)
            }
            is Effect.SendSDKEvent -> scope.launch {
                val event = effect.jsEventFields()
                if (effect is Effect.SendSDKEvent.AppMessage && !ownsJsState(stateHandler)) {
                    log(WARN) { "$LOG_PREFIX ${event["name"]} ${effect.id} dropped: the flow no longer owns the JS state" }
                    return@launch
                }
                stateHandler.sendSDKEvent(JsLiteralWriter.writeEvent(event))
            }
            is Effect.ClearActionHandler -> {
                currentActionHandler()?.let(stateHandler::releaseActionHandler)
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
                @Suppress("UNCHECKED_CAST")
                Message.JSCallback.CustomAction(userCustomId, params["arguments"] as? Map<String, Any?>)
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
                val contextPath = (params["contextPath"] as? String)?.takeIf { it.isNotEmpty() }
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
                val events = params["events"] as? List<*> ?: return null
                if (events.isEmpty() || events.any { it !is String }) return null
                Message.JSCallback.SendEvents(params["instanceId"] as? String, events.filterIsInstance<String>())
            }
            "SDK.showAppRate" -> Message.JSCallback.ShowAppRate
            else -> null
        }
    }

    private fun ownsJsState(stateHandler: StateHandler): Boolean {
        val owner = flowConfiguration() ?: return false
        return stateHandler.stateOwner === owner
    }
}

internal fun Effect.SendSDKEvent.jsEventFields(): Map<String, Any?> = when (val effect = this) {
    is Effect.SendSDKEvent.ProductsLoaded ->
        mapOf("id" to effect.id, "name" to "productsLoaded")
    is Effect.SendSDKEvent.WillPurchase ->
        mapOf("id" to effect.id, "name" to "willPurchase", "productId" to effect.productId)
    is Effect.SendSDKEvent.DidPurchase ->
        mapOf("id" to effect.id, "name" to "didPurchase", "productId" to effect.productId, "result" to effect.result)
    is Effect.SendSDKEvent.WillRestorePurchases ->
        mapOf("id" to effect.id, "name" to "willRestorePurchases")
    is Effect.SendSDKEvent.DidRestorePurchases ->
        mapOf("id" to effect.id, "name" to "didRestorePurchases", "result" to effect.result)
    is Effect.SendSDKEvent.AppMessage -> when (val source = effect.source) {
        is Effect.SendSDKEvent.AppMessage.Source.Application ->
            mapOf("id" to effect.id, "name" to "applicationMsg", "payload" to effect.payload)
        is Effect.SendSDKEvent.AppMessage.Source.CustomElement -> mapOf(
            "id" to effect.id,
            "name" to "customElementMsg",
            "screen" to source.screen.toJsScreenInstance(),
            "element" to mapOf("id" to source.customId, "type" to source.customType),
            "payload" to effect.payload,
        )
    }
}
