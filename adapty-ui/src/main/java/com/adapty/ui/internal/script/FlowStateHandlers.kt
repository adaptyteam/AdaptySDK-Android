@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.script

import android.content.Context
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.adoptFlowStateForConfigChange
import com.adapty.ui.internal.utils.initializeFlowState
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.sendProductsLoaded
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import com.adapty.utils.AdaptyLogLevel.Companion.WARN
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal sealed class FlowStateHandlers {

    abstract fun handlerFor(config: AdaptyUI.FlowConfiguration): StateHandler

    abstract suspend fun initialize(
        config: AdaptyUI.FlowConfiguration,
        sdkEnvJson: String,
        sdkProductsJson: String,
    )

    abstract fun runningStateOf(config: AdaptyUI.FlowConfiguration): StateHandler?

    suspend fun awaitRunningStateOf(config: AdaptyUI.FlowConfiguration): StateHandler? {
        config.runtimeState.presentationRequest.get()?.join()
        config.runtimeState.initialization.get()?.join()
        return runningStateOf(config)
    }

    abstract fun release(config: AdaptyUI.FlowConfiguration)

    abstract fun adopt(previous: AdaptyUI.FlowConfiguration, next: AdaptyUI.FlowConfiguration)

    class Shared(private val handler: StateHandler) : FlowStateHandlers() {

        override fun handlerFor(config: AdaptyUI.FlowConfiguration) = handler

        override suspend fun initialize(
            config: AdaptyUI.FlowConfiguration,
            sdkEnvJson: String,
            sdkProductsJson: String,
        ) = handler.initializeFlowState(config, sdkEnvJson, sdkProductsJson)

        override fun runningStateOf(config: AdaptyUI.FlowConfiguration): StateHandler? =
            handler.takeIf { it.stateOwner === config }

        override fun release(config: AdaptyUI.FlowConfiguration) {
            if (handler.stateOwner === config) handler.stateOwner = null
        }

        override fun adopt(previous: AdaptyUI.FlowConfiguration, next: AdaptyUI.FlowConfiguration) {
            handler.adoptFlowStateForConfigChange(previous, next)
        }
    }

    class Isolated internal constructor(
        newHandler: () -> StateHandler,
        warmUp: (onUnavailable: (Throwable) -> Unit) -> Unit,
        maxLive: Int = MAX_LIVE_ISOLATES,
    ) : FlowStateHandlers() {

        constructor(appContext: Context, gson: Gson) : this(
            newHandler = { JSStateHandler(JSStateMachine(appContext, JSActionBridge(gson), gson), gson) },
            warmUp = { onUnavailable ->
                CoroutineScope(Dispatchers.Default).launch {
                    runCatching { JSEngineAndroidx.acquireSandbox(appContext) }
                        .onFailure(onUnavailable)
                }
            },
        )

        companion object {
            const val MAX_LIVE_ISOLATES = 8
        }

        private val registry = IsolateRegistry(
            maxLive = maxLive,
            slots = object : IsolateRegistry.Slots<AdaptyUI.FlowConfiguration, StateHandler> {
                override fun get(owner: AdaptyUI.FlowConfiguration) =
                    owner.runtimeState.isolateStateHandler

                override fun set(owner: AdaptyUI.FlowConfiguration, handler: StateHandler?) {
                    owner.runtimeState.isolateStateHandler = handler
                }

                override fun onEvicted(owner: AdaptyUI.FlowConfiguration) {
                    owner.runtimeState.clear()
                    log(VERBOSE) { "$LOG_PREFIX FlowStateHandlers: evicted the isolate of ${owner.id} (cap $maxLive)" }
                }
            },
            create = newHandler,
            close = StateHandler::close,
        )

        @Volatile
        private var sandboxUnavailable = false

        private val fallback = lazy { Shared(newHandler()) }

        init {
            warmUp { e ->
                log(WARN) { "$LOG_PREFIX FlowStateHandlers: JavaScriptSandbox unavailable, new flows share one VM: ${e.localizedMessage}" }
                sandboxUnavailable = true
            }
        }

        private fun isolateOf(config: AdaptyUI.FlowConfiguration): StateHandler? =
            registry.existing(config)

        override fun handlerFor(config: AdaptyUI.FlowConfiguration): StateHandler =
            isolateOf(config)
                ?: if (sandboxUnavailable) fallback.value.handlerFor(config)
                else registry.getOrCreate(config).also {
                    log(VERBOSE) { "$LOG_PREFIX FlowStateHandlers: isolate for ${config.id} (live: ${registry.liveCount})" }
                }

        override suspend fun initialize(
            config: AdaptyUI.FlowConfiguration,
            sdkEnvJson: String,
            sdkProductsJson: String,
        ) {
            val handler = handlerFor(config)
            if (handler !== isolateOf(config)) {
                fallback.value.initialize(config, sdkEnvJson, sdkProductsJson)
                return
            }
            if (handler.stateOwner === config) {
                log(VERBOSE) { "$LOG_PREFIX initializeFlowState: the flow's isolate is running, keeping it" }
                return
            }
            config.runtimeState.navigation = null
            config.runtimeState.timerCommands = emptyMap()
            handler.setActionHandler(null)
            handler.reset()
            handler.injectSDKGlobals(sdkEnvJson, sdkProductsJson)
            handler.loadScript(config.initialScript)
            if (sdkProductsJson != "{}") handler.sendProductsLoaded()
            handler.stateOwner = config
        }

        override fun runningStateOf(config: AdaptyUI.FlowConfiguration): StateHandler? =
            isolateOf(config)?.takeIf { it.stateOwner === config }
                ?: fallback.initializedOrNull()?.runningStateOf(config)

        override fun release(config: AdaptyUI.FlowConfiguration) {
            if (config.runtimeState.isolateStateHandler != null) registry.release(config)
            else fallback.initializedOrNull()?.release(config)
        }

        override fun adopt(previous: AdaptyUI.FlowConfiguration, next: AdaptyUI.FlowConfiguration) {
            if (isolateOf(previous) == null) {
                fallback.initializedOrNull()?.adopt(previous, next)
                    ?: next.runtimeState.adoptFrom(previous.runtimeState)
                return
            }
            next.runtimeState.adoptFrom(previous.runtimeState)
            registry.move(previous, next)
            next.runtimeState.isolateStateHandler?.let { handler ->
                if (handler.stateOwner === previous) handler.stateOwner = next
            }
        }

        private fun Lazy<Shared>.initializedOrNull(): Shared? = if (isInitialized()) value else null
    }
}
