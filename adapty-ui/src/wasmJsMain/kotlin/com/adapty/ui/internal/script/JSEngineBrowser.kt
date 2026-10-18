@file:OptIn(InternalAdaptyUiApi::class, ExperimentalWasmJsInterop::class)

package com.adapty.ui.internal.script

import com.adapty.ui.internal.platform.ERROR
import com.adapty.ui.internal.platform.InternalAdaptyUiApi
import com.adapty.ui.internal.platform.WARN
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.await
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlin.js.Promise

/**
 * Browser counterpart of JSEngineAndroidx: each realm is a hidden same-origin iframe whose host (DOM / Web IDL)
 * globals are deleted, so flow scripts see roughly what an isolated V8 offers (ECMA-262 globals + console).
 *
 * Gap: the non-configurable `window`, `document`, `location` and `top` stay (a flow `var location = …` would
 * navigate the iframe), and EventTarget methods remain reachable through the window's prototype chain.
 */
internal class JSEngineBrowser(private val bridge: JSActionBridge) : JSEngine {

    private var realm: JsAny? = null
    private var generation = 0
    private val rpcScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override suspend fun initialize() {
        createFreshRealm()
    }

    private fun createFreshRealm() {
        realm?.let { removeRealm(it) }
        realm = null
        val gen = ++generation
        val created = createRealm { msg -> dispatchRpc(gen, msg) }
        realm = created
        loadIntoRealm(created, RPC_GLUE)?.let { throw BrowserScriptException("RPC glue failed: $it") }
    }

    // Like the Android poll loop: RPCs reach the bridge on Main, also those posted from later promise continuations
    private fun dispatchRpc(gen: Int, msg: String) {
        rpcScope.launch {
            try {
                bridge.handleRpc(msg) { promiseId, result, error -> resolveJsPromise(gen, promiseId, result, error) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                log(ERROR) { "$LOG_PREFIX_ERROR Error handling RPC: ${e.message}" }
                rpcPromiseId(msg)?.let { resolveJsPromise(gen, it, null, e.message ?: e.toString()) }
            }
        }
    }

    private fun resolveJsPromise(gen: Int, promiseId: String, result: Any?, error: String?) {
        val target = realm ?: return
        if (gen != generation) return
        val id = PlatformJson.stringify(promiseId)
        val script = if (error != null) {
            "_resolveJsPromise($id, null, ${PlatformJson.stringify(error)});"
        } else {
            "_resolveJsPromise($id, ${PlatformJson.stringify(result)}, null);"
        }
        try {
            evalInRealm(target, script)
        } catch (e: Throwable) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error resolving JS promise '$promiseId': ${e.message}" }
        }
    }

    private fun rpcPromiseId(msg: String): String? =
        try {
            (PlatformJson.parse(msg) as? Map<*, *>)?.get("promiseId") as? String
        } catch (_: Exception) {
            null
        }

    override suspend fun execute(jsExpr: String): Any? {
        val target = realm ?: return null
        val resultJson = try {
            executeInRealm(target, jsExpr, JS_ERROR_KEY).await<JsString?>()?.toString()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error during execute: ${e.message}" }
            return null
        }
        return if (resultJson.isNullOrEmpty() || resultJson == "undefined") null else PlatformJson.parse(resultJson)
    }

    override suspend fun loadScript(script: String) {
        val target = realm ?: return
        val error = try {
            loadIntoRealm(target, script)
        } catch (e: Throwable) {
            throw BrowserScriptException(e.message ?: e.toString())
        }
        if (error != null) throw BrowserScriptException(error)
    }

    override suspend fun reset() {
        createFreshRealm()
    }

    override suspend fun put(name: String, value: Any) {
        val scriptValue = when (value) {
            is Number, is Boolean -> value.toString()
            is String, is Map<*, *>, is List<*> -> PlatformJson.stringify(value)
            else -> {
                log(WARN) { "$LOG_PREFIX Unsupported type for 'put' method: ${value::class.simpleName}. Only primitives, Maps, Lists, and Strings are supported." }
                return
            }
        }
        val script = JSPathUtils.generateSetValueScript(JSPathUtils.parsePath(name), scriptValue)
        evalOrDefault("put('$name')", Unit) { evalInRealm(it, script) }
    }

    override suspend fun has(name: String): Boolean {
        val script = JSPathUtils.generateHasScript(JSPathUtils.parsePath(name))
        return evalOrDefault("has('$name')", false) { evalInRealm(it, script) == "true" }
    }

    override suspend fun get(name: String): Any? {
        val script = JSPathUtils.generateGetJsonScript(JSPathUtils.parsePath(name))
        return evalOrDefault("get('$name')", null) { target ->
            evalInRealm(target, script)?.let { PlatformJson.parse(it) }
        }
    }

    private inline fun <T> evalOrDefault(operationName: String, default: T, block: (JsAny) -> T): T {
        val target = realm ?: return default
        return try {
            block(target)
        } catch (e: Throwable) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error during $operationName: ${e.message}" }
            default
        }
    }

    fun close() {
        rpcScope.coroutineContext.cancelChildren()
        realm?.let { removeRealm(it) }
        realm = null
        generation++
    }

    private class BrowserScriptException(message: String) : Exception(message)

    private companion object {
        // Same contract as JSEngineAndroidx.setupGlobalBridge; postToHost calls the host directly (no RPC queue)
        val RPC_GLUE = """
            var postToHost = (function (post, stringify) {
              return function postToHost(rpc) {
                post((typeof rpc === 'string') ? rpc : stringify(rpc));
              };
            })(globalThis.__adapty_post__, JSON.stringify);
            delete globalThis.__adapty_post__;

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
    }
}

// Creates the iframe realm, strips host globals (ECMA-262 + Intl/WebAssembly/console are kept) and
// returns a host-side handle with the iframe's eval/JSON/DOM primitives captured before the cleanup.
@JsFun(
    """(onRpc) => {
    const frame = document.createElement('iframe');
    frame.setAttribute('data-adapty-js-realm', '');
    frame.setAttribute('aria-hidden', 'true');
    frame.tabIndex = -1;
    frame.style.display = 'none';
    (document.body || document.documentElement).appendChild(frame);
    const win = frame.contentWindow;
    const doc = win.document;
    const realm = {
        frame: frame,
        doc: doc,
        root: doc.documentElement,
        error: undefined,
        eval: win.eval,
        stringify: win.JSON.stringify,
        createElement: win.Document.prototype.createElement,
        setText: Object.getOwnPropertyDescriptor(win.HTMLScriptElement.prototype, 'text').set,
        appendChild: win.Node.prototype.appendChild,
        removeChild: win.Node.prototype.removeChild,
    };
    win.addEventListener('error', (ev) => {
        realm.error = (ev.error !== undefined && ev.error !== null) ? ev.error : ev.message;
        ev.preventDefault();
    });
    const keep = new Set([
        'globalThis', 'Infinity', 'NaN', 'undefined',
        'eval', 'isFinite', 'isNaN', 'parseFloat', 'parseInt',
        'decodeURI', 'decodeURIComponent', 'encodeURI', 'encodeURIComponent', 'escape', 'unescape',
        'AggregateError', 'Array', 'ArrayBuffer', 'BigInt', 'BigInt64Array', 'BigUint64Array', 'Boolean',
        'DataView', 'Date', 'Error', 'EvalError', 'FinalizationRegistry', 'Float16Array', 'Float32Array',
        'Float64Array', 'Function', 'Int8Array', 'Int16Array', 'Int32Array', 'Iterator', 'Map', 'Number',
        'Object', 'Promise', 'Proxy', 'RangeError', 'ReferenceError', 'RegExp', 'Set', 'SharedArrayBuffer',
        'String', 'Symbol', 'SyntaxError', 'TypeError', 'Uint8Array', 'Uint8ClampedArray', 'Uint16Array',
        'Uint32Array', 'URIError', 'WeakMap', 'WeakRef', 'WeakSet',
        'Atomics', 'JSON', 'Math', 'Reflect', 'Intl', 'WebAssembly', 'console',
    ]);
    for (const key of Object.getOwnPropertyNames(win)) {
        if (keep.has(key)) continue;
        try { delete win[key]; } catch (e) { /* non-configurable: window, document, location, top */ }
    }
    win.__adapty_post__ = (msg) => { onRpc(msg); };
    return realm;
}"""
)
private external fun createRealm(onRpc: (String) -> Unit): JsAny

// Runs code as a classic <script> (top-level let/const stay global like in evaluateJavaScript); returns the error text
@JsFun(
    """(realm, code) => {
    realm.error = undefined;
    const script = realm.createElement.call(realm.doc, 'script');
    realm.setText.call(script, code);
    realm.appendChild.call(realm.root, script);
    realm.removeChild.call(realm.root, script);
    const err = realm.error;
    realm.error = undefined;
    if (err === undefined) return null;
    try { return String(err); } catch (e) { return 'Error'; }
}"""
)
private external fun loadIntoRealm(realm: JsAny, code: String): String?

// Same wrapper as JSEngineAndroidx.execute: JSON.stringify(await (0, eval)(expr)), errors → { JS_ERROR_KEY: e.toString() }
@JsFun(
    """(realm, expr, errorKey) => (async () => {
    try {
        return realm.stringify(await realm.eval(expr));
    } catch (e) {
        let message;
        try { message = String(e); } catch (_) { message = 'Error'; }
        return realm.stringify({ [errorKey]: message });
    }
})().then((json) => json === undefined ? null : json)"""
)
private external fun executeInRealm(realm: JsAny, expr: String, errorKey: String): Promise<JsAny?>

@JsFun("(realm, code) => { const v = realm.eval(code); return (v === undefined || v === null) ? null : String(v); }")
private external fun evalInRealm(realm: JsAny, code: String): String?

@JsFun("(realm) => { realm.frame.remove(); }")
private external fun removeRealm(realm: JsAny)
