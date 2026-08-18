@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.SDKGlobals
import com.adapty.ui.internal.script.StateHandler
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.ui.event.EventDispatcher
import com.adapty.ui.internal.ui.event.LifecyclePhase
import com.adapty.ui.internal.ui.element.Action
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class JsEffectHandler(
    private val scope: CoroutineScope,
    private val stateHandler: StateHandler,
    private val eventDispatcher: EventDispatcher,
    private val exitFlushTimeoutMillis: Long = EXIT_FLUSH_TIMEOUT_MILLIS,
) : EffectHandler {
    private var valueWriteTail: Job? = null
    private val valueWriteParent = SupervisorJob()

    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.ExecuteJSActions -> {
                val jsActions = dispatchSdkActions(effect.actions, dispatch)
                if (jsActions.isNotEmpty()) {
                    scope.launch {
                        executeJsActions(jsActions, effect.screen)
                    }
                }
            }
            is Effect.SetJSValue -> {
                val previousWrite = valueWriteTail
                val writeJob = scope.launch(valueWriteParent) {
                    previousWrite?.join()
                    stateHandler.setValue(effect.binding, effect.value, effect.screen)
                }
                valueWriteTail = writeJob
            }
            is Effect.FlushBeforeExit -> {
                val pendingValueWrite = valueWriteTail
                scope.launch(NonCancellable) {
                    var rpcDrained = false
                    val flushCompleted = withTimeoutOrNull(exitFlushTimeoutMillis) {
                        pendingValueWrite?.join()
                        effect.focusActions.forEach { executeActions(it, dispatch) }
                        effect.willDisappearActions.forEach { actions ->
                            val screen = actions.screen
                            val willDisappearPublished = eventDispatcher
                                .lifecycleHistoryFor(screen.screenInstanceId, screen.epoch)
                                .any { event -> event.phase == LifecyclePhase.WILL_DISAPPEAR }
                            if (!willDisappearPublished) {
                                eventDispatcher.publishLifecycle(
                                    LifecyclePhase.WILL_DISAPPEAR,
                                    screen.screenInstanceId,
                                    screen.transitionId,
                                    screen.epoch,
                                )
                                executeActions(actions, dispatch)
                            }
                        }
                        rpcDrained = awaitRpcDrain()
                        true
                    } == true
                    if (!flushCompleted) {
                        valueWriteParent.cancelChildren()
                        valueWriteTail = null
                        log(WARN) { "$LOG_PREFIX Flow exit flush timed out; completing Flow exit" }
                    } else if (!rpcDrained) {
                        log(WARN) {
                            "$LOG_PREFIX RPC drain barrier failed after $RPC_BARRIER_MAX_ATTEMPTS attempts; completing Flow exit"
                        }
                    }
                    dispatch(FlowExitFlushed)
                }
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
                stateHandler.stateOwner = null
            }
            else -> return
        }
    }

    private suspend fun executeActions(effect: Effect.ExecuteJSActions, dispatch: (Message) -> Unit) {
        executeJsActions(dispatchSdkActions(effect.actions, dispatch), effect.screen)
    }

    private suspend fun executeJsActions(actions: List<Action>, screen: NavigationEntry) {
        actions.forEach { action ->
            stateHandler.executeAction(action.func, action.params, action.scope, screen)
        }
    }

    private fun dispatchSdkActions(actions: List<Action>, dispatch: (Message) -> Unit): List<Action> {
        val jsActions = mutableListOf<Action>()
        for (action in actions) {
            val shortCircuited = if (action.func.startsWith("SDK.") && action.scope == Scope.Global) {
                sdkActionToJSCallback(action.func, action.params)
            } else null

            if (shortCircuited != null) dispatch(shortCircuited)
            else jsActions.add(action)
        }
        return jsActions
    }

    private suspend fun awaitRpcDrain(): Boolean {
        repeat(RPC_BARRIER_MAX_ATTEMPTS) { attempt ->
            if (stateHandler.awaitRpcDrain()) return true
            if (attempt + 1 < RPC_BARRIER_MAX_ATTEMPTS) {
                delay(RPC_BARRIER_RETRY_DELAY_MILLIS)
            }
        }
        return false
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

    private companion object {
        const val EXIT_FLUSH_TIMEOUT_MILLIS = 6_000L
        const val RPC_BARRIER_MAX_ATTEMPTS = 2
        const val RPC_BARRIER_RETRY_DELAY_MILLIS = 100L
    }
}
