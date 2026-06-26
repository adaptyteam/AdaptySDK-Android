@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import android.content.Context
import androidx.javascriptengine.JavaScriptIsolate
import androidx.javascriptengine.JavaScriptSandbox
import androidx.javascriptengine.SandboxDeadException
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class JSEngineAndroidx(
    private val context: Context,
    private val jsActionBridge: JSActionBridge,
    private val gson: Gson,
) :
    JSEngine {

    companion object {
        private val sandboxMutex = Mutex()
        private var sharedSandbox: JavaScriptSandbox? = null
        private var sharedGeneration = 0L

        suspend fun acquireSandbox(context: Context): Pair<JavaScriptSandbox, Long> {
            sandboxMutex.withLock {
                sharedSandbox?.let { return it to sharedGeneration }
                val sandbox = JavaScriptSandbox.createConnectedInstanceAsync(context).await()
                sharedSandbox = sandbox
                return sandbox to sharedGeneration
            }
        }

        suspend fun reconnectSandbox(context: Context, failedGeneration: Long): Pair<JavaScriptSandbox, Long> {
            sandboxMutex.withLock {
                if (sharedGeneration != failedGeneration) {
                    sharedSandbox?.let { return it to sharedGeneration }
                } else {
                    try { sharedSandbox?.close() } catch (_: Exception) {}
                    sharedSandbox = null
                    sharedGeneration++
                }
                val sandbox = JavaScriptSandbox.createConnectedInstanceAsync(context).await()
                sharedSandbox = sandbox
                return sandbox to sharedGeneration
            }
        }
    }

    private var jsSandbox: JavaScriptSandbox? = null
    private var jsIsolate: JavaScriptIsolate? = null

    private val addedInterfaces = mutableSetOf<String>()

    private var pollJob: Job? = null

    private val reinitMutex = Mutex()
    private var sandboxGeneration = 0L

    var onSandboxDied: (suspend () -> Unit)? = null

    private suspend fun <T> withSandboxRecovery(
        operationName: String,
        default: T,
        block: suspend (JavaScriptIsolate) -> T,
    ): T {
        val isolate = jsIsolate ?: return default
        val gen = sandboxGeneration
        return try {
            block(isolate)
        } catch (e: Exception) {
            if (isSandboxDead(e)) {
                log(WARN) { "$LOG_PREFIX Sandbox died during $operationName, re-initializing…: ${e.localizedMessage}" }
                reinitializeSandbox(gen)
            } else {
                log(ERROR) { "$LOG_PREFIX_ERROR Error during $operationName: ${e.localizedMessage}" }
            }
            default
        }
    }

    private suspend fun reinitializeSandbox(failedGeneration: Long) {
        reinitMutex.withLock {
            if (sandboxGeneration != failedGeneration) return
            sandboxGeneration++
            log(WARN) { "$LOG_PREFIX Sandbox died, re-creating…" }
            tearDownIsolate()
            try {
                val (sandbox, gen) = reconnectSandbox(context, failedGeneration)
                jsSandbox = sandbox
                sandboxGeneration = gen
                jsIsolate = sandbox.createIsolate()
                setupGlobalBridge()
                startPollLoop()
                onSandboxDied?.invoke()
                log(INFO) { "$LOG_PREFIX Sandbox re-created successfully" }
            } catch (e: Exception) {
                log(ERROR) { "$LOG_PREFIX_ERROR Failed to re-create sandbox: ${e.localizedMessage}" }
            }
        }
    }

    private fun tearDownIsolate() {
        pollJob?.cancel()
        pollJob = null
        try { jsIsolate?.close() } catch (_: Exception) {}
        jsIsolate = null
    }

    private fun tearDown() {
        tearDownIsolate()
        jsSandbox = null
    }

    override suspend fun initialize() {
        val (sandbox, gen) = acquireSandbox(context)
        jsSandbox = sandbox
        sandboxGeneration = gen
        jsIsolate = sandbox.createIsolate()

        setupGlobalBridge()
        startPollLoop()
    }

    private fun startPollLoop() {
        pollJob?.cancel()
        pollJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val gen = sandboxGeneration
                try {
                    val msg = jsIsolate?.evaluateJavaScriptAsync("__nextRpc()")?.await()
                    if (!msg.isNullOrEmpty() && msg != "null") {
                        withContext(Dispatchers.Main) {
                            jsActionBridge.handleRpc(msg) { promiseId, result, error ->
                                val script = if (error != null)
                                    "_resolveJsPromise('$promiseId', null, ${gson.toJson(error)});"
                                else
                                    "_resolveJsPromise('$promiseId', ${gson.toJson(result)}, null);"
                                jsIsolate?.evaluateJavaScriptAsync(script)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isSandboxDead(e)) {
                        log(WARN) { "$LOG_PREFIX Sandbox died during polling, re-initializing…: ${e.localizedMessage}" }
                        reinitializeSandbox(gen)
                        return@launch
                    }
                    log(ERROR) { "$LOG_PREFIX_ERROR Error during RPC poll: ${e.localizedMessage}" }
                    delay(50)
                }
            }
        }
    }

    private suspend fun setupGlobalBridge() {
        val setupScript = """
            // RPC transport: an outgoing queue plus a single host waiter so the
            // host can long-poll (__nextRpc) instead of busy-polling, avoiding a
            // continuous 50ms IPC round-trip to the out-of-process sandbox.
            const __rpcQueue = [];
            let __rpcWaiter = null;
            function postToHost(rpc) {
              const msg = (typeof rpc === 'string') ? rpc : JSON.stringify(rpc);
              if (__rpcWaiter) {
                const resolve = __rpcWaiter;
                __rpcWaiter = null;
                resolve(msg);
              } else {
                __rpcQueue.push(msg);
              }
            }
            // Host long-polls this: resolves with the next message as soon as one
            // is available — immediately if queued, otherwise when postToHost fires.
            function __nextRpc() {
              if (__rpcQueue.length > 0) {
                return Promise.resolve(__rpcQueue.shift());
              }
              return new Promise((resolve) => { __rpcWaiter = resolve; });
            }

            // Promise storage for async replies from host
            const _pendingPromises = {};
            let _promiseIdCounter = 0;

            // Host will call this to resolve/reject JS promises
            function _resolveJsPromise(promiseId, result, error) {
              const promise = _pendingPromises[promiseId];
              if (!promise) return;
              if (error) {
                try { promise.reject(new Error(error)); } catch(e) { /* ignore */ }
              } else {
                try { promise.resolve(result); } catch(e) { /* ignore */ }
              }
              delete _pendingPromises[promiseId];
            }

            function logMessageFromJS(message, level) {
              const rpc = { method: 'logMessageFromJS', params: [message, level] };
              postToHost(rpc);
            }
        """.trimIndent()
        jsIsolate?.evaluateJavaScriptAsync(setupScript)?.await()
    }

    override suspend fun execute(jsExpr: String): Any? {
        val wrappedExpr = """
            (async function() {
                try {
                    const result = await (0, eval)(${gson.toJson(jsExpr)});
                    return JSON.stringify(result);
                } catch (e) {
                    return JSON.stringify({ "$JS_ERROR_KEY": e.toString() });
                }
            })();
        """.trimIndent()

        return withSandboxRecovery("execute", null) { isolate ->
            val resultJson = isolate.evaluateJavaScriptAsync(wrappedExpr).await()
            if (resultJson.isNullOrEmpty() || resultJson == "undefined") null
            else gson.fromJson(resultJson, Any::class.java)
        }
    }

    override suspend fun loadScript(script: String) {
        val isolate = jsIsolate ?: return
        val gen = sandboxGeneration
        try {
            isolate.evaluateJavaScriptAsync(script).await()
        } catch (e: Exception) {
            if (isSandboxDead(e)) {
                log(WARN) { "$LOG_PREFIX Sandbox died during loadScript, re-initializing…: ${e.localizedMessage}" }
                reinitializeSandbox(gen)
            } else {
                throw e
            }
        }
    }

    override suspend fun reset() {
        tearDownIsolate()
        addedInterfaces.clear()
        val sandbox = jsSandbox
        if (sandbox != null) {
            jsIsolate = sandbox.createIsolate()
            setupGlobalBridge()
            startPollLoop()
        } else {
            initialize()
        }
    }

    override suspend fun put(name: String, value: Any) {
        val scriptValue = when (value) {
            is Number, is Boolean -> value.toString()
            is String, is Map<*, *>, is List<*> -> gson.toJson(value)
            else -> {
                log(WARN) { "$LOG_PREFIX Unsupported type for 'put' method: ${value::class.java.name}. Only primitives, Maps, Lists, and Strings are supported." }
                return
            }
        }
        val path = JSPathUtils.parsePath(name)
        val script = JSPathUtils.generateSetValueScript(path, scriptValue)
        withSandboxRecovery("put('$name')", Unit) { isolate ->
            isolate.evaluateJavaScriptAsync(script).await()
        }
    }

    override suspend fun has(name: String): Boolean {
        val path = JSPathUtils.parsePath(name)
        val script = JSPathUtils.generateHasScript(path)
        return withSandboxRecovery("has('$name')", false) { isolate ->
            isolate.evaluateJavaScriptAsync(script).await()?.toBoolean() ?: false
        }
    }

    override suspend fun get(name: String): Any? {
        val path = JSPathUtils.parsePath(name)
        val script = JSPathUtils.generateGetJsonScript(path)
        return withSandboxRecovery("get('$name')", null) { isolate ->
            val resultJson = isolate.evaluateJavaScriptAsync(script).await()
            gson.fromJson(resultJson, Any::class.java)
        }
    }

    fun close() {
        tearDown()
    }
}

internal fun isSandboxDead(e: Throwable): Boolean =
    e is SandboxDeadException || e.cause is SandboxDeadException
