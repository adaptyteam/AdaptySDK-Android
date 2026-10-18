@file:OptIn(com.adapty.ui.internal.platform.InternalAdaptyUiApi::class)

package com.adapty.ui.internal.script

import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.localeForTag
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.preview.previewDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

// Flow scripts through the shared JSStateMachine/JSStateHandler running on JSEngineBrowser.
class JSEngineBrowserTest {

    private class Flow {
        val handler = RecordingActionHandler()
        val bridge = JSActionBridge().also { it.actionHandler = handler }
        val machine = JSStateMachine(PlatformContextHandle(), bridge)
        val state = JSStateHandler(machine)

        suspend fun snapshot(): Map<String, Any?> = state.collectStateSnapshot() ?: fail("no state snapshot")
    }

    private fun flowTest(block: suspend Flow.() -> Unit) = runTest {
        val flow = Flow()
        try {
            flow.block()
        } finally {
            flow.machine.close()
        }
    }

    @Test
    fun calmToggleUpdatesCollectedState() = flowTest {
        state.loadScript(fetchFixture("script.js"))
        assertEquals(false, snapshot()["goal2Selected"])
        machine.executeScript("toggleGoal2()")
        assertEquals(true, snapshot()["goal2Selected"])
    }

    @Test
    fun openScreenReachesHost() = flowTest {
        state.loadScript(fetchFixture("script.js"))
        machine.executeScript("goToReviews()")
        eventually { handler.openedScreens.isNotEmpty() }
        val entry = handler.openedScreens.single()
        val expected = NavigationEntry(
            screenInstanceId = "reviews",
            screenType = "screen_reviews",
            contextPath = null,
            navigatorId = "default",
            transitionId = "forward",
            epoch = entry.epoch,
        )
        assertEquals(expected, entry)
    }

    @Test
    fun stateNumbersAreDoubles() = flowTest {
        state.loadScript("var tabIndex = 0;")
        val tabIndex = snapshot()["tabIndex"]
        assertTrue(tabIndex is Double, "tabIndex must be Double like Gson, got ${tabIndex?.let { it::class.simpleName }}")
        assertEquals(0.0, tabIndex)
    }

    @Test
    fun browserGlobalsDoNotShadowFlowState() = flowTest {
        state.loadScript("var name = 'x'; var status = 1; var self = 'me'; var length = 3;")
        val snapshot = snapshot()
        assertEquals("x", snapshot["name"])
        assertEquals(1.0, snapshot["status"])
        assertEquals("me", snapshot["self"])
        assertEquals(3.0, snapshot["length"])
        // Like an isolated V8: nothing but the script's own globals is collected
        assertEquals(setOf("name", "status", "self", "length"), snapshot.keys)
        assertEquals("string|number", machine.getValue("typeof name + '|' + typeof status"))
    }

    @Test
    fun hostGlobalsAreRemoved() = flowTest {
        val leftovers = machine.getValue(
            """
            ['fetch','setTimeout','alert','HTMLElement','EventTarget','localStorage','navigator','queueMicrotask','chrome','frames','parent','opener']
                .filter(function (k) { return Object.prototype.hasOwnProperty.call(globalThis, k); })
            """.trimIndent()
        )
        assertEquals(emptyList<Any?>(), leftovers)
        // Non-deletable host properties are the documented gap
        assertEquals("object", machine.getValue("typeof document"))
    }

    @Test
    fun onlyUnforgeableHostGlobalsRemain() = runTest {
        val engine = JSEngineBrowser(JSActionBridge())
        engine.initialize()
        try {
            val names = (engine.execute("Object.getOwnPropertyNames(globalThis)") as List<*>).map { it as String }.toSet()
            val leftovers = names - ECMA_GLOBALS - setOf("postToHost", "_resolveJsPromise", "logMessageFromJS")
            assertEquals(setOf("window", "document", "location", "top"), leftovers)
        } finally {
            closeJsEngine(engine)
        }
    }

    @Test
    fun throwingActionDoesNotBreakEngine() = flowTest {
        state.loadScript("function boom() { throw new Error('x'); }")
        assertNull(machine.executeScript("boom()"))
        assertEquals(listOf("Error: x"), handler.jsErrors)
        assertEquals(2.0, machine.getValue("1+1"))
    }

    @Test
    fun engineExecuteMapsErrorsToMarker() = runTest {
        val engine = JSEngineBrowser(JSActionBridge())
        engine.initialize()
        try {
            engine.loadScript("function boom() { throw new Error('x'); }")
            assertEquals(mapOf(JS_ERROR_KEY to "Error: x"), engine.execute("boom()"))
            assertEquals(2.0, engine.execute("1+1"))
            assertEquals<Any?>(3.0, engine.execute("Promise.resolve(3)"), message = "execute awaits promises")
            assertNull(engine.execute("undefined"))
        } finally {
            closeJsEngine(engine)
        }
    }

    @Test
    fun loadScriptErrorIsReported() = flowTest {
        state.loadScript("var ok = 1; var = ;")
        assertEquals(1, handler.jsErrors.size, "syntax error reported: ${handler.jsErrors}")
        assertTrue(handler.jsErrors.single().contains("SyntaxError"), handler.jsErrors.single())
        state.loadScript("throw new TypeError('late')")
        assertEquals("TypeError: late", handler.jsErrors.last())
        assertEquals(2.0, machine.getValue("1+1"))
    }

    @Test
    fun ecmaBuiltinsSurvive() = flowTest {
        assertEquals(
            true,
            machine.getValue("typeof JSON === 'object' && typeof Promise === 'function' && typeof Math.max === 'function'"),
        )
    }

    @Test
    fun asyncContinuationRpcIsDelivered() = flowTest {
        state.loadScript(
            """
            new Promise(function (resolve, reject) { _pendingPromises['p1'] = { resolve: resolve, reject: reject }; })
                .then(function () { SDK.closeAll(); });
            """.trimIndent()
        )
        machine.executeScript("_resolveJsPromise('p1', 1, null)")
        eventually { "closeAll" in handler.calls }
    }

    @Test
    fun hostPromiseReplyRejectsPendingPromise() = flowTest {
        // Unknown RPC method with a promiseId: the bridge answers through _resolveJsPromise with an error
        state.loadScript(
            """
            new Promise(function (resolve, reject) { _pendingPromises['p2'] = { resolve: resolve, reject: reject }; })
                .catch(function (e) { SDK.userCustomAction({ userCustomId: e.message }); });
            postToHost({ method: 'SDK.noSuchMethod', params: {}, promiseId: 'p2' });
            """.trimIndent()
        )
        eventually { handler.calls.any { it.startsWith("userCustomAction:") } }
        assertEquals(listOf("userCustomAction:method 'SDK.noSuchMethod' not found"), handler.calls)
    }

    @Test
    fun sdkEnvPlatformIsAndroid() = flowTest {
        val env = SDKGlobals.buildPreviewSDKEnvJson(previewDependencies(PlatformContextHandle()).metaInfo, localeForTag("en-US"))
        state.injectSDKGlobals(env, "{}")
        assertEquals("android", machine.getValue("SDKEnv.platform"))
        assertTrue("SDKEnv" !in snapshot().keys)
    }

    @Test
    fun resetGivesFreshRealmWithBridge() = flowTest {
        state.loadScript("var leftover = 1;")
        val framesBefore = adaptyFrameCount()
        state.reset()
        assertEquals("undefined", machine.getValue("typeof leftover"))
        assertEquals("function", machine.getValue("typeof SDK.openScreen"))
        assertEquals(framesBefore, adaptyFrameCount(), "reset replaces the iframe instead of adding one")
        state.loadScript("var name = 'y';")
        assertEquals(mapOf<String, Any?>("name" to "y"), snapshot())
    }

    @Test
    fun parseJsonShapes() {
        assertEquals(
            mapOf("a" to listOf(1.0, "s", true, null), "b" to mapOf("c" to 1.5)),
            PlatformJson.parse("""{"a":[1,"s",true,null],"b":{"c":1.5}}"""),
        )
    }
}

private val ECMA_GLOBALS = setOf(
    "globalThis", "Infinity", "NaN", "undefined", "eval", "isFinite", "isNaN", "parseFloat", "parseInt",
    "decodeURI", "decodeURIComponent", "encodeURI", "encodeURIComponent", "escape", "unescape",
    "AggregateError", "Array", "ArrayBuffer", "BigInt", "BigInt64Array", "BigUint64Array", "Boolean",
    "DataView", "Date", "Error", "EvalError", "FinalizationRegistry", "Float16Array", "Float32Array",
    "Float64Array", "Function", "Int8Array", "Int16Array", "Int32Array", "Iterator", "Map", "Number",
    "Object", "Promise", "Proxy", "RangeError", "ReferenceError", "RegExp", "Set", "SharedArrayBuffer",
    "String", "Symbol", "SyntaxError", "TypeError", "Uint8Array", "Uint8ClampedArray", "Uint16Array",
    "Uint32Array", "URIError", "WeakMap", "WeakRef", "WeakSet",
    "Atomics", "JSON", "Math", "Reflect", "Intl", "WebAssembly", "console",
)

private suspend fun eventually(timeoutMs: Long = 3000, condition: () -> Boolean) {
    // Real time: RPCs are dispatched on Dispatchers.Main, outside runTest's virtual clock
    withContext(Dispatchers.Default) {
        withTimeout(timeoutMs) { while (!condition()) delay(5) }
    }
}

private suspend fun fetchFixture(name: String): String {
    val base = fixturesBaseUrl()
    if (base.isEmpty()) fail("fixture base URL is not configured: karma.config.d/fixtures.js or the generated paths.js was not loaded")
    val url = "$base/$name"
    val response = fetchText(url).await<JsAny>()
    val status = statusOf(response)
    if (status != 200) fail("fixture fetch returned HTTP $status for $url")
    return bodyOf(response)
}

@JsFun("() => document.querySelectorAll('iframe[data-adapty-js-realm]').length")
private external fun adaptyFrameCount(): Int

@JsFun("() => (window.__karma__ && window.__karma__.config && window.__karma__.config.adaptyUiFixturesUrl) || ''")
private external fun fixturesBaseUrl(): String

@JsFun("(url) => fetch(url).then(r => r.text().then(t => ({ status: r.status, body: t })))")
private external fun fetchText(url: String): Promise<JsAny>

@JsFun("(r) => r.status")
private external fun statusOf(response: JsAny): Int

@JsFun("(r) => r.body")
private external fun bodyOf(response: JsAny): String
