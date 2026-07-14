@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.Adapty
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

internal class AnalyticsEffectHandler(
    private val flowKey: String,
) : EffectHandler {
    override fun handle(effect: Effect, dispatch: (Message) -> Unit) {
        when (effect) {
            is Effect.LogShowFlow -> {
                log(VERBOSE) { "$LOG_PREFIX $flowKey logShowFlow begin" }
                Adapty.logShowFlow(effect.flow) { error ->
                    if (error != null) {
                        log(ERROR) { "$LOG_PREFIX_ERROR $flowKey logShowFlow error: ${error.message}" }
                    } else {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey logShowFlow success" }
                    }
                }
            }
            is Effect.LogFlowEvent -> {
                log(VERBOSE) { "$LOG_PREFIX $flowKey logFlowEvent (${effect.name}) begin" }
                Adapty.logFlowEvent(effect.flow, effect.viewConfigurationId, effect.toEventProperties()) { error ->
                    if (error != null) {
                        log(ERROR) { "$LOG_PREFIX_ERROR $flowKey logFlowEvent (${effect.name}) error: ${error.message}" }
                    } else {
                        log(VERBOSE) { "$LOG_PREFIX $flowKey logFlowEvent (${effect.name}) success" }
                    }
                }
            }
            else -> return
        }
    }

    private companion object {
        val jsInternalKeys = setOf("name", "instanceId", "isBackendEvent", "isCustomerEvent")
    }

    private fun Effect.LogFlowEvent.toEventProperties(): Map<String, Any> {
        val properties = mutableMapOf<String, Any>()
        params.forEach { (key, value) ->
            if (key !in jsInternalKeys && value != null) properties[key] = normalizeJsonValue(value)
        }
        properties["event_type"] = name
        (params["instanceId"] as? String)?.let { properties["screen_id"] = it }
        return properties
    }

    private fun normalizeJsonValue(value: Any): Any = when (value) {
        is Double -> value.toLong().takeIf { it.toDouble() == value } ?: value
        is Map<*, *> -> value.mapValues { (_, v) -> v?.let(::normalizeJsonValue) }
        is List<*> -> value.map { v -> v?.let(::normalizeJsonValue) }
        else -> value
    }
}
