@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.INFO
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class JSEngineWebView(
    private val context: Context,
    private val jsActionBridge: JSActionBridge,
    private val gson: Gson,
) : JSEngine {

    private var webView: WebView? = null
    private var pollJob: Job? = null

    private val bgScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var onRendererGone: (suspend () -> Unit)? = null

    @Volatile
    private var pageLoaded = CompletableDeferred<Unit>()

    private fun createWebViewClient() = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            pageLoaded.complete(Unit)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            val didCrash = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && detail.didCrash()
            log(WARN) { "$LOG_PREFIX WebView render process gone (didCrash=$didCrash), re-creating…" }
            if (view === webView) {
                webView = null
                view.destroy()
                bgScope.launch { reinitialize() }
            }
            return true
        }
    }

    private suspend fun reinitialize() {
        pollJob?.cancelAndJoin()
        initialize()
        onRendererGone?.invoke()
        log(INFO) { "$LOG_PREFIX WebView re-created after renderer death" }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override suspend fun initialize() {
        withContext(Dispatchers.Main) {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    databaseEnabled = false
                    setSupportMultipleWindows(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    saveFormData = false
                }

                WebView.setWebContentsDebuggingEnabled(false)

                webViewClient = createWebViewClient()

                pageLoaded = CompletableDeferred()
                loadDataWithBaseURL("about:blank", "<html><body></body></html>", "text/html", "utf-8", null)
            }
        }

        awaitPageLoaded()

        withContext(Dispatchers.Main) {
            evaluateJavascriptSuspend(setupScript)
        }

        startPollLoop()
    }

    private suspend fun awaitPageLoaded() {
        withTimeoutOrNull(1000) { pageLoaded.await() }
            ?: log(WARN) { "$LOG_PREFIX onPageFinished not delivered in time, proceeding" }
    }

    private fun startPollLoop() {
        pollJob?.cancel()
        pollJob = bgScope.launch {
            while (isActive) {
                try {
                    val msg = withContext(Dispatchers.Main) {
                        evaluateJavascriptSuspend("__dequeueRpc()")
                    }

                    if (!msg.isNullOrEmpty() && msg != "null") {
                        val cleaned = stripJsResult(msg)
                        withContext(Dispatchers.Main) {
                            jsActionBridge.handleRpc(cleaned) { promiseId, result, error ->
                                val script = buildString {
                                    append("_resolveJsPromise(")
                                    append(gson.toJson(promiseId))
                                    append(", ")
                                    if (error != null) {
                                        append("null, ${gson.toJson(error)}")
                                    } else {
                                        append("${gson.toJson(result)}, null")
                                    }
                                    append(");")
                                }
                                runBlocking(Dispatchers.Main) {
                                    try {
                                        webView?.evaluateJavascript(script) { _ -> }
                                    } catch (e: Exception) {
                                        log(ERROR) { "$LOG_PREFIX_ERROR Error resolving js promise: ${e.localizedMessage}" }
                                    }
                                }
                            }
                        }
                    }

                    delay(50)
                } catch (t: Throwable) {
                    log(ERROR) { "$LOG_PREFIX_ERROR Polling error: ${t.localizedMessage}" }
                    delay(250)
                }
            }
        }
    }

    override suspend fun loadScript(script: String) {
        val injector = """
            window.__adaptyScriptOk = false;
            window.__adaptyLastScriptError = null;
            (function(src) {
              var s = document.createElement('script');
              s.textContent = src + "\n;window.__adaptyScriptOk = true;";
              (document.head || document.documentElement).appendChild(s);
              s.remove();
            })(${gson.toJson(script)});
            window.__adaptyScriptOk ? null : String(window.__adaptyLastScriptError || "unknown top-level script error");
        """.trimIndent()
        val result = withContext(Dispatchers.Main) {
            evaluateJavascriptSuspend(injector)
        }
        val error = stripJsResult(result)
        if (error != "null" && error.isNotEmpty()) {
            throw IllegalStateException(error)
        }
    }
    
    override suspend fun execute(jsExpr: String): Any? {
        val wrapped = """
            (function() {
              function safeStringify(v) {
                try { return JSON.stringify(v) ?? "null"; }
                catch(e) { return JSON.stringify({ "$JS_ERROR_KEY": e.toString() }); }
              }
              try {
                const result = (0, eval)(${gson.toJson(jsExpr)});
                return safeStringify(result);
              } catch (e) {
                return safeStringify({ "$JS_ERROR_KEY": e.toString() });
              }
            })();
        """.trimIndent()

        return try {
            val raw = withContext(Dispatchers.Main) {
                evaluateJavascriptSuspend(wrapped)
            }
            val cleaned = stripJsResult(raw)
            if (cleaned == "null" || cleaned.isEmpty()) return null
            gson.fromJson(cleaned, Any::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error executing JS: '$jsExpr': ${e.localizedMessage}" }
            null
        }
    }

    override suspend fun put(name: String, value: Any) {
        val scriptValue = when (value) {
            is Number, is Boolean -> value.toString()
            is String, is Map<*, *>, is List<*> -> gson.toJson(value)
            else -> {
                log(WARN) { "$LOG_PREFIX Unsupported type for put: ${value::class.java.name}" }
                return
            }
        }
        val path = JSPathUtils.parsePath(name)
        val script = JSPathUtils.generateSetValueScript(path, scriptValue)
        withContext(Dispatchers.Main) {
            try {
                webView?.evaluateJavascript(script) { _ -> }
            } catch (e: Exception) {
                log(ERROR) { "$LOG_PREFIX_ERROR Error in put('$name'): ${e.localizedMessage}" }
            }
        }
    }

    override suspend fun has(name: String): Boolean {
        val path = JSPathUtils.parsePath(name)
        val script = JSPathUtils.generateHasScript(path)
        return try {
            val raw = withContext(Dispatchers.Main) {
                evaluateJavascriptSuspend(script)
            }
            val cleaned = stripJsResult(raw)
            cleaned == "true"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error checking has('$name'): ${e.localizedMessage}" }
            false
        }
    }

    override suspend fun get(name: String): Any? {
        val path = JSPathUtils.parsePath(name)
        val script = JSPathUtils.generateGetJsonScript(path)
        return try {
            val raw = withContext(Dispatchers.Main) {
                evaluateJavascriptSuspend(script)
            }
            val cleaned = stripJsResult(raw)
            if (cleaned == "null" || cleaned.isEmpty()) return null
            gson.fromJson(cleaned, Any::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error getting '$name': ${e.localizedMessage}" }
            null
        }
    }

    override suspend fun reset() {
        pollJob?.cancelAndJoin()
        val wv = webView
        if (wv != null) {
            withContext(Dispatchers.Main) {
                pageLoaded = CompletableDeferred()
                wv.loadDataWithBaseURL("about:blank", "<html><body></body></html>", "text/html", "utf-8", null)
            }
            awaitPageLoaded()
            withContext(Dispatchers.Main) {
                evaluateJavascriptSuspend(setupScript)
            }
            startPollLoop()
        } else {
            initialize()
        }
    }

    fun close() {
        pollJob?.cancel()
        bgScope.cancel()
        runBlocking(Dispatchers.Main) {
            try {
                webView?.stopLoading()
                webView?.removeAllViews()
                webView?.destroy()
            } catch (e: Exception) {
                log(WARN) { "$LOG_PREFIX Error destroying webview: ${e.localizedMessage}" }
            }
            webView = null
        }
    }

    private suspend fun evaluateJavascriptSuspend(script: String): String =
        suspendCoroutine { cont ->
            try {
                webView?.evaluateJavascript(script, ValueCallback<String> { result ->
                    if (result == null) cont.resume("null") else cont.resume(result)
                }) ?: cont.resumeWithException(IllegalStateException("WebView is null"))
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    private fun stripJsResult(raw: String?): String {
        if (raw == null) return "null"
        if (raw.length >= 2 && raw.first() == '"' && raw.last() == '"') {
            return try {
                gson.fromJson(raw, String::class.java)
            } catch (e: Exception) {
                log(WARN) { "$LOG_PREFIX Failed to parse JS result as a JSON string: ${e.localizedMessage}" }
                raw
            }
        }
        return raw
    }

    private val setupScript = """
        // Make sure these are global bindings (use var)
        var __adaptyScriptOk = true;
        var __adaptyLastScriptError = null;
        window.addEventListener('error', function (e) {
          window.__adaptyLastScriptError = (e && e.message) ? e.message : 'unknown error';
        });
        var __rpcQueue = [];
        function postToHost(rpc) {
          try {
            var msg = (typeof rpc === 'string') ? rpc : JSON.stringify(rpc);
            __rpcQueue.push(msg);
          } catch (e) {
            __rpcQueue.push(JSON.stringify({ method: "logMessageFromJS", params: ["postToHost error: " + e.toString(), "ERROR"] }));
          }
        }
        function __dequeueRpc() {
          if (__rpcQueue.length > 0) {
            var m = __rpcQueue.shift();
            return m;
          }
          return null;
        }

        var _pendingPromises = {};
        var _promiseIdCounter = 0;

        function _resolveJsPromise(promiseId, result, error) {
          var p = _pendingPromises[promiseId];
          if (!p) return;
          try {
            if (error) p.reject(new Error(error));
            else p.resolve(result);
          } catch(e) {
            postToHost({ method: "logMessageFromJS", params: ["_resolveJsPromise error: " + e.toString(), "ERROR"] });
          }
          delete _pendingPromises[promiseId];
        }

        function logMessageFromJS(message, level) {
          postToHost({ method: 'logMessageFromJS', params: [message, level] });
        }

    """.trimIndent()
}
