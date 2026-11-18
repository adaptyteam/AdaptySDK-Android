@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI
import com.adapty.ui.internal.script.FlowStateHandlers
import com.adapty.ui.internal.script.StateHandler
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicReference

internal class FlowRuntimeState {

    @Volatile
    var isolateStateHandler: StateHandler? = null

    val initialization = AtomicReference<Job?>(null)

    val presentationRequest = AtomicReference<CompletableJob?>(null)

    @Volatile
    var stateHandlers: FlowStateHandlers? = null

    val appMessageOrder = Mutex()

    @Volatile
    var jsSnapshot: Map<String, Any?>? = null

    @Volatile
    var loadedAssets: Map<String, AdaptyUI.FlowConfiguration.Asset> = emptyMap()

    @Volatile
    var navigation: NavigationState? = null

    @Volatile
    var timerCommands: Map<String, TimerSetCommand> = emptyMap()

    val restorableNavigation: NavigationState?
        get() = navigation?.takeIf { it.entries.isNotEmpty() }

    fun adoptFrom(other: FlowRuntimeState) {
        jsSnapshot = other.jsSnapshot
        navigation = other.navigation
        timerCommands = other.timerCommands
        loadedAssets = other.loadedAssets
    }

    fun clear() {
        jsSnapshot = null
        navigation = null
        timerCommands = emptyMap()
        loadedAssets = emptyMap()
    }
}
