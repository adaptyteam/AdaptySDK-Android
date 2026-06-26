@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import android.content.Context
import com.adapty.internal.data.cloud.AnalyticsTracker
import com.adapty.internal.data.models.AnalyticsEvent
import com.adapty.internal.di.Dependencies
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class JSStateMachine(
    private val context: Context,
    private val jsActionBridge: JSActionBridge,
    private val gson: Gson,
) {
    private var jsEngine: JSEngine? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val initialized = CompletableDeferred<Unit>()
    @Volatile private var freshlyInitialized = false
    
    fun setActionHandler(handler: ActionHandler?) {
        jsActionBridge.actionHandler = handler
    }

    init {
        scope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        try {
            val androidx = JSEngineAndroidx(context, jsActionBridge, gson)
            jsEngine = try {
                androidx.initialize()
                androidx.also { js ->
                    js.onSandboxDied = {
                        log(WARN) { "$LOG_PREFIX Sandbox died — re-setting up bridge functions" }
                        setupBridgeFunctions()
                    }
                }
            } catch (e: Throwable) {
                log(WARN) { "$LOG_PREFIX JSEngineAndroidx unavailable, falling back to JSEngineWebView: ${e.localizedMessage}" }
                reportWebViewFallback(e)
                try { androidx.close() } catch (_: Throwable) {}
                JSEngineWebView(context, jsActionBridge, gson).also { js ->
                    js.onRendererGone = {
                        log(WARN) { "$LOG_PREFIX WebView renderer died — re-setting up bridge functions" }
                        setupBridgeFunctions()
                    }
                    js.initialize()
                }
            }

            setupBridgeFunctions()
            freshlyInitialized = true
        } catch (e: Throwable) {
            log(ERROR) { "$LOG_PREFIX_ERROR JS engine initialization failed; scripting disabled: ${e.localizedMessage}" }
        } finally {
            initialized.complete(Unit)
        }
    }

    suspend fun reset() {
        jsActionBridge.reset()
        initialized.await()
        if (freshlyInitialized) {
            freshlyInitialized = false
            return
        }
        try {
            jsEngine?.reset()
            setupBridgeFunctions()
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error resetting JS engine: ${e.localizedMessage}" }
        }
    }
    
    suspend fun loadScript(script: String) {
        initialized.await()
        freshlyInitialized = false
        try {
            jsEngine?.loadScript(script)
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error loading script: ${e.localizedMessage}" }
            if (!isSandboxDead(e)) {
                jsActionBridge.onJsError(e.message ?: e.toString())
            }
        }
    }

    private fun reportJsErrorIfPresent(result: Any?, operation: String): Boolean {
        val message = jsErrorMessage(result) ?: return false
        log(ERROR) { "$LOG_PREFIX_ERROR JS error during $operation: $message" }
        jsActionBridge.onJsError(message)
        return true
    }

    private fun jsErrorMessage(result: Any?): String? =
        (result as? Map<*, *>)?.get(JS_ERROR_KEY) as? String

    private suspend fun setupBridgeFunctions() {
        val bridgeScript = """
            function log(message) {
                const rpc = { method: 'logMessageFromJS', params: [message, 'DEBUG'] };
                postToHost(rpc);
            }
            
            // SDK object - provides functions that call back to Kotlin
            var SDK = {};
            
            SDK.openUrl = function(params) {
                const rpc = { method: 'SDK.openUrl', params: params };
                postToHost(rpc);
            };
            
            SDK.userCustomAction = function(params) {
                const rpc = { method: 'SDK.userCustomAction', params: params };
                postToHost(rpc);
            };
            
            var __adapty_purchase_callbacks__ = {};
            var __adapty_purchase_seq__ = 0;
            SDK.purchaseProduct = function(params) {
                var rpcParams = {};
                for (var k in params) {
                    if (k !== 'callback') rpcParams[k] = params[k];
                }
                if (typeof params.callback === 'function') {
                    var cbId = '__purchase_' + (++__adapty_purchase_seq__);
                    __adapty_purchase_callbacks__[cbId] = params.callback;
                    rpcParams.__callback_id__ = cbId;
                }
                const rpc = { method: 'SDK.purchaseProduct', params: rpcParams };
                postToHost(rpc);
            };

            SDK.webPurchaseProduct = function(params) {
                var rpcParams = {};
                for (var k in params) {
                    if (k !== 'callback') rpcParams[k] = params[k];
                }
                if (typeof params.callback === 'function') {
                    var cbId = '__purchase_' + (++__adapty_purchase_seq__);
                    __adapty_purchase_callbacks__[cbId] = params.callback;
                    rpcParams.__callback_id__ = cbId;
                }
                const rpc = { method: 'SDK.webPurchaseProduct', params: rpcParams };
                postToHost(rpc);
            };

            var __adapty_restore_callbacks__ = {};
            var __adapty_restore_seq__ = 0;
            SDK.restorePurchases = function(params) {
                params = params || {};
                var rpcParams = {};
                for (var k in params) {
                    if (k !== 'callback') rpcParams[k] = params[k];
                }
                if (typeof params.callback === 'function') {
                    var cbId = '__restore_' + (++__adapty_restore_seq__);
                    __adapty_restore_callbacks__[cbId] = params.callback;
                    rpcParams.__callback_id__ = cbId;
                }
                const rpc = { method: 'SDK.restorePurchases', params: rpcParams };
                postToHost(rpc);
            };
            function __adapty_invoke_purchase_callback__(callbackId, response) {
                var cb = __adapty_purchase_callbacks__[callbackId];
                if (typeof cb === 'function') {
                    cb(response);
                    delete __adapty_purchase_callbacks__[callbackId];
                }
            }
            function __adapty_invoke_restore_callback__(callbackId, response) {
                var cb = __adapty_restore_callbacks__[callbackId];
                if (typeof cb === 'function') {
                    cb(response);
                    delete __adapty_restore_callbacks__[callbackId];
                }
            }
            
            SDK.closeAll = function(params) {
                const rpc = { method: 'SDK.closeAll', params: params || {} };
                postToHost(rpc);
            };
            
            SDK.onSelectProduct = function(params) {
                const rpc = { method: 'SDK.onSelectProduct', params: params };
                postToHost(rpc);
            };
            
            SDK.log = function(params) {
                const rpc = { method: 'logMessageFromJS', params: [params.message, params.level || 'DEBUG'] };
                postToHost(rpc);
            };
            
            // Screen functions
            SDK.openScreen = function(params) {
                const rpc = { method: 'SDK.openScreen', params: params };
                postToHost(rpc);
            }
            
            SDK.closeScreen = function(params) {
                const rpc = { method: 'SDK.closeScreen', params: params || {} };
                postToHost(rpc);
            }

            SDK.moveScroll = function(params) {
                const rpc = { method: 'SDK.moveScroll', params: params };
                postToHost(rpc);
            }

            SDK.changeFocus = function(params) {
                const rpc = { method: 'SDK.changeFocus', params: params };
                postToHost(rpc);
            }

            var __adapty_timer_callbacks__ = {};
            SDK.setTimer = function(params) {
                if (typeof params.callback === 'function') {
                    __adapty_timer_callbacks__[params.id] = params.callback;
                }
                var rpcParams = {};
                for (var k in params) {
                    if (k !== 'callback') rpcParams[k] = params[k];
                }
                const rpc = { method: 'SDK.setTimer', params: rpcParams };
                postToHost(rpc);
            }
            function __adapty_invoke_timer_callback__(timerId) {
                var cb = __adapty_timer_callbacks__[timerId];
                if (typeof cb === 'function') {
                    delete __adapty_timer_callbacks__[timerId];
                    cb({ timerId: timerId });
                }
            }

            // SDK.sendAnalyticsEvent / SDK.sendEvents / SDK.showAppRate — fire-and-forget
            SDK.sendAnalyticsEvent = function(params) {
                const rpc = { method: 'SDK.sendAnalyticsEvent', params: params || {} };
                postToHost(rpc);
            }
            SDK.sendEvents = function(params) {
                const rpc = { method: 'SDK.sendEvents', params: params || {} };
                postToHost(rpc);
            }
            SDK.showAppRate = function(params) {
                const rpc = { method: 'SDK.showAppRate', params: params || {} };
                postToHost(rpc);
            }

            // SDK.showAlertDialog — callback receives { actionId: string|null }
            var __adapty_alert_callbacks__ = {};
            var __adapty_alert_seq__ = 0;
            SDK.showAlertDialog = function(params) {
                var rpcParams = {};
                for (var k in params) {
                    if (k !== 'callback') rpcParams[k] = params[k];
                }
                if (typeof params.callback === 'function') {
                    var cbId = '__alert_' + (++__adapty_alert_seq__);
                    __adapty_alert_callbacks__[cbId] = params.callback;
                    rpcParams.__callback_id__ = cbId;
                }
                const rpc = { method: 'SDK.showAlertDialog', params: rpcParams };
                postToHost(rpc);
            }
            function __adapty_invoke_alert_callback__(callbackId, response) {
                var cb = __adapty_alert_callbacks__[callbackId];
                if (typeof cb === 'function') {
                    cb(response);
                    delete __adapty_alert_callbacks__[callbackId];
                }
            }

            // SDK.showRequestPermission — callback receives { permission, customArgs?, result, detailResult? }
            var __adapty_perm_callbacks__ = {};
            var __adapty_perm_seq__ = 0;
            SDK.showRequestPermission = function(params) {
                var rpcParams = {};
                for (var k in params) {
                    if (k !== 'callback') rpcParams[k] = params[k];
                }
                if (typeof params.callback === 'function') {
                    var cbId = '__perm_' + (++__adapty_perm_seq__);
                    __adapty_perm_callbacks__[cbId] = params.callback;
                    rpcParams.__callback_id__ = cbId;
                }
                const rpc = { method: 'SDK.showRequestPermission', params: rpcParams };
                postToHost(rpc);
            }
            function __adapty_invoke_permission_callback__(callbackId, response) {
                var cb = __adapty_perm_callbacks__[callbackId];
                if (typeof cb === 'function') {
                    cb(response);
                    delete __adapty_perm_callbacks__[callbackId];
                }
            }

            // Snapshot global keys before user script loads so refreshStateCache
            // can distinguish user-defined variables from browser/engine built-ins.
            var __adapty_baseline_keys__ = {};
            (function() {
                var g = (typeof globalThis !== 'undefined') ? globalThis : this;
                var names = Object.getOwnPropertyNames(g);
                for (var i = 0; i < names.length; i++) __adapty_baseline_keys__[names[i]] = 1;
                __adapty_baseline_keys__['__adapty_baseline_keys__'] = 1;
            })();
        """.trimIndent()

        try {
            jsEngine?.loadScript(bridgeScript)
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error loading bridge script: ${e.localizedMessage}" }
        }
    }

    suspend fun executeAction(funcPath: List<PathComponent>, params: Map<String, Any?>) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return

        val paramsJson = gson.toJson(params)
        val script = JSPathUtils.generateCallScript(funcPath, paramsJson)

        try {
            val result = jsEngine.execute(script)
            if (reportJsErrorIfPresent(result, "action '${funcPath.joinToString(".") { it.value }}'")) return
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error executing action '${funcPath.joinToString(".") { it.value }}': ${e.localizedMessage}" }
        }
    }

    suspend fun getValue(key: String): Any? {
        initialized.await()
        val jsEngine = this.jsEngine ?: return null

        return try {
            val result = jsEngine.execute(key)
            if (reportJsErrorIfPresent(result, "getValue")) return null
            result
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error in getValue: ${e.localizedMessage}" }
            null
        }
    }

    suspend fun hasFunction(path: List<PathComponent>): Boolean {
        initialized.await()
        val jsEngine = this.jsEngine ?: return false
        return try {
            val script = JSPathUtils.generateHasFunctionScript(path)
            val result = jsEngine.execute(script)
            if (reportJsErrorIfPresent(result, "hasFunction '${path.joinToString(".") { it.value }}'")) return false
            result == true || result == "true"
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error checking hasFunction for '${path.joinToString(".") { it.value }}': ${e.localizedMessage}" }
            false
        }
    }

    suspend fun executeScript(script: String): Any? {
        initialized.await()
        val jsEngine = this.jsEngine ?: return null
        return try {
            val result = jsEngine.execute(script)
            if (reportJsErrorIfPresent(result, "executeScript")) return null
            result
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error executing script: ${e.localizedMessage}" }
            null
        }
    }

    suspend fun injectSDKGlobals(sdkEnvJson: String, sdkProductsJson: String) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val script = """
                var __sdkEnv__ = $sdkEnvJson; Object.freeze(__sdkEnv__);
                Object.defineProperty(globalThis, 'SDKEnv', { value: __sdkEnv__, writable: false, configurable: false, enumerable: false });
                var __sdkProducts__ = $sdkProductsJson; Object.freeze(__sdkProducts__);
                Object.defineProperty(globalThis, 'SDKProducts', { value: __sdkProducts__, writable: false, configurable: true, enumerable: false });
            """.trimIndent()
            jsEngine.loadScript(script)
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error injecting SDK globals: ${e.localizedMessage}" }
        }
    }

    suspend fun updateSDKProducts(sdkProductsJson: String) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val script = """
                var __sdkProducts__ = $sdkProductsJson; Object.freeze(__sdkProducts__);
                Object.defineProperty(globalThis, 'SDKProducts', { value: __sdkProducts__, writable: false, configurable: true, enumerable: false });
            """.trimIndent()
            jsEngine.loadScript(script)
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error updating SDKProducts: ${e.localizedMessage}" }
        }
    }

    suspend fun sendSDKEvent(eventJson: String) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val script = """
                if (typeof handleSDKEvent === 'function') { handleSDKEvent($eventJson); true; } else { false; }
            """.trimIndent()
            val result = jsEngine.execute(script)
            val errorMessage = jsErrorMessage(result)
            if (errorMessage != null) {
                log(ERROR) { "$LOG_PREFIX_ERROR JS error during sendSDKEvent: $errorMessage" }
            } else if (result != true && result != "true") {
                log(VERBOSE) { "$LOG_PREFIX handleSDKEvent not defined, ignoring event: $eventJson" }
            }
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error sending SDK event: ${e.localizedMessage}" }
        }
    }

    suspend fun invokeTimerCallback(timerId: String) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val escapedId = timerId.replace("\\", "\\\\").replace("\"", "\\\"")
            val result = jsEngine.execute("__adapty_invoke_timer_callback__(\"$escapedId\")")
            reportJsErrorIfPresent(result, "timer callback '$timerId'")
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error invoking timer callback for '$timerId': ${e.localizedMessage}" }
        }
    }

    suspend fun invokeAlertCallback(callbackId: String, response: Map<String, Any?>) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val escapedId = callbackId.replace("\\", "\\\\").replace("\"", "\\\"")
            val responseJson = gson.toJson(response)
            val result = jsEngine.execute("__adapty_invoke_alert_callback__(\"$escapedId\", $responseJson)")
            reportJsErrorIfPresent(result, "alert callback '$callbackId'")
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error invoking alert callback for '$callbackId': ${e.localizedMessage}" }
        }
    }

    suspend fun invokePurchaseCallback(callbackId: String, response: Map<String, Any?>) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val escapedId = callbackId.replace("\\", "\\\\").replace("\"", "\\\"")
            val responseJson = gson.toJson(response)
            val result = jsEngine.execute("__adapty_invoke_purchase_callback__(\"$escapedId\", $responseJson)")
            reportJsErrorIfPresent(result, "purchase callback '$callbackId'")
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error invoking purchase callback for '$callbackId': ${e.localizedMessage}" }
        }
    }

    suspend fun invokeRestoreCallback(callbackId: String, response: Map<String, Any?>) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val escapedId = callbackId.replace("\\", "\\\\").replace("\"", "\\\"")
            val responseJson = gson.toJson(response)
            val result = jsEngine.execute("__adapty_invoke_restore_callback__(\"$escapedId\", $responseJson)")
            reportJsErrorIfPresent(result, "restore callback '$callbackId'")
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error invoking restore callback for '$callbackId': ${e.localizedMessage}" }
        }
    }

    suspend fun invokePermissionCallback(callbackId: String, response: Map<String, Any?>) {
        initialized.await()
        val jsEngine = this.jsEngine ?: return
        try {
            val escapedId = callbackId.replace("\\", "\\\\").replace("\"", "\\\"")
            val responseJson = gson.toJson(response)
            val result = jsEngine.execute("__adapty_invoke_permission_callback__(\"$escapedId\", $responseJson)")
            reportJsErrorIfPresent(result, "permission callback '$callbackId'")
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error invoking permission callback for '$callbackId': ${e.localizedMessage}" }
        }
    }

    fun close() {
        when (val js = jsEngine) {
            is JSEngineAndroidx -> js.close()
            is JSEngineWebView -> js.close()
        }
    }

    private fun reportWebViewFallback(cause: Throwable) {
        if (!webViewFallbackReported.compareAndSet(false, true)) return
        runCatching {
            Dependencies.injectInternal<AnalyticsTracker>(named = "base").trackSystemEvent(
                AnalyticsEvent.InternalEventData.create(
                    eventName = "webview_js_engine_fallback",
                    error = cause.toString().take(200),
                )
            )
        }
    }

    private companion object {
        private val webViewFallbackReported = AtomicBoolean(false)
    }
}
