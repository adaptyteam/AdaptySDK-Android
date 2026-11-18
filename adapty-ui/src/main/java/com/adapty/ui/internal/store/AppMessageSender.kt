@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.script.FlowStateHandlers
import com.adapty.ui.internal.script.JsLiteralWriter
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.util.UUID

private val appMessageScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal fun sendApplicationMessage(
    config: AdaptyUI.FlowConfiguration,
    stateHandlers: FlowStateHandlers?,
    payload: Map<String, Any?>,
    id: String = UUID.randomUUID().toString(),
    scope: CoroutineScope = appMessageScope,
) {
    val name = "applicationMsg"
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
        config.runtimeState.appMessageOrder.withLock {
            try {
                yield()
                val stateHandler = stateHandlers?.awaitRunningStateOf(config)
                if (stateHandler == null) {
                    log(WARN) { "$LOG_PREFIX $name $id dropped: the flow has no running JS state (not presented yet, or released)" }
                    return@withLock
                }
                val event = Effect.SendSDKEvent.AppMessage(id, payload, Effect.SendSDKEvent.AppMessage.Source.Application)
                stateHandler.sendSDKEvent(JsLiteralWriter.writeEvent(event.jsEventFields()))
                log(VERBOSE) { "$LOG_PREFIX $name $id sent" }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                log(ERROR) { "$LOG_PREFIX_ERROR $name $id failed: ${e.localizedMessage}" }
            }
        }
    }
}
