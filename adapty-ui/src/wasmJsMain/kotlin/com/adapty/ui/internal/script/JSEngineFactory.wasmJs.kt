package com.adapty.ui.internal.script

import com.adapty.ui.internal.platform.PlatformContextHandle
import com.adapty.ui.internal.platform.webStub

/** Inert engine until the browser engine lands. */
private class StubJSEngine : JSEngine {
    override suspend fun initialize() {}
    override suspend fun execute(jsExpr: String): Any? = null
    override suspend fun loadScript(script: String) {}
    override suspend fun reset() {}
    override suspend fun put(name: String, value: Any) {}
    override suspend fun has(name: String): Boolean = false
    override suspend fun get(name: String): Any? = null
}

internal actual suspend fun createJsEngine(
    ctx: PlatformContextHandle,
    bridge: JSActionBridge,
    onEngineDied: suspend () -> Unit,
): JSEngine {
    webStub("createJsEngine")
    return StubJSEngine()
}

internal actual fun isJsEngineDead(e: Throwable): Boolean = false

internal actual fun closeJsEngine(engine: JSEngine?) {}
