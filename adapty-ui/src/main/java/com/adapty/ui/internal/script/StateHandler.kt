@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import androidx.compose.runtime.Composable
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.Binding
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR

internal interface StateHandler {

    @Composable
    fun observeState(): StateAccessor

    suspend fun reset()

    suspend fun loadScript(script: String)

    var stateOwner: Any?

    suspend fun collectStateSnapshot(): Map<String, Any?>?

    suspend fun applyStateSnapshot(snapshot: Map<String, Any?>)

    suspend fun setActionsSuppressed(suppressed: Boolean)

    var onStateRefreshed: (() -> Unit)?

    suspend fun refreshState()

    suspend fun setValue(binding: TwoWayBinding, value: Any?, screen: NavigationEntry)

    suspend fun executeAction(func: String, params: Map<String, Any?>, scope: Scope, screen: NavigationEntry)

    suspend fun invokeTimerCallback(timerId: String)

    suspend fun sendSDKEvent(eventJson: String)

    suspend fun invokePurchaseCallback(callbackId: String, response: Map<String, Any?>)

    suspend fun invokeRestoreCallback(callbackId: String, response: Map<String, Any?>)

    suspend fun invokeAlertCallback(callbackId: String, response: Map<String, Any?>)

    suspend fun invokePermissionCallback(callbackId: String, response: Map<String, Any?>)

    suspend fun injectSDKGlobals(sdkEnvJson: String, sdkProductsJson: String)

    suspend fun updateSDKProducts(sdkProductsJson: String)

    fun setActionHandler(handler: ActionHandler?)
}

@InternalAdaptyApi
public interface StateAccessor {

    public operator fun get(key: String): Any?
}

@Composable
internal operator fun StateAccessor.get(binding: Binding) : Any? {
    val key = when (binding.scope) {
        Scope.Global -> binding.variable
        Scope.Screen -> {
            val cp = LocalScreenInstance.current.contextPath
            if (cp.isNullOrEmpty()) binding.variable
            else "$cp.${binding.variable}"
        }
    }
    val rawValue = get(key)
    val converter = binding.converter
    if (converter != null) {
        return applyConverter(rawValue, converter, binding.converterParams)
    }
    return rawValue
}

internal fun Any?.toJsInt(): Int? = when (this) {
    is Number -> toInt()
    is Boolean -> if (this) 1 else 0
    else -> null
}

internal fun Any?.toJsFloat(): Float? = when (this) {
    is Number -> toFloat()
    is Boolean -> if (this) 1f else 0f
    else -> null
}

internal fun Any?.toJsLong(): Long? = when (this) {
    is Number -> toLong()
    is Boolean -> if (this) 1L else 0L
    is String, is List<*>, is Map<*, *> -> jsToNumber(this).takeIf { it.isFinite() }?.toLong()
    else -> null
}

private fun jsToNumber(value: Any?): Double = when (value) {
    is String -> jsStringToNumber(value)
    is List<*> -> when (value.size) {
        0 -> 0.0
        1 -> when (val item = value[0]) {
            null -> 0.0
            is Number -> item.toDouble()
            else -> jsToNumber(item)
        }
        else -> Double.NaN
    }
    else -> Double.NaN
}

private val JS_DECIMAL_LITERAL = Regex("[+-]?(?:Infinity|(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)")
private val JS_NON_DECIMAL_LITERAL = Regex("0([xX][0-9a-fA-F]+|[oO][0-7]+|[bB][01]+)")

internal fun jsStringToNumber(str: String): Double {
    val s = str.trim { it.isWhitespace() || it == '\uFEFF' }
    if (s.isEmpty()) return 0.0
    JS_NON_DECIMAL_LITERAL.matchEntire(s)?.let { m ->
        val body = m.groupValues[1]
        val radix = when (body[0].lowercaseChar()) { 'x' -> 16; 'o' -> 8; else -> 2 }
        return body.substring(1).toBigInteger(radix).toDouble()
    }
    if (!JS_DECIMAL_LITERAL.matches(s)) return Double.NaN
    return when {
        s.endsWith("Infinity") -> if (s.startsWith('-')) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        else -> s.toDouble()
    }
}

private fun applyConverter(value: Any?, converter: String, params: Any?): Any? {
    return when (converter) {
        "is_equal" -> {
            val target = if (params is Map<*, *>) params["value"] else params
            if (value is Number && target is Number) {
                value.toDouble() == target.toDouble()
            } else {
                value == target
            }
        }
        "map" -> {
            val list = params as? List<*> ?: return null
            val index = value.toJsInt32() ?: return null
            if (index !in list.indices) {
                log(ERROR) { "$LOG_PREFIX_ERROR map converter: index $index out of range (size ${list.size})" }
                return null
            }
            list[index]
        }
        else -> value
    }
}

private fun Any?.toJsInt32(): Int? = when (this) {
    is Number -> toDouble().jsToInt32()
    is Boolean -> if (this) 1 else 0
    is String -> jsToNumber().jsToInt32()
    else -> null
}

private fun String.jsToNumber(): Double = trim().let { s ->
    if (s.isEmpty()) 0.0 else s.toDoubleOrNull() ?: Double.NaN
}

private fun Double.jsToInt32(): Int =
    if (isNaN() || isInfinite()) 0 else toLong().toInt()
