@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.store

import com.adapty.internal.utils.InternalAdaptyApi

internal class FlowRuntimeState {

    @Volatile
    var jsSnapshot: Map<String, Any?>? = null

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
    }

    fun clear() {
        jsSnapshot = null
        navigation = null
        timerCommands = emptyMap()
    }
}
