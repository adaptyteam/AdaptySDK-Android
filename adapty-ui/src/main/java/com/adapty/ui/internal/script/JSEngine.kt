package com.adapty.ui.internal.script

internal const val JS_ERROR_KEY = "__adapty_js_error__"

/**
 * JS expression for the global object; Android System WebView has `globalThis` only from 71.
 * The `this` fallback is the global object only where `this` is bound to it: the top level of a
 * classic script ([JSEngine.loadScript]), indirect eval ([JSEngine.execute] runs `(0, eval)`),
 * or a sloppy-mode function called without a receiver (the `var g = ...` IIFEs). It is not valid
 * inside a strict-mode function, where that `this` is `undefined`.
 */
internal const val JS_GLOBAL_OBJECT = "(typeof globalThis !== 'undefined' ? globalThis : this)"

internal interface JSEngine {

    suspend fun initialize()

    suspend fun execute(jsExpr: String): Any?
    
    suspend fun loadScript(script: String)

    suspend fun reset()

    suspend fun put(name: String, value: Any)

    suspend fun has(name: String): Boolean

    suspend fun get(name: String): Any?
}
