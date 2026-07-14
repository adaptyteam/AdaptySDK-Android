package com.adapty.ui.internal.script

internal const val JS_ERROR_KEY = "__adapty_js_error__"

internal interface JSEngine {

    suspend fun initialize()

    suspend fun execute(jsExpr: String): Any?
    
    suspend fun loadScript(script: String)

    suspend fun reset()

    suspend fun put(name: String, value: Any)

    suspend fun has(name: String): Boolean

    suspend fun get(name: String): Any?
}
