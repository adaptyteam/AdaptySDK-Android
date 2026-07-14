@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.event

import androidx.compose.runtime.compositionLocalOf
import com.adapty.internal.utils.InternalAdaptyApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal enum class LifecyclePhase(val key: String) {
    WILL_APPEAR("on_will_appear"),
    DID_APPEAR("on_did_appear"),
    WILL_DISAPPEAR("on_will_disappear"),
    DID_DISAPPEAR("on_did_disappear"),
}

internal sealed interface DispatchedEvent {
    val screenInstanceId: String?
    val transitionId: String?
    val sequence: Long
    val eventKey: String

    data class Lifecycle(
        val phase: LifecyclePhase,
        override val screenInstanceId: String,
        override val transitionId: String?,
        override val sequence: Long,
        val epoch: Long,
    ) : DispatchedEvent {
        override val eventKey: String get() = phase.key
    }

    data class Custom(
        val customId: String,
        override val screenInstanceId: String?,
        override val sequence: Long,
        val epoch: Long?,
    ) : DispatchedEvent {
        override val transitionId: String? get() = null
        override val eventKey: String get() = customId
    }
}

internal class EventDispatcher {

    private val _events = MutableSharedFlow<DispatchedEvent>(
        replay = REPLAY_SIZE,
        extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
    )
    val events: SharedFlow<DispatchedEvent> = _events.asSharedFlow()

    private val fireCounts = mutableMapOf<Pair<String, String>, Int>()

    private val lifecycleHistoryByScope = mutableMapOf<Pair<String, Long>, MutableList<DispatchedEvent.Lifecycle>>()

    @Volatile
    private var sequenceCounter: Long = 0L

    @Synchronized
    fun publishLifecycle(phase: LifecyclePhase, screenInstanceId: String, transitionId: String?, epoch: Long) {
        incrementFireCount(screenInstanceId, phase.key)
        val seq = sequenceCounter++
        val event = DispatchedEvent.Lifecycle(phase, screenInstanceId, transitionId, seq, epoch)
        _events.tryEmit(event)
        val scopeKey = screenInstanceId to epoch
        if (phase == LifecyclePhase.DID_DISAPPEAR) {
            lifecycleHistoryByScope.remove(scopeKey)
        } else {
            lifecycleHistoryByScope.getOrPut(scopeKey) { mutableListOf() }.add(event)
        }
    }

    @Synchronized
    fun lifecycleHistoryFor(screenInstanceId: String, epoch: Long): List<DispatchedEvent.Lifecycle> =
        lifecycleHistoryByScope[screenInstanceId to epoch]?.toList().orEmpty()

    @Synchronized
    fun publishCustom(customId: String, screenInstanceId: String?, epoch: Long?) {
        incrementFireCount(screenInstanceId, customId)
        _events.tryEmit(
            DispatchedEvent.Custom(customId, screenInstanceId, sequenceCounter++, epoch)
        )
    }

    @Synchronized
    fun fireCount(screenInstanceId: String?, eventId: String): Int =
        fireCounts[(screenInstanceId ?: "") to eventId] ?: 0

    private fun incrementFireCount(screenInstanceId: String?, eventId: String) {
        val key = (screenInstanceId ?: "") to eventId
        fireCounts[key] = (fireCounts[key] ?: 0) + 1
    }

    private companion object {
        const val REPLAY_SIZE = 16
        const val EXTRA_BUFFER_CAPACITY = 16
    }
}

internal val LocalEventDispatcher = compositionLocalOf<EventDispatcher> {
    error("LocalEventDispatcher not provided")
}
