@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import android.os.Looper
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.runOnMain
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

internal interface ActionHandler {
    fun onOpenUrl(source: Message.JSCallback.OpenUrl.Source, openIn: String?)
    fun onUserCustomAction(userCustomId: String)
    fun onPurchaseProduct(productId: String, paywallId: String?, callbackId: String?)
    fun onWebPurchaseProduct(productId: String, paywallId: String?, openIn: String?, callbackId: String?)
    fun onRestorePurchases(callbackId: String?)
    fun onCloseAll()
    fun onSelectProduct(productId: String, paywallId: String?)
    fun onOpenScreen(navEntry: NavigationEntry)
    fun onCloseScreen(navigatorId: String = "default", transitionId: String = "on_disappear")
    fun onMoveScroll(instanceId: String, kind: String, value: String)
    fun onSetTimer(timerId: String, endAtMs: Long?, durationSeconds: Long?, behavior: String?)
    fun onChangeFocus(focusId: String?)
    fun onSendAnalyticsEvent(name: String, params: Map<String, Any?>)
    fun onSendEvents(instanceId: String?, eventIds: List<String>)
    fun onShowAppRate()
    fun onShowAlertDialog(
        title: String?,
        message: String?,
        actions: List<Message.JSCallback.ShowAlertDialog.AlertAction>,
        callbackId: String?,
    )
    fun onShowRequestPermission(
        permission: String?,
        customArgs: Map<String, String>?,
        callbackId: String?,
    )
    fun onJsError(message: String)
}

private class ActionHandlerSubject {
    private val actionQueue = LinkedBlockingQueue<(ActionHandler) -> Unit>()
    private val handlerRef = AtomicReference<ActionHandler?>()
    private val MAX_QUEUE_SIZE = 100

    fun setHandler(handler: ActionHandler?) {
        val previous = handlerRef.getAndSet(handler)
        if (handler != null && previous == null) {
            drainQueue(handler)
        } else if (handler == null && previous != null) {
            clearQueue()
        }
    }

    fun getHandler(): ActionHandler? = handlerRef.get()

    private fun executeOnMain(action: () -> Unit) {
        val isMainThread = Looper.getMainLooper().thread == Thread.currentThread()
        if (isMainThread) {
            action()
        } else {
            runOnMain { action() }
        }
    }

    fun execute(action: (ActionHandler) -> Unit) {
        val handler = handlerRef.get()
        if (handler != null) {
            executeOnMain { action(handler) }
            drainQueueIfNeeded()
        } else {
            if (actionQueue.size >= MAX_QUEUE_SIZE) {
                log(WARN) { "$LOG_PREFIX Action queue full, dropping action" }
                return
            }
            actionQueue.offer(action)
            drainQueueIfNeeded()
        }
    }

    fun reset() {
        clearQueue()
    }

    private fun drainQueueIfNeeded() {
        val handler = handlerRef.get()
        if (handler != null && !actionQueue.isEmpty()) {
            drainQueue(handler)
        }
    }

    private fun drainQueue(handler: ActionHandler) {
        val actions = mutableListOf<(ActionHandler) -> Unit>()
        actionQueue.drainTo(actions)
        actions.forEach { action ->
            try {
                executeOnMain { action(handler) }
            } catch (e: Exception) {
                log(ERROR) { "$LOG_PREFIX Error executing queued action: ${e.localizedMessage}" }
            }
        }
    }

    private fun clearQueue() {
        actionQueue.clear()
    }
}

internal class JSActionBridge(private val gson: Gson) {
    private val actionHandlerSubject = ActionHandlerSubject()

    var actionHandler: ActionHandler?
        get() = actionHandlerSubject.getHandler()
        set(value) {
            actionHandlerSubject.setHandler(value)
        }

    fun reset() {
        actionHandlerSubject.reset()
    }

    fun onJsError(message: String) {
        actionHandlerSubject.execute { it.onJsError(message) }
    }

    @Suppress("UNCHECKED_CAST")
    fun handleRpc(jsonString: String, onResult: (promiseId: String, result: Any?, error: String?) -> Unit) {
        try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val rpcCall: Map<String, Any> = gson.fromJson(jsonString, type)

            val method = rpcCall["method"] as? String ?: return
            val params = rpcCall["params"] as? Map<String, Any?> ?: emptyMap()
            val promiseId = rpcCall["promiseId"] as? String
            
            when (method) {
                "logMessageFromJS" -> {
                    val paramsList = rpcCall["params"] as? List<*>
                    val message = paramsList?.get(0) as? String
                    val level = paramsList?.get(1) as? String
                    if (message != null && level != null) {
                        val logLevel = when (level.lowercase()) {
                            "error" -> ERROR
                            "warn" -> WARN
                            "info" -> INFO
                            else -> VERBOSE
                        }
                        log(logLevel) { "$LOG_PREFIX #js# $message" }
                    }
                }
                
                "SDK.openUrl" -> {
                    val source = (params["url"] as? String)?.let { Message.JSCallback.OpenUrl.Source.Url(it) }
                        ?: (params["stringId"] as? String)?.let { Message.JSCallback.OpenUrl.Source.StringId(it) }
                        ?: return
                    val openIn = params["openIn"] as? String
                    actionHandlerSubject.execute { it.onOpenUrl(source, openIn) }
                }
                "SDK.userCustomAction" -> {
                    val userCustomId = params["userCustomId"] as? String ?: return
                    actionHandlerSubject.execute { it.onUserCustomAction(userCustomId) }
                }
                "SDK.purchaseProduct" -> {
                    val productId = params["productId"] as? String ?: return
                    val paywallId = params["paywallId"] as? String
                    val callbackId = params["__callback_id__"] as? String
                    actionHandlerSubject.execute { it.onPurchaseProduct(productId, paywallId, callbackId) }
                }
                "SDK.webPurchaseProduct" -> {
                    val productId = params["productId"] as? String ?: return
                    val paywallId = params["paywallId"] as? String
                    val openIn = params["openIn"] as? String
                    val callbackId = params["__callback_id__"] as? String
                    actionHandlerSubject.execute { it.onWebPurchaseProduct(productId, paywallId, openIn, callbackId) }
                }
                "SDK.restorePurchases" -> {
                    val callbackId = params["__callback_id__"] as? String
                    actionHandlerSubject.execute { it.onRestorePurchases(callbackId) }
                }
                "SDK.closeAll" -> {
                    actionHandlerSubject.execute { it.onCloseAll() }
                }
                "SDK.onSelectProduct" -> {
                    val productId = params["productId"] as? String ?: return
                    val paywallId = params["paywallId"] as? String
                    actionHandlerSubject.execute { it.onSelectProduct(productId, paywallId) }
                }
                
                "SDK.openScreen" -> {
                    val screenInstanceId = params["instanceId"] as? String ?: run {
                        log(WARN) { "$LOG_PREFIX openScreen ignored: missing or invalid 'instanceId'" }
                        return
                    }
                    val screenType = params["type"] as? String ?: run {
                        log(WARN) { "$LOG_PREFIX openScreen ignored: missing or invalid 'type'" }
                        return
                    }
                    val contextPath = params["contextPath"] as? String
                    val navigatorId = params["navigatorId"] as? String ?: "default"
                    val transitionId = params["transitionId"] as? String ?: run {
                        log(WARN) { "$LOG_PREFIX openScreen ignored: missing or invalid 'transitionId'" }
                        return
                    }
                    actionHandlerSubject.execute { it.onOpenScreen(NavigationEntry(screenInstanceId, screenType, contextPath, navigatorId, transitionId)) }
                }
                "SDK.closeScreen" -> {
                    val navigatorId = params["navigatorId"] as? String ?: "default"
                    val transitionId = params["transitionId"] as? String ?: "on_disappear"
                    actionHandlerSubject.execute { it.onCloseScreen(navigatorId, transitionId) }
                }
                "SDK.moveScroll" -> {
                    val instanceId = params["instanceId"] as? String ?: return
                    val kind = params["kind"] as? String ?: return
                    val value = params["value"] as? String ?: return
                    actionHandlerSubject.execute { it.onMoveScroll(instanceId, kind, value) }
                }
                "SDK.changeFocus" -> {
                    val focusId = params["id"] as? String
                    actionHandlerSubject.execute { it.onChangeFocus(focusId) }
                }
                "SDK.setTimer" -> {
                    val timerId = params["id"] as? String ?: return
                    val endAtMs = (params["endAt"] as? Number)?.toLong()
                    val durationSeconds = (params["duration"] as? Number)?.toLong()
                    val behavior = params["behavior"] as? String
                    actionHandlerSubject.execute { it.onSetTimer(timerId, endAtMs, durationSeconds, behavior) }
                }
                "SDK.sendAnalyticsEvent" -> {
                    val name = params["name"] as? String ?: return
                    actionHandlerSubject.execute { it.onSendAnalyticsEvent(name, params) }
                }
                "SDK.sendEvents" -> {
                    val events = (params["events"] as? List<*>)?.mapNotNull { it as? String } ?: return
                    if (events.isEmpty()) return
                    val instanceId = params["instanceId"] as? String
                    actionHandlerSubject.execute { it.onSendEvents(instanceId, events) }
                }
                "SDK.showAppRate" -> {
                    actionHandlerSubject.execute { it.onShowAppRate() }
                }
                "SDK.showAlertDialog" -> {
                    val title = params["title"] as? String
                    val message = params["message"] as? String
                    val actions = (params["actions"] as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        val actionTitle = map["title"] as? String
                        val styleStr = map["style"] as? String
                        val style = when (styleStr) {
                            "destructive" -> Message.JSCallback.ShowAlertDialog.AlertAction.Style.DESTRUCTIVE
                            "cancel" -> Message.JSCallback.ShowAlertDialog.AlertAction.Style.CANCEL
                            else -> Message.JSCallback.ShowAlertDialog.AlertAction.Style.DEFAULT
                        }
                        val actionId = (map["actionId"] as? String) ?: actionTitle
                        Message.JSCallback.ShowAlertDialog.AlertAction(actionTitle, style, actionId)
                    } ?: emptyList()
                    val callbackId = params["__callback_id__"] as? String
                    actionHandlerSubject.execute { it.onShowAlertDialog(title, message, actions, callbackId) }
                }
                "SDK.showRequestPermission" -> {
                    val permission = params["permission"] as? String
                    @Suppress("UNCHECKED_CAST")
                    val customArgs = (params["customArgs"] as? Map<String, Any?>)
                        ?.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }
                        ?.toMap()
                    val callbackId = params["__callback_id__"] as? String
                    actionHandlerSubject.execute { it.onShowRequestPermission(permission, customArgs, callbackId) }
                }

                else -> {
                    log(WARN) { "$LOG_PREFIX Unknown RPC method: $method" }
                    promiseId?.let { onResult(it, null, "method '$method' not found") }
                }
            }
        } catch (e: Exception) {
            log(WARN) { "$LOG_PREFIX Invalid RPC call: $jsonString: ${e.localizedMessage}" }
        }
    }
}