@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.ui.NavigationEntry
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.Scope
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import java.util.concurrent.ConcurrentHashMap

internal class JSStateHandler(
    private val stateMachine: JSStateMachine,
    private val gson: Gson,
) : StateHandler {

    private val stateAccessor = JSStateAccessor()

    override var onStateRefreshed: (() -> Unit)? = null

    private val stateCells = ConcurrentHashMap<String, MutableState<Any?>>()

    private val staticKeys = ConcurrentHashMap.newKeySet<String>()

    private val setterExistsCache = ConcurrentHashMap<String, Boolean>()

    private fun cell(key: String): MutableState<Any?> =
        stateCells.getOrPut(key) { mutableStateOf(null) }

    @Composable
    override fun observeState(): StateAccessor {
        return stateAccessor
    }

    override suspend fun reset() {
        stateCells.values.forEach { it.value = null }
        staticKeys.clear()
        setterExistsCache.clear()
        stateMachine.reset()
    }
    
    override suspend fun loadScript(script: String) {
        stateMachine.loadScript(script)
        refreshStateCache()
    }

    override suspend fun refreshState() = refreshStateCache()

    override suspend fun executeAction(func: String, params: Map<String, Any?>, scope: Scope, screen: NavigationEntry) {
        val screenParams = mapOf(
            "instanceId" to screen.screenInstanceId,
            "type" to screen.screenType,
            "contextPath" to screen.contextPath,
            "navigatorId" to screen.navigatorId,
        )
        val paramsWithScreen = params + ("_screen" to screenParams)
        val basePath: String? = when (scope) {
            Scope.Global -> null
            Scope.Screen -> screen.contextPath?.takeIf { it.isNotEmpty() }
        }
        val funcPath = JSPathUtils.buildPath(basePath, func)
        stateMachine.executeAction(funcPath, paramsWithScreen)
        refreshStateCache()
    }
    
    override suspend fun setValue(binding: TwoWayBinding, value: Any?, screen: NavigationEntry) {
        val resolvedValue = if (binding.converter != null) {
            applyReverseConverter(value, binding.converter, binding.converterParams)
        } else {
            value
        }

        val basePath: String? = when (binding.scope) {
            Scope.Global -> null
            Scope.Screen -> screen.contextPath?.takeIf { it.isNotEmpty() }
        }

        val screenParams = mapOf(
            "instanceId" to screen.screenInstanceId,
            "type" to screen.screenType,
            "contextPath" to screen.contextPath,
            "navigatorId" to screen.navigatorId,
        )

        val setterParams = mapOf(
            "name" to binding.variable,
            "value" to resolvedValue,
            "_screen" to screenParams,
        )

        val explicitSetter = binding.setter
        if (explicitSetter != null) {
            val fullSetterPath = JSPathUtils.buildPath(basePath, explicitSetter)
            val exists = checkSetterExists(fullSetterPath)
            if (exists) {
                callSetter(fullSetterPath, setterParams)
                return
            } else {
                log(WARN) { "$LOG_PREFIX Explicit setter '${fullSetterPath.joinToString(".") { it.value }}' not found" }
                return
            }
        }

        val derivedSetterName = "set${binding.variable.replaceFirstChar { it.uppercase() }}"
        val fullDerivedSetterPath = JSPathUtils.buildPath(basePath, derivedSetterName)
        val derivedExists = checkSetterExists(fullDerivedSetterPath)
        if (derivedExists) {
            callSetter(fullDerivedSetterPath, setterParams)
            return
        }

        val fullVariablePath = JSPathUtils.buildPath(basePath, binding.variable)
        setValueDirectly(fullVariablePath, resolvedValue)
    }

    private suspend fun checkSetterExists(setterPath: List<PathComponent>): Boolean {
        val cacheKey = setterPath.joinToString(".") { it.value }
        setterExistsCache[cacheKey]?.let { return it }

        val exists = stateMachine.hasFunction(setterPath)
        setterExistsCache[cacheKey] = exists
        return exists
    }

    private suspend fun callSetter(setterPath: List<PathComponent>, params: Map<String, Any?>) {
        val paramsJson = gson.toJson(params)
        val script = JSPathUtils.generateCallScript(setterPath, paramsJson)
        stateMachine.executeScript(script)
        refreshStateCache()
    }

    private suspend fun setValueDirectly(fullPath: List<PathComponent>, value: Any?) {
        val valueJson = gson.toJson(value)
        val script = JSPathUtils.generateSetValueScript(fullPath, valueJson)
        stateMachine.executeScript(script)
        refreshStateCache()
    }

    private fun applyReverseConverter(value: Any?, converter: String, params: Any?): Any? {
        return when (converter) {
            "is_equal" -> {
                if (params is Map<*, *>) {
                    if (value == true) params["value"] else params["false_value"]
                } else {
                    if (value == true) params else null
                }
            }
            else -> value
        }
    }

    override suspend fun invokeTimerCallback(timerId: String) {
        stateMachine.invokeTimerCallback(timerId)
        refreshStateCache()
    }

    override suspend fun sendSDKEvent(eventJson: String) {
        stateMachine.sendSDKEvent(eventJson)
        refreshStateCache()
    }

    override suspend fun invokePurchaseCallback(callbackId: String, response: Map<String, Any?>) {
        stateMachine.invokePurchaseCallback(callbackId, response)
        refreshStateCache()
    }

    override suspend fun invokeRestoreCallback(callbackId: String, response: Map<String, Any?>) {
        stateMachine.invokeRestoreCallback(callbackId, response)
        refreshStateCache()
    }

    override suspend fun invokeAlertCallback(callbackId: String, response: Map<String, Any?>) {
        stateMachine.invokeAlertCallback(callbackId, response)
        refreshStateCache()
    }

    override suspend fun invokePermissionCallback(callbackId: String, response: Map<String, Any?>) {
        stateMachine.invokePermissionCallback(callbackId, response)
        refreshStateCache()
    }

    override suspend fun injectSDKGlobals(sdkEnvJson: String, sdkProductsJson: String) {
        stateMachine.injectSDKGlobals(sdkEnvJson, sdkProductsJson)
        setStaticValue("SDKEnv", gson.fromJson(sdkEnvJson, Map::class.java))
        setStaticValue("SDKProducts", gson.fromJson(sdkProductsJson, Map::class.java))
    }

    override suspend fun updateSDKProducts(sdkProductsJson: String) {
        stateMachine.updateSDKProducts(sdkProductsJson)
        setStaticValue("SDKProducts", gson.fromJson(sdkProductsJson, Map::class.java))
    }

    private fun setStaticValue(key: String, value: Any?) {
        staticKeys.add(key)
        cell(key).value = value
    }

    override fun setActionHandler(handler: ActionHandler?) {
        stateMachine.setActionHandler(handler)
    }
    
    private suspend fun refreshStateCache() {
        try {
            val stateScript = """
                (function() {
                    var result = {};
                    var g = (typeof globalThis !== 'undefined') ? globalThis : this;
                    var skip = (typeof __adapty_baseline_keys__ !== 'undefined')
                        ? __adapty_baseline_keys__
                        : {'SDK':1,'console':1,'undefined':1,'NaN':1,'Infinity':1,
                           'window':1,'self':1,'globalThis':1,'document':1,
                           'navigator':1,'location':1,'history':1,'screen':1,
                           'postToHost':1,'log':1};
                    skip['SDKEnv'] = 1;
                    skip['SDKProducts'] = 1;
                    function collect(obj, name) {
                        result[name] = {};
                        var keys = Object.keys(obj);
                        for (var j = 0; j < keys.length; j++) {
                            try {
                                var v = obj[keys[j]];
                                if (typeof v !== 'function' && v !== g) {
                                    result[name][keys[j]] = v;
                                }
                            } catch(e) {}
                        }
                    }
                    var names = Object.getOwnPropertyNames(g);
                    for (var i = 0; i < names.length; i++) {
                        var key = names[i];
                        if (skip[key]) continue;
                        try {
                            var val_ = g[key];
                            if (val_ === null || val_ === undefined || val_ === g) continue;
                            if (typeof val_ === 'function') continue;
                            if (typeof val_ === 'object' && !Array.isArray(val_)) {
                                collect(val_, key);
                            } else {
                                result[key] = val_;
                            }
                        } catch(e) {}
                    }
                    try { if (typeof Legacy !== 'undefined' && !result['Legacy']) collect(Legacy, 'Legacy'); } catch(e) {}
                    return result;
                })()
            """.trimIndent()

            val result = stateMachine.getValue(stateScript) as? Map<*, *>

            val newKeys = HashSet<String>(result?.size ?: 0)
            result?.forEach { (key, value) ->
                val k = key.toString()
                newKeys.add(k)
                cell(k).value = value
            }
            stateCells.forEach { (key, cell) ->
                if (key !in newKeys && key !in staticKeys) cell.value = null
            }

            onStateRefreshed?.invoke()
        } catch (e: Exception) {
            log(ERROR) { "$LOG_PREFIX_ERROR Error refreshing state cache: ${e.localizedMessage}" }
        }
    }
    
    private inner class JSStateAccessor : StateAccessor {

        override operator fun get(key: String): Any? {
            val parts = JSPathUtils.parsePath(key)
            val root = parts.firstOrNull()?.value ?: return null
            var current: Any? = cell(root).value
            for (part in parts.drop(1)) {
                current = (current as? Map<*, *>)?.get(part.value)
                if (current == null) {
                    break
                }
            }
            return current
        }
    }
}
